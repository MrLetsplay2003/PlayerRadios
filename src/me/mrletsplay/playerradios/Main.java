package me.mrletsplay.playerradios;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import me.mrletsplay.mrcore.bukkitimpl.versioned.NMSVersion;
import me.mrletsplay.mrcore.misc.MiscUtils;
import me.mrletsplay.playerradios.util.ImportResult;
import me.mrletsplay.playerradios.util.PasteText;
import me.mrletsplay.playerradios.util.PlayerRadiosPlaceholder;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.RadioStations;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.Tools;
import me.mrletsplay.playerradios.util.UpdateChecker;
import me.mrletsplay.playerradios.util.UpdateChecker.Result;
import me.mrletsplay.playerradios.util.song.Song;
import me.mrletsplay.playerradios.util.songloader.SongLoader;
import me.mrletsplay.playerradios.util.songloader.SongLoadingException;

public class Main extends JavaPlugin {
	
	/*TODO
	 * Manage broadcasts
	 * Open room cmd
	 * Volume
	 * 
	 * songinfo
	 * ru file
	 * cn file?
	 */

	public static String PLUGIN_VERSION;
	public static Plugin pl;
	private static Thread t, t2;
	public static boolean enabled;
	public static boolean exportRunning = false;
	public static Thread tempProcessThread;
	public static int tickTimeMs;
	private static int refrT;
	public static Random random = new Random();

	@Override
	public void onEnable() {
		MrCoreBukkitImpl.loadMrCore(this);
		pl = this;
		enabled = false;
		PLUGIN_VERSION = getDescription().getVersion();
		Config.init();
		PlayerManager.init();
		getLogger().info("Detected MC server version: " + NMSVersion.getCurrentServerVersion().getFriendlyName() + " (NMS version: " + NMSVersion.getCurrentServerVersion().getNMSName() + ")");
		Bukkit.getPluginManager().registerEvents(new Events(), this);
		if (Config.enable_update_check) {
			getLogger().info("Checking for update...");
			UpdateChecker.Result res = UpdateChecker.checkForUpdate();
			if(res.updAvailable) {
				for (Player pl : Bukkit.getOnlinePlayers()) {
					if (pl.hasPermission(Config.PERM_NOTIFY_UPDATE)) {
						UpdateChecker.sendUpdateMessage(res, pl);
					}
				}
			}
			getLogger().info("Current version: "+PLUGIN_VERSION+", Newest version: "+res.updVer);
			if(res.updAvailable) {
				getLogger().info("----------------------------------------------");
				getLogger().info("There's an update available: "+res.updVer);
				res.updChlog.stream().forEach(getLogger()::info);
				getLogger().info("----------------------------------------------");
			}else{
				getLogger().info("No update available!");
			}
		}
		if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			PlayerRadiosPlaceholder.hook();
			getLogger().info("PaceholderAPI hook registered!");
		}
		int count = SongManager.countSongs();
		if(Config.thread_on_process && count>=Config.thread_on_process_when) {
			t2 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					getLogger().info("Importing songs using thread as there are "+count+" files to import");
					init();
				}
			}, "PlayerRadios-Import-Thread");
			t2.start();
		}else {
			init();
		}
		if(!Config.use_fixed_playback) {
			tickTimeMs = 50;
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
	
				@Override
				public void run() {
					refrT++;
					if(refrT*tickTimeMs>=Config.gui_refresh_interval_ms) {
						refrT = 0;
						StationManager.updateStationsGUI();
					}
					RadioStations.tick();
				}
			}, 20, 1);
		}else {
			t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					tickTimeMs = 10;
					Main.pl.getLogger().info("Thread started successfully!");
					while(!Thread.interrupted()) {
						refrT++;
						if(refrT*tickTimeMs>=Config.gui_refresh_interval_ms) {
							refrT = 0;
							StationManager.updateStationsGUI();
						}
						RadioStations.tick();
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							break;
						}
					}
					Main.pl.getLogger().info("Thread ended successfully!");
				}
			}, "PlayerRadios-Ticker-Thread");
			t.start();
		}
		if(Config.save_last_listened) {
			for(Player p : Bukkit.getOnlinePlayers()) {
				int rID = PlayerManager.getLastListened(p);
				if(rID>=0) {
					RadioStation r = StationManager.getRadioStation(rID);
					if(r!=null) r.addPlayerListening(p);
				}
			}
		}
		Metrics m = new Metrics(this);
		m.addCustomChart(new Metrics.SimplePie("use_uuids", () -> String.valueOf(Config.use_uuids)));
		m.addCustomChart(new Metrics.SingleLineChart("song_count", SongManager.getSongs()::size));
		getLogger().info("Enabled PlayerRadios v"+PLUGIN_VERSION);
	}
	
	private void init() {
		refrT = 0;
		SongManager.init();
		StationManager.init();
		SongManager.registerNewSongs();
		StationManager.updateStationsGUI();
		if(Config.default_station>=0 && StationManager.getRadioStation(Config.default_station)==null) {
			getLogger().info("Couldn't find default station "+Config.default_station);
		}
		enabled = true;
		t2 = null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onDisable() {
		if(Config.use_fixed_playback) {
			if(t!=null) {
				t.interrupt();
				long timeS = System.currentTimeMillis();
				while(t.isAlive()) {
					try {
						Thread.sleep(10);
						if(System.currentTimeMillis()-timeS>=5000) {
							getLogger().info("Radio station thread didn't stop after 5 seconds. Stopping thread manually...");
							t.stop();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if(exportRunning && tempProcessThread!=null) {
			exportRunning = false;
			tempProcessThread.interrupt();
			long timeS = System.currentTimeMillis();
			while(tempProcessThread.isAlive()) {
				try {
					Thread.sleep(10);
					if(System.currentTimeMillis()-timeS>=5000) {
						getLogger().info("Process thread didn't stop after 5 seconds. Stopping thread manually...");
						tempProcessThread.stop();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(t2!=null) {
			t2.stop();
		}
		
		if(Config.save_last_listened) {
			for(Player p : Bukkit.getOnlinePlayers()) {
				RadioStation r = RadioStations.getRadioStationListening(p);
				if(r!=null) {
					r.removePlayerListening(p);
					PlayerManager.setLastListened(p, r.getID(), false);
				}else {
					PlayerManager.setLastListened(p, -1, false);
				}
			}
			PlayerManager.save();
		}
		StationManager.saveAll();
		RadioStations.stations.clear();
		getLogger().info("Disabled");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("playerradios")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				if(!enabled) {
					p.sendMessage(Config.getMessage("still-loading"));
					return true;
				}
				if(args.length>=1) {
					if (args[0].equalsIgnoreCase("version")) {
						if (p.hasPermission(Config.PERM_NOTIFY_UPDATE)) {
							p.sendMessage("Current PlayerRadios version: §7"+PLUGIN_VERSION);
							if(Config.enable_update_check && Config.update_check_on_command) {
								Result r = UpdateChecker.checkForUpdate();
								if(r.updAvailable) {
									UpdateChecker.sendUpdateMessage(r, p);
								}else {
									p.sendMessage("§aYou are using the newest version of PlayerRadios");
								}
							}
						} else {
							sendCommandHelp(p, null);
						}
						return true;
					}else if(args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
						if(p.hasPermission(Config.PERM_RELOAD)) {
							if(args.length == 1){
								Config.config.reload(false);
								Bukkit.getPluginManager().disablePlugin(this);
								Bukkit.getPluginManager().enablePlugin(this);
								p.sendMessage(Config.getMessage("reload-complete"));
							}else{
								sendCommandHelp(p, null);
							}
						}else {
							sendCommandHelp(p, null);
						}
						return true;
					}else if(args[0].equalsIgnoreCase("bugreport")) {
						if(p.hasPermission(Config.PERM_ALLOW_BUGREPORT)) {
							if(args.length == 1){
								try {
									List<String> config = Files.readAllLines(Config.config.getConfigFile().toPath());
									List<String> stations = Files.readAllLines(StationManager.stationFile.toPath());
									p.sendMessage(Config.getMessage("bugreport.success", "link", PasteText.glotSafe(
											"config.yml",
											config.stream().collect(Collectors.joining("\n")),
											"stations.yml",
											stations.stream().collect(Collectors.joining("\n")))));
								} catch (IOException e) {
									e.printStackTrace();
								}
							}else{
								sendCommandHelp(p, null);
							}
						}else {
							sendCommandHelp(p, null);
						}
						return true;
					}
				}

				if(Config.world_list_blacklist == Config.world_list.contains(p.getWorld().getName())) {
					p.sendMessage(Config.getMessage("world-blacklisted"));
					return true;
				}
				
				if (args.length >= 1) {
					if(args[0].equalsIgnoreCase("help")) {
						if(args.length==1) {
							sendCommandHelp(p, null);
						}else if(args.length==2) {
							sendCommandHelp(p, args[1]);
						}
					}
					
					//Disableable commands (all)
					else if(Config.disable_commands && Config.disable_commands_all) {
						p.sendMessage(Config.getMessage("commands-disabled"));
						return true;
					}else if(args[0].equalsIgnoreCase("submit")) {
						if(args.length==2) {
							if((Config.enable_submit && (Config.submit_needs_perm && sender.hasPermission(Config.PERM_SUBMIT) || !Config.submit_needs_perm)) || sender.hasPermission(Config.PERM_SUBMIT_WHEN_DISABLED)) {
								if(tempProcessThread!=null) {
									p.sendMessage(Config.getMessage("process-already-running"));
									return true;
								}
								tempProcessThread = new Thread(new Runnable() {
									
									@Override
									public void run() {
										try {
											URL url = new URL(args[1]);
											if(Tools.getFileSize(url)/1024<=Config.submit_max_file_size) {
												File dlFolder = new File(pl.getDataFolder(), "/import/download/");
												p.sendMessage(Config.getMessage("submit.downloading"));
												File f = Tools.downloadWithFileName(url, dlFolder);
												p.sendMessage(Config.getMessage("submit.reading"));
												ImportResult ss = SongManager.tryAllImport(f);
												f.delete();
												dlFolder.delete();
												if(ss!=null) {
													for(Song s : ss.songs) {
														s.setID(-1);
														SongManager.getSongs().add(s);
													}
													SongManager.registerNewSongs();
													p.sendMessage(Config.getMessage("submit.success").replace("%count%", ss.songs.size()+"").replace("%format%", ss.format));
												}else {
													p.sendMessage(Config.getMessage("submit.invalid-file"));
												}
											}else {
												p.sendMessage(Config.getMessage("submit.file-too-big"));
											}
										}catch(MalformedURLException e) {
											p.sendMessage(Config.getMessage("submit.invalid-url"));
										} catch (IOException e) {
											sendCommandHelp(p, null);
										}
										tempProcessThread = null;
									}
								},  "PlayerRadios-Process-Thread");
								tempProcessThread.start();
							}else {
								sendCommandHelp(p, null);
							}
						}else {
							sendCommandHelp(p, null);
						}
					}else if(args[0].equalsIgnoreCase("search")) {
						if(Config.allow_create_stations || p.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
							if(args.length>=2) {
								String name = "";
								for(int i = 1; i < args.length; i++) {
									name+=args[i]+" ";
								}
								name = name.substring(0, name.length()-1);
								final String term = name.toLowerCase();
								List<Song> ss = new ArrayList<>(SongManager.getSongs());
								ss.sort(new Comparator<Song>() {
									@Override
									public int compare(Song o1, Song o2) {
										return (int) (Tools.similarity(term, o1.getName().toLowerCase())*100-Tools.similarity(term, o2.getName().toLowerCase())*100);
									}
								}.reversed());
								p.sendMessage(Config.getMessage("search-results"));
								if(!ss.isEmpty()) {
									for(int i = 0; i < (ss.size() < Config.result_amount?ss.size():Config.result_amount); i++) {
										Song s = ss.get(i);
										p.sendMessage(Config.getMessage("search-results-entry").replace("%song-id%", ""+s.getID()).replace("%song-author%", (s.getOriginalAuthor()!=null?s.getOriginalAuthor():Config.default_author)).replace("%song-name%", s.getName()).replace("%song-by%", (s.getAuthor()!=null?s.getAuthor():Config.default_author)));
									}
								}else {
									p.sendMessage(Config.getMessage("search-results-empty"));
								}
							}else {
								sendCommandHelp(p, null);
							}
						}else {
							p.sendMessage(Config.getMessage("creation-disabled"));
						}
					}else if(args[0].equalsIgnoreCase("playlist")) {
						RadioStation r = RadioStations.getRadioStationListening(p);
						if(r!=null) {
							p.sendMessage(Config.getMessage("station.playlist").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()).replace("%loop%", ""+r.isLooping()));
							if(!r.getPlaylist().isEmpty()) {
								int i = 0;
								for(Integer e : r.getPlaylist()) {
									Song s = SongManager.getSongByID(e);
									if(s!=null) {
										if(r.getCurrentIndex()>=0 && r.getCurrentIndex()==i && r.isRunning()) {
											p.sendMessage(Config.getMessage("station.playlist-entry-playing").replace("%index%", ""+i).replace("%song-id%", ""+s.getID()).replace("%song-name%", s.getName()));
										}else {
											p.sendMessage(Config.getMessage("station.playlist-entry").replace("%index%", ""+i).replace("%song-id%", ""+s.getID()).replace("%song-name%", s.getName()));
										}
									}
									i++;
								}
							}else {
								p.sendMessage(Config.getMessage("station.playlist-empty").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()));
							}
						}else {
							p.sendMessage(Config.getMessage("not-listening"));
						}
					}else if(args[0].equalsIgnoreCase("export")) {
						if(p.hasPermission(Config.PERM_EXPORT)) {
							if(args.length==3) {
								try {
									String eMode = args[2];
									SongLoader l = SongManager.getSongLoader(eMode);
									SongLoader aL = eMode.toLowerCase().endsWith("-archive") ? SongManager.getSongLoader(eMode.substring(0, eMode.length() - "-archive".length())) : null;
									if(l == null && (aL == null || !aL.supportsSongArchives()) && !eMode.equalsIgnoreCase("settings")) {
										sendCommandHelp(p, null);
										return true;
									}
									if(args[1].equalsIgnoreCase("all")) {
										if(p.hasPermission(Config.PERM_EXPORT_ALL)) {
											if(!exportRunning) {
												p.sendMessage(Config.getMessage("export.wait-all").replace("%count%", ""+SongManager.getSongs().size()));
												exportRunning = true;
												if(SongManager.getSongs().size()>=Config.thread_on_process_when) {
													tempProcessThread = new Thread(new Runnable() {
														
														@Override
														public void run() {
															playerExportAll(p, eMode, true);
															tempProcessThread = null;
														}
													}, "PlayerRadios-Process-Thread");
													tempProcessThread.start();
												}else {
													playerExportAll(p, eMode, false);
												}
											}else {
												p.sendMessage(Config.getMessage("process-already-running"));
											}
										}else {
											sendCommandHelp(p, null);
										}
									}else {
										if(eMode.equalsIgnoreCase("sng-archive")){
											p.sendMessage(Config.getMessage("export.not-available"));
											return true;
										}
										Song s = SongManager.getSongByID(Integer.parseInt(args[1]));
										if(s == null) {
											p.sendMessage(Config.getMessage("export.invalid-song"));
											return true;
										}
										try {
											p.sendMessage(Config.getMessage("export.wait", "song-name", s.getName()));
											File f = null;
											if(eMode.equalsIgnoreCase("settings")) {
												f = SongManager.getConfigFile(s.getID());
												SongManager.setDefaultSongSettings(s);
											}else {
												f = l.getSongExportFile(s);
												l.saveSongs(f, s);
											}
											p.sendMessage(Config.getMessage("export.done").replace("%file-name%", f.getName()));
										} catch (Exception e) {
											e.printStackTrace();
											p.sendMessage(Config.getMessage("export.failed"));
										}
									}
								}catch(NumberFormatException e) {
									sendCommandHelp(p, null);
								}
							}else {
								sendCommandHelp(p, null);
							}
						}else {
							sendCommandHelp(p, null);
						}
					}/*else if(args[0].equalsIgnoreCase("songinfo")) {
						if(Config.allow_create_stations || p.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
							if(args.length==2) {
								try {
									int sID = Integer.parseInt(args[1]);
									Song s = SongManager.getSongByID(sID);
									
								}catch(Exception e) {
									sendCommandHelp(p, null);
								}
							}else {
								sendCommandHelp(p, null);
							}
						}else {
							p.sendMessage(Config.getMessage("creation-disabled"));
						}
					}*/
					
					//Disableable commands
					else if(Config.disable_commands) {
						p.sendMessage(Config.getMessage("commands-disabled"));
						return true;
					}else if (args[0].equalsIgnoreCase("station")) {
						if(Config.allow_create_stations || p.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
							if(args.length>=3 && args[1].equalsIgnoreCase("create")) {
								String name = "";
								for(int i = 2; i < args.length; i++) {
									name+=args[i]+" ";
								}
								name = name.substring(0, name.length()-1);
								if (name.length() <= Config.max_station_name_length) {
									if(StationManager.getRadioStationsByPlayer(p).size()<Config.max_stations_per_player) {
										RadioStation r = StationManager.createStation(p, name);
										p.sendMessage(Config.getMessage("station-created.1", "id", ""+r.getID(), "station-name", r.getName()));
										p.sendMessage(Config.getMessage("station-created.2", "id", ""+r.getID()));
									}else {
										p.sendMessage(Config.getMessage("station.too-many-stations"));
									}
								}else {
									p.sendMessage(Config.getMessage("station.name-too-long"));
								}
							}else if(args.length==2 && args[1].equalsIgnoreCase("list")) {
								p.sendMessage(Config.getMessage("stations"));
								List<RadioStation> ss = StationManager.getRadioStationsByPlayer(p);
								if(!ss.isEmpty()) {
									for(RadioStation r : ss) {
										p.sendMessage(Config.getMessage("stations-entry", "station-id", ""+r.getID(), "station-name", r.getName()));
									}
								}else {
									p.sendMessage(Config.getMessage("stations-empty"));
								}
							}else if (args.length >= 2) {
								int rsID;
								try {
									rsID = Integer.parseInt(args[1]);
								}catch (Exception e) {
									sendCommandHelp(p, "station");
									return true;
								}
								RadioStation r = StationManager.getRadioStation(rsID);
								if(r==null) {
									p.sendMessage(Config.getMessage("station.doesnt-exist"));
									return true;
								}
								if(!r.isOwner(p) && !p.hasPermission(Config.PERM_EDIT_OTHER)) {
									p.sendMessage(Config.getMessage("station.not-your-station"));
									return true;
								}
								if(args.length==2) {
									p.openInventory(GUIs.getStationGUI(p, r.getID(), 0));
								}else if(args.length>=3) {
									if (args[2].equalsIgnoreCase("playlist")) {
										if(args.length == 5) {
											if(!r.isRunning()) {
												if(args[3].equalsIgnoreCase("add")) {
													try {
														int sID = Integer.parseInt(args[4]);
														Song s = SongManager.getSongByID(sID);
														if(s!=null) {
															r.addSong(s.getID());
															p.sendMessage(Config.getMessage("station.song-added"));
														}else {
															p.sendMessage(Config.getMessage("station.song-doesnt-exist"));
														}
													}catch(Exception e) {
														sendCommandHelp(p, "station");
													}
												}else if(args[3].equalsIgnoreCase("remove")) {
													try {
														int index = Integer.parseInt(args[4]);
														if(index>=0 && index < r.getPlaylist().size()) {
															r.removeSong(index);
															p.sendMessage(Config.getMessage("station.song-removed"));
															StationManager.updateStationGUI(r.getID());
														}else {
															p.sendMessage(Config.getMessage("station.song-not-on-playlist"));
														}
													}catch(Exception e) {
														sendCommandHelp(p, "station");
													}
												}else {
													sendCommandHelp(p, "station");
												}
											}else {
												p.sendMessage(Config.getMessage("station.cannot-modify"));
											}
										}else if(args.length == 3){
											p.sendMessage(Config.getMessage("station.playlist").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()).replace("%loop%", ""+r.isLooping()));
											if(!r.getPlaylist().isEmpty()) {
												int i = 0;
												for(Integer e : r.getPlaylist()) {
													Song s = SongManager.getSongByID(e);
													if(s!=null) {
														if(r.getCurrentIndex()>=0 && r.getCurrentIndex()==i && r.isRunning()) {
															p.sendMessage(Config.getMessage("station.playlist-entry-playing").replace("%index%", ""+i).replace("%song-id%", ""+s.getID()).replace("%song-name%", s.getName()));
														}else {
															p.sendMessage(Config.getMessage("station.playlist-entry").replace("%index%", ""+i).replace("%song-id%", ""+s.getID()).replace("%song-name%", s.getName()));
														}
													}
													i++;
												}
											}else {
												p.sendMessage(Config.getMessage("station.playlist-empty").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()));
											}
										}else {
											sendCommandHelp(p, "station");
										}
									}else if(args[2].equalsIgnoreCase("start")) {
										if(!r.isRunning()) {
											r.setRunning(true);
											p.sendMessage(Config.getMessage("station.started"));
										}else {
											p.sendMessage(Config.getMessage("station.already-running"));
										}
									}else if(args[2].equalsIgnoreCase("stop")) {
										if(r.isRunning()) {
											r.setRunning(false);
											p.sendMessage(Config.getMessage("station.stopped"));
										}else {
											p.sendMessage(Config.getMessage("station.not-running"));
										}
									}else if(args[2].equalsIgnoreCase("set")) {
										if(args.length>=5) {
											String setting = args[3];
											if(setting.equalsIgnoreCase("name")) {
												String name = "";
												for(int i = 4; i < args.length; i++) {
													name+=args[i]+" ";
												}
												name = name.substring(0, name.length()-1);
												if(name.length() <= Config.max_station_name_length) {
													String oName = r.getName();
													r.setName(name);
													p.sendMessage(Config.getMessage("station.set.name").replace("%old-name%", oName).replace("%new-name%", name));
												}else {
													p.sendMessage(Config.getMessage("station.name-too-long"));
												}
											}else if(setting.equalsIgnoreCase("loop")) {
												if(args.length==5) {
													boolean bool = Tools.stringToBoolean(args[4]);
													r.setLoop(bool);
													if(bool) {
														p.sendMessage(Config.getMessage("station.set.loop.enable"));
													}else {
														p.sendMessage(Config.getMessage("station.set.loop.disable"));
													}
												}else {
													sendCommandHelp(p, "station");
												}
											}else {
												sendCommandHelp(p, "station");
											}
										}else {
											sendCommandHelp(p, "station");
										}
									}else if(args[2].equalsIgnoreCase("skip")) {
										if(r.isRunning()) {
											r.skipTrack();
											p.sendMessage(Config.getMessage("station.track-skipped"));
										}else {
											p.sendMessage(Config.getMessage("station.not-running"));
										}
									}else if(args[2].equalsIgnoreCase("delete")) {
										StationManager.deleteRadioStation(r.getID());
										p.sendMessage(Config.getMessage("station.deleted").replace("%station-id%", ""+r.getID()).replace("%station-name%", r.getName()));
									}else {
										sendCommandHelp(p, "station");
									}
								}
							} else {
								sendCommandHelp(p, "station");
							}
						}else {
							p.sendMessage(Config.getMessage("creation-disabled"));
						}
					}else {
						sendCommandHelp(p, null);
					}
				}else if(args.length == 0) {
					p.openInventory(GUIs.getStationsGUI(p, null));
				} else {
					sendCommandHelp(sender, null);
				}
			} else {
				sender.sendMessage("§cOnly players can use this command");
			}
		}
		return true;
	}

	private void sendCommandHelp(CommandSender sender, String topic) {
		sender.sendMessage(Config.getMessage("help.header").replace("%topic%", (topic!=null?StringUtils.capitalize(topic):"General")));
		if(topic == null) {
			sender.sendMessage(Config.getHelpMessage("pr", "pr", ""));
			sender.sendMessage(Config.getHelpMessage("pr-help", "pr help", " [topic]"));
			if(!Config.disable_commands || !Config.disable_commands_all) {
				sender.sendMessage(Config.getHelpMessage("pr-playlist", "pr playlist", ""));
				if(sender.hasPermission(Config.PERM_EXPORT)) {
					sender.sendMessage(Config.getHelpMessage("pr-export", "pr export", " <Song-ID"+(sender.hasPermission(Config.PERM_EXPORT_ALL)?"/all":"")+"> <"
							+ SongManager.getSongLoaders().stream().map(l -> l.getName() + (l.supportsSongArchives() ? "/" + l.getName() + "-archive" : "")).collect(Collectors.joining("/")) + ">"));
				}
				if((Config.enable_submit && (!Config.submit_needs_perm || sender.hasPermission(Config.PERM_SUBMIT))) || sender.hasPermission(Config.PERM_SUBMIT_WHEN_DISABLED)) {
					sender.sendMessage(Config.getHelpMessage("pr-submit", "pr submit", " <Link>"));
				}
			}
			if(sender.hasPermission(Config.PERM_NOTIFY_UPDATE)) {
				sender.sendMessage(Config.getHelpMessage("pr-version", "pr version", ""));
			}
			if(sender.hasPermission(Config.PERM_RELOAD)) {
				sender.sendMessage(Config.getHelpMessage("pr-reload", "pr <reload/rl>", ""));
			}
			if(sender.hasPermission(Config.PERM_ALLOW_BUGREPORT)) {
				sender.sendMessage(Config.getHelpMessage("pr-bugreport", "pr bugreport", ""));
			}
			if(!Config.disable_commands) {
				if(Config.allow_create_stations || sender.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
					sender.sendMessage(Config.getMessage("help.use-topic-stations"));
				}
			}
		}else if(topic.equalsIgnoreCase("stations") || topic.equalsIgnoreCase("station")) {
			if(!Config.disable_commands && (Config.allow_create_stations || sender.hasPermission(Config.PERM_CREATE_WHEN_DISABLED))) {
				sender.sendMessage(Config.getHelpMessage("pr-search", "pr search", " <Song name>"));
				sender.sendMessage(Config.getHelpMessage("pr-station-create", "pr station create", " <Name>"));
				sender.sendMessage(Config.getHelpMessage("pr-station-list", "pr station list", ""));
				sender.sendMessage(Config.getHelpMessage("pr-station", "pr station", " <ID>"));
				sender.sendMessage(Config.getHelpMessage("pr-station-playlist", "pr station <ID> playlist", ""));
				sender.sendMessage(Config.getHelpMessage("pr-station-playlist-add", "pr station <ID> playlist add", " <Song ID>"));
				sender.sendMessage(Config.getHelpMessage("pr-station-playlist-remove", "pr station <ID> playlist remove", " <Index>"));
				sender.sendMessage(Config.getHelpMessage("pr-station-set", "pr station <ID> set", " <name/loop> <Value>"));
				sender.sendMessage(Config.getHelpMessage("pr-station-start-stop", "pr station <ID> <start/stop>", ""));
				sender.sendMessage(Config.getHelpMessage("pr-station-skip", "pr station <ID> skip", ""));
				sender.sendMessage(Config.getHelpMessage("pr-station-delete", "pr station <ID> delete", ""));
			}else{
				sender.sendMessage(Config.getMessage("help.invalid-topic").replace("%topic%", StringUtils.capitalize(topic)));
			}
		}else {
			sender.sendMessage(Config.getMessage("help.invalid-topic").replace("%topic%", StringUtils.capitalize(topic)));
		}
	}
	
	private void playerExportAll(Player p, String format, boolean threadMode) {
		long t = System.currentTimeMillis();
		Map.Entry<Integer, List<Entry<Integer, SongLoadingException>>> r = saveAllSongs(format, threadMode);
		exportRunning = false;
		p.sendMessage(Config.getMessage("export.done-all", "count", ""+r.getKey(), "time", Tools.timeTaken(t, System.currentTimeMillis(), true), "failed", ""+(SongManager.getSongs().size() - r.getKey())));
		for(Map.Entry<Integer, SongLoadingException> en : r.getValue()) {
			if(r.getKey() == -1) {
				p.sendMessage(Config.getMessage("export.error-line-all", "error", en.getValue().getMessage()));
			}else {
				p.sendMessage(Config.getMessage("export.error-line", "song", ""+en.getKey(), "error", en.getValue().getMessage()));
			}
		}
	}
	
	private Map.Entry<Integer, List<Map.Entry<Integer, SongLoadingException>>> saveAllSongs(String format, boolean checkInterrupt) {
		int c = 0;
		List<Map.Entry<Integer, SongLoadingException>> exs = new ArrayList<>();
		if(format.equalsIgnoreCase("settings")) {
			for(Song s : SongManager.getSongs()) {
				SongManager.setDefaultSongSettings(s);
				c++;
			}
		}else if(format.toLowerCase().endsWith("-archive")) {
			String rF = format.substring(0, format.length() - "-archive".length());
			SongLoader l = SongManager.getSongLoader(rF);
			if(l == null || !l.supportsSongArchives()) return MiscUtils.newMapEntry(0, Collections.singletonList(
						MiscUtils.newMapEntry(-1, new SongLoadingException("Song loader not found or doesn't support song archives"))
					));
			try {
				l.saveSongs(new File(l.getSongExportFolder(), "all-songs." + l.getFileExtension()), SongManager.getSongs().stream().toArray(Song[]::new));
				c = SongManager.getSongs().size();
			}catch(SongLoadingException e) {
				return MiscUtils.newMapEntry(0, Collections.singletonList(
						MiscUtils.newMapEntry(-1, e)
					));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			SongLoader l = SongManager.getSongLoader(format);
			if(l == null) return MiscUtils.newMapEntry(0, Collections.singletonList(
					MiscUtils.newMapEntry(0, new SongLoadingException("Song loader not found"))
				));
			for(Song s : SongManager.getSongs()) {
				if(checkInterrupt && Thread.interrupted()) break;
				try {
					l.saveSongs(l.getSongExportFile(s), s);
					c++;
				}catch(SongLoadingException e) {
					exs.add(MiscUtils.newMapEntry(s.getID(), e));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return MiscUtils.newMapEntry(c, exs);
	}

}
