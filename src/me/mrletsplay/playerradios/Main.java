package me.mrletsplay.playerradios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
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
		getCommand("playerradios").setExecutor(CommandPlayerRadios.INSTANCE);
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
