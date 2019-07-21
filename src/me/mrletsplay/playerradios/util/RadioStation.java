package me.mrletsplay.playerradios.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.mrletsplay.mrcore.bukkitimpl.versioned.NMSVersion;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.song.Layer;
import me.mrletsplay.playerradios.util.song.Note;
import me.mrletsplay.playerradios.util.song.NotePitch;
import me.mrletsplay.playerradios.util.song.Song;

public class RadioStation {

	private String name, owner;
	private List<Integer> playlist;
	private List<Player> players;
	private List<String> broadcasts;
	private HashMap<Player, Integer> listenTicks = new HashMap<>();
	private int id, currIndex;
	private boolean isRunning, loop, dontdelete, blacklistMode;
	private ItemStack icon;
	
	private Iterator<Integer> sI;
	private int lastBcIndex;
	private Song currSong;
	private int sleepingTicks, tick;
	
	public RadioStation(String owner, String name, int id) {
		this.owner = owner;
		this.name = name;
		this.id = id;
		this.playlist = new CopyOnWriteArrayList<>();
		this.players = new CopyOnWriteArrayList<>();
		this.lastBcIndex = -1;
		this.broadcasts = new ArrayList<>();
	}
	
	public RadioStation(String owner, String name, int id, boolean loop, int currIndex, List<Integer> playlist, List<String> broadcasts, boolean isRunning, ItemStack icon, boolean dontdelete, boolean blacklistMode) {
		this.owner = owner;
		this.name = name;
		this.id = id;
		this.playlist = new CopyOnWriteArrayList<>();
		this.players = new CopyOnWriteArrayList<>();
		this.loop = loop;
		this.currIndex = currIndex;
		this.playlist = playlist;
		this.isRunning = isRunning;
		this.icon = icon;
		this.lastBcIndex = -1;
		this.broadcasts = broadcasts;
		this.dontdelete = dontdelete;
		this.blacklistMode = blacklistMode;
	}

	public void setName(String name) {
		this.name = name;
		StationManager.updateStationGUI(id);
		StationManager.updateStationsGUI();
	}
	
	public String getName() {
		return name;
	}
	
	public void setIcon(ItemStack icon) {
		this.icon = icon;
		StationManager.updateStationGUI(id);
	}
	
	public void setPlaylist(List<Integer> playlist) {
		this.playlist = playlist;
		StationManager.updateStationGUI(id);
	}
	
	public List<Integer> getPlaylist() {
		return playlist;
	}
	
	public int getID() {
		return id;
	}
	
	public List<Player> getPlayers() {
		return players;
	}
	
	public List<String> getBroadcasts() {
		return broadcasts;
	}
	
	public void addBroadcast(String broadcast) {
		broadcasts.add(broadcast);
	}
	
	public void removeBroadcast(int index) {
		broadcasts.remove(index);
	}
	
	public void setBroadcasts(List<String> broadcasts) {
		this.broadcasts = broadcasts;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public ItemStack getIcon() {
		return icon;
	}
	
	public void addPlayerListening(Player p) {
		players.add(p);
		StationManager.updateStationGUI(id);
	}
	
	public void removePlayerListening(Player p) {
		players.remove(p);
	}
	
	public void addSong(Integer songID) {
		playlist.add(songID);
		StationManager.updateStationGUI(id);
	}
	
	public void removeSong(int index) {
		playlist.remove(index);
	}
	
	public Song getCurrentSong() {
		return currSong;
	}
	
	public int getCurrentIndex() {
		return currIndex;
	}
	
	public void setCurrentIndex(int currIndex) {
		this.currIndex = currIndex;
	}
	
	public void skipTrack() {
		nextSong();
	}
	
	public boolean isDontDelete() {
		return dontdelete;
	}
	
	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
		
		Bukkit.getScheduler().runTask(Main.pl, new Runnable() {
			
			@Override
			public void run() {
				StationManager.updateStationGUI(id, currIndex);
			}
		});
		if(!isRunning) {
			tick = 0;
			this.currIndex = -1;
			sI = null;
			currSong = null;
			for(Player p : players) {
				StationManager.stop(p);
				p.sendMessage(Config.getMessage("stopped-broadcasting").replace("%station-name%", name));
			}
		}
		StationManager.updateStationsGUI();
	}
	
	public void stop() {
		setRunning(false);
	}
	
	public void start() {
		setRunning(true);
	}
	
	public String getOwner() {
		return owner;
	}
	
	public HashMap<Player, Integer> getListenTicks() {
		return listenTicks;
	}
	
	public int getListenTicks(Player p) {
		return listenTicks.getOrDefault(p,0);
	}
	
	public int getListenMs(Player p) {
		return listenTicks.getOrDefault(p,0) * Main.tickTimeMs;
	}
	
	public void setLoop(boolean loop) {
		this.loop = loop;
		StationManager.updateStationGUI(id);
	}
	
	public boolean isLooping() {
		return loop;
	}
	
	public String getOwnerName() {
		if(owner!=null) {
			if(Config.use_uuids){
				OfflinePlayer pl = Bukkit.getOfflinePlayer(UUID.fromString(owner));
				if(pl!=null) {
					return pl.getName();
				}else {
					return "Invalid";
				}
			}else {
				return owner;
			}
		}else {
			return "None";
		}
	}
	
	public boolean isOwner(Player p) {
		return getOwner().equals((Config.use_uuids?p.getUniqueId().toString():p.getName())) || p.hasPermission(Config.PERM_EDIT_OTHER);
	}

	public void tick() {
		for(Player p : players) {
			listenTicks.put(p, listenTicks.getOrDefault(p, 0)+1);
		}
		if(sleepingTicks>0) {
			sleepingTicks--;
			return;
		}
		Iterator<Player> pI = listenTicks.keySet().iterator();
		while(pI.hasNext()) {
			Player p = pI.next();
			if(!players.contains(p)) {
				pI.remove();
			}
		}
		if(currSong != null && !broadcasts.isEmpty() && tick==(currSong.getLength()/2)) {
			lastBcIndex++;
			if(lastBcIndex>=broadcasts.size()) {
				lastBcIndex = 0;
			}
			players.forEach(p -> p.sendMessage(Config.getMessage("station-broadcast").replace("%message%", broadcasts.get(lastBcIndex))));
		}
		if(sI==null) {
			sI = blacklistMode?null:playlist.iterator();
			if(currIndex>0) {
				for(int i = 0; i < currIndex; i++) {
					if(!nextSong()) {
						return;
					}
				}
				if(!nextSong()) {
					return;
				}
			}
		}
		if(currSong==null || tick>=currSong.getLength()) {
			nextSong();
			return;
		}
		for(int t = tick; t < tick+1; t++) {
			for(int l = 0; l < currSong.getHeight(); l++) {
				Note n = currSong.getNoteAt(t, l);
				if(n!=null) {
					Layer layer = currSong.getLayers().get(l);
					int vol = layer.getVolume();
					if(n.getVolume() > 0) vol = n.getVolume();
					if((!n.isCustom() && n.getSound()!=null) || (n.isCustom() && n.getCustomSound()!=null)) {
						for(Player p : players) {
							if(!n.isCustom()) {
								p.playSound(p.getLocation().add(p.getLocation().getDirection().normalize()), n.getSound().getBukkitSound(), vol / 10f, NotePitch.getPitch(n.getNote()));
							}else {
								p.playSound(p.getLocation().add(p.getLocation().getDirection().normalize()), n.getCustomSound(), vol / 10f, NotePitch.getPitch(n.getNote()));
							}
							if(Config.enable_particles) {
								if(NMSVersion.getCurrentServerVersion().isOlderThan(NMSVersion.V1_13_R1)) {
									p.getWorld().playEffect(p.getLocation().add(0, (p.isSneaking()?1.9:2.2), 0), org.bukkit.Effect.valueOf("NOTE"), 0);
								}else {
									p.getWorld().spawnParticle(org.bukkit.Particle.NOTE, p.getLocation().add(0, (p.isSneaking()?1.9:2.2), 0), 1);
								}
							}
						}
					}
				}
			}
		}
		float slTicks = (Config.use_fixed_playback?100:20)/currSong.getTPS();
		tick++;
		sleepingTicks = (int) (slTicks);
	}
	
	private boolean nextSong() {
		tick = 0;
		if(blacklistMode) {
			currSong = null;
			if(SongManager.getSongs().stream().allMatch(s -> playlist.contains(s.getID()))) {
				stop();
				return false;
			}
			while(currSong==null || playlist.contains(currSong.getID())) {
				currSong = SongManager.getSongs().get(Main.random.nextInt(SongManager.getSongs().size()));
			}
			for(Player p : players) {
				p.sendMessage(Config.getMessage("song-changed").replace("%station-name%", name).replace("%song-name%", currSong.getName()));
			}
			sleepingTicks = Config.sleep_ticks;
			StationManager.updateStationGUI(id);
			StationManager.updateStationsGUI();
			return true;
		}
		if(!playlist.isEmpty()) {
			if(sI!=null && sI.hasNext()) {
				currIndex++;
				currSong = SongManager.getSongByID(sI.next());
				for(Player p : players) {
					p.sendMessage(Config.getMessage("song-changed").replace("%station-name%", name).replace("%song-name%", currSong.getName()));
				}
				sleepingTicks = Config.sleep_ticks;
				StationManager.updateStationGUI(id);
				StationManager.updateStationsGUI();
				return true;
			}else {
				if(!loop) {
					stop();
					return false;
				}else {
					currIndex = 0;
					sI = playlist.iterator();
					currSong = SongManager.getSongByID(sI.next());
					for(Player p : players) {
						p.sendMessage(Config.getMessage("song-changed").replace("%station-name%", name).replace("%song-name%", currSong.getName()));
					}
					sleepingTicks = Config.sleep_ticks;
					StationManager.updateStationGUI(id);
					StationManager.updateStationsGUI();
					return true;
				}
			}
		}else {
			stop();
			return false;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof RadioStation) return ((RadioStation)obj).id == id;
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	public void setBlacklistMode(boolean blacklistMode) {
		this.blacklistMode = blacklistMode;
	}
	
	public boolean isBlacklistMode() {
		return blacklistMode;
	}
	
}
