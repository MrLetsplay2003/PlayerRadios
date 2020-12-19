package me.mrletsplay.playerradios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import me.mrletsplay.playerradios.command.CommandPlayerRadios;
import me.mrletsplay.playerradios.util.PlayerRadiosPlaceholder;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.RadioStations;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.Tools;
import me.mrletsplay.playerradios.util.UpdateChecker;
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

	public static Plugin pl;
	public static String pluginVersion;
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
		pluginVersion = getDescription().getVersion();
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
			getLogger().info("Current version: "+pluginVersion+", Newest version: "+res.updVer);
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
			// TODO: fix
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
		getCommand("playerradios").setExecutor(new CommandPlayerRadios());
		getLogger().info("Enabled PlayerRadios v"+pluginVersion);
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
						
						return true;
					}else if(args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
						
					}else if(args[0].equalsIgnoreCase("bugreport")) {
						
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
						
					}else if(args[0].equalsIgnoreCase("search")) {
						
					}else if(args[0].equalsIgnoreCase("playlist")) {
						
					}else if(args[0].equalsIgnoreCase("export")) {
						
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
								
							}else if(args.length==2 && args[1].equalsIgnoreCase("list")) {
								
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
													
												}else if(args[3].equalsIgnoreCase("remove")) {
													
												}else {
													sendCommandHelp(p, "station");
												}
											}else {
												p.sendMessage(Config.getMessage("station.cannot-modify"));
											}
										}else if(args.length == 3){
											
										}else {
											sendCommandHelp(p, "station");
										}
									}else if(args[2].equalsIgnoreCase("start")) {
										
									}else if(args[2].equalsIgnoreCase("stop")) {
										
									}else if(args[2].equalsIgnoreCase("set")) {
										if(args.length>=5) {
											String setting = args[3];
											if(setting.equalsIgnoreCase("name")) {
												
											}else if(setting.equalsIgnoreCase("loop")) {
												if(args.length==5) {
													
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
										
									}else if(args[2].equalsIgnoreCase("delete")) {
										
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

	public static void sendCommandHelp(CommandSender sender, String topic) {
		sender.sendMessage(Config.getMessage("help.header").replace("%topic%", (topic!=null?StringUtils.capitalize(topic):"General")));
		if(topic == null) {
			sender.sendMessage(Config.getHelpMessage("pr", "pr", ""));
			sender.sendMessage(Config.getHelpMessage("pr-help", "pr help", " [topic]"));
			if(!Config.disable_commands || !Config.disable_commands_all) {
				sender.sendMessage(Config.getHelpMessage("pr-playlist", "pr playlist", ""));
				if(sender.hasPermission(Config.PERM_EXPORT)) {
					sender.sendMessage(Config.getHelpMessage("pr-export", "pr export", " <Song-ID"+(sender.hasPermission(Config.PERM_EXPORT_ALL)?"/all":"")+"> <"
							+ SongManager.getSongLoaders().stream().map(l -> l.getName() + (l.supportsSongArchives() ? "/" + l.getName() + "-archive" : "")).collect(Collectors.joining("/")) + "/settings>"));
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
	
	public static void playerExportAll(Player p, String format, boolean threadMode) {
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
	
	public static Map.Entry<Integer, List<Map.Entry<Integer, SongLoadingException>>> saveAllSongs(String format, boolean checkInterrupt) {
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
