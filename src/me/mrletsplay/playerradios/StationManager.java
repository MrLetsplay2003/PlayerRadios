package me.mrletsplay.playerradios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.mrletsplay.mrcore.bukkitimpl.GUIUtils;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIHolder;
import me.mrletsplay.mrcore.bukkitimpl.ItemUtils;
import me.mrletsplay.mrcore.bukkitimpl.config.BukkitCustomConfig;
import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.RadioStations;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.Tools;

public class StationManager {

	public static File stationFile = new File(Main.pl.getDataFolder(), "stations.yml");
	public static BukkitCustomConfig stations = ConfigLoader.loadConfigFromFile(new BukkitCustomConfig(stationFile), stationFile, true);
	public static File ostationFile = new File(Main.pl.getDataFolder(), "disabled-stations.yml");
	public static BukkitCustomConfig ostations = ConfigLoader.loadConfigFromFile(new BukkitCustomConfig(ostationFile), ostationFile, true);

	@SuppressWarnings("deprecation")
	public static void init() {
		stations.setHeader("This is the stations file. Do not edit anything unless you know what you're doing! (or at least don't come complaining to me if something breaks :D)");
		RadioStations.stations.clear();
		Main.pl.getLogger().info("Loading radio station(s)...");
		for (String key : stations.getKeys("station", false, false)) {
			try {
				String owner = stations.getString("station." + key + ".owner");
				if(!Config.use_uuids && Tools.isUUID(owner)) {
					Main.pl.getLogger().info("Converting station "+key+"'s owner uuid to name...");
					owner = Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName();
				}else if(Config.use_uuids && !Tools.isUUID(owner)){
					Main.pl.getLogger().info("Converting station "+key+"'s owner name to uuid...");
					owner = Bukkit.getOfflinePlayer(owner).getUniqueId().toString();
				}
				boolean dontdelete = stations.getBoolean("station." + key + ".dontautodelete");
				String name = stations.getString("station." + key + ".name");
				List<Integer> playlist = stations.getIntegerList("station." + key + ".playlist");
				int currIndex = stations.getInt("station." + key + ".currentIndex");
				boolean isRunning = stations.getBoolean("station." + key + ".isRunning");
				boolean loop = stations.getBoolean("station." + key + ".loop");
				List<String> bcs = stations.getStringList("station." + key + ".broadcasts");
				boolean blacklistMode = stations.getBoolean("station."+key+".blacklistMode");
				int rID = Integer.parseInt(key);
				for(int i = 0; i < playlist.size();  i++) {
					Integer sID = playlist.get(i);
					if(SongManager.getSongByID(sID)==null) {
						Main.pl.getLogger().info("Found invalid song "+sID+" in station "+rID+"'s playlist, removing...");
						currIndex = -1;
						playlist.remove(i);
					}
				}
				ItemStack icon = stations.getItemStack("station." + key + ".icon");
				if(icon == null) icon = ItemUtils.createItem(Material.DIAMOND, 1, 0, null);
				if(!dontdelete && Config.autodelete && System.currentTimeMillis()-PlayerManager.getLastOnlineTime(owner) >= Config.autodelete_days*1000*60*60*24) {
					Main.pl.getLogger().info("Deleting station "+key+" because of user inactivity");
					Main.pl.getLogger().info("The station's settings will be backed up to the \"disabled-stations.yml\" file");
					ostations.set("station." + key + ".owner", owner);
					ostations.set("station." + key + ".name", name);
					ostations.set("station."+key+".playlist", playlist);
					ostations.set("station."+key+".isRunning", isRunning);
					ostations.set("station."+key+".loop", loop);
					ostations.set("station."+key+".currentIndex", currIndex);
					ostations.set("station."+key+".icon", icon);
					ostations.set("station."+key+".broadcasts", bcs);
					ostations.set("station."+key+".dontautodelete", dontdelete);
					ostations.set("station."+key+".blacklistMode", blacklistMode);
					stations.set("station."+key, null);
					continue;
				}
				RadioStation r = new RadioStation(owner, name, rID, loop, currIndex, playlist, bcs, isRunning, icon, dontdelete, blacklistMode);
				RadioStations.stations.add(r);
			} catch (Exception e) {
				Main.pl.getLogger().info("Failed to load radio station \"" + key + "\", skipping...");
				e.printStackTrace();
			}
		}
		save();
		saveOld();
		Main.pl.getLogger().info("Loaded "+RadioStations.stations.size()+" station(s)");
	}

	public static void saveAll() {
		for (RadioStation r : RadioStations.stations) {
			if (r.getOwner() != null)
				stations.set("station." + r.getID() + ".owner", r.getOwner());
			stations.set("station." + r.getID() + ".name", r.getName());
			stations.set("station."+r.getID()+".playlist", r.getPlaylist());
			stations.set("station."+r.getID()+".isRunning", r.isRunning());
			stations.set("station."+r.getID()+".loop", r.isLooping());
			stations.set("station."+r.getID()+".currentIndex", r.getCurrentIndex());
			stations.set("station."+r.getID()+".icon", r.getIcon());
			stations.set("station."+r.getID()+".broadcasts", r.getBroadcasts());
			stations.set("station."+r.getID()+".dontautodelete", r.isDontDelete());
			stations.set("station."+r.getID()+".blacklistMode", r.isBlacklistMode());
		}
		save();
	}
	
	public static boolean stop(Player p) {
		boolean is = false;
		for (RadioStation rs : RadioStations.stations) {
			if (rs.getPlayers().contains(p)) {
				rs.removePlayerListening(p);
				is = true;
			}
		}
		return is;
	}

	public static void save() {
		stations.saveToFile();
	}

	public static void saveOld() {
		ostations.saveToFile();
	}

	public static RadioStation createStation(Player owner, String name) {
		String pl;
		if(Config.use_uuids) {
			pl = owner.getUniqueId().toString();
		}else {
			pl = owner.getName();
		}
		RadioStation r = new RadioStation(pl, name, newID());
		r.setIcon(new ItemStack(Material.DIAMOND));
		RadioStations.stations.add(r);
		updateStationsGUI();
		return r;
	}
	
	public static void deleteRadioStation(int id) {
		RadioStation r = getRadioStation(id);
		if(r!=null) {
			if(r.isRunning()) {
				r.stop();
			}
			RadioStations.stations.remove(r);
			stations.set("station."+id, null);
		}
	}
	
	private static int newID() {
		List<Integer> ids = getRadioIDs();
		int id = 0;
		while(ids.contains(id)) {
			id++;
		}
		return id;
	}

	public static RadioStation getRadioStation(int id) {
		for (RadioStation r : RadioStations.stations) {
			if (r.getID() == id) {
				return r;
			}
		}
		return null;
	}

	public static List<Integer> getRadioIDs() {
		List<Integer> ids = new ArrayList<>();
		for(RadioStation r : RadioStations.stations) {
			ids.add(r.getID());
		}
		return ids;
	}

	@SuppressWarnings("deprecation")
	public static List<RadioStation> getRadioStationsByPlayerName(String playerName) {
		String pl = playerName;
		if(Config.use_uuids) {
			OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
			if(p==null) return new ArrayList<>();
			pl = p.getUniqueId().toString();
		}
		List<RadioStation> rs = new ArrayList<>();
		for (RadioStation r : RadioStations.stations) {
			if (r.getOwner() != null && r.getOwner().equals(pl)) {
				rs.add(r);
			}
		}
		return rs;
	}
	
	public static List<RadioStation> getRadioStationsByPlayerNameOrUUID(String playerNameOrUUID) {
		List<RadioStation> rs = new ArrayList<>();
		for (RadioStation r : RadioStations.stations) {
			if (r.getOwner() != null && r.getOwner().equals(playerNameOrUUID)) {
				rs.add(r);
			}
		}
		return rs;
	}
	
	public static List<RadioStation> getRadioStationsByPlayer(OfflinePlayer player) {
		return getRadioStationsByPlayerNameOrUUID((Config.use_uuids?player.getUniqueId().toString():player.getName()));
	}
	
	public static void updateStationsGUI(){
		GUIs.STATIONS_GUI.refreshAllInstances();
	}
	
	public static void updateStationGUI(int rID){
		updateStationGUI(rID, null);
	}
	
	public static void updateStationGUI(int rID, Integer idx){
		if(idx != null && idx == -1) idx = null;
		for(Player p : GUIs.STATION_GUI.getAllOpenInstances()) {
			Inventory inv = p.getOpenInventory().getTopInventory();
			GUIHolder h = GUIUtils.getGUIHolder(inv);
			Object o = h.getProperty(Main.pl, "station_id");
			if(o != null && (int) o == rID) {
				if(idx != null) h.setProperty(Main.pl, "index", (int) idx);
				GUIs.STATION_GUI.refreshInstance(p);
			}
		}
	}
	
//	public static void updateStationGUI(int rID){
//		for(Player pl : Bukkit.getOnlinePlayers()){
//			String t = getInvType(pl);
//			if(t.equals("radio station")){
//				Inventory inv = pl.getOpenInventory().getTopInventory();
//				GUIHolder holder = (GUIHolder) inv.getHolder();
//				holder.getGui().refreshInstance(pl);
//			}
//		}
//	}
//	
//	public static void updateStationsGUI(){
//		for(Player pl : Bukkit.getOnlinePlayers()){
//			String t = getInvType(pl);
//			if(t.equals("radio stations")){
//				Inventory inv = pl.getOpenInventory().getTopInventory();
//				GUIHolder holder = (GUIHolder) inv.getHolder();
//				holder.getGui().refreshInstance(pl);
//			}
//		}
//	}
//	
//	public static void updateStationGUI(int rID, int index){
//		for(Player pl : Bukkit.getOnlinePlayers()){
//			String t = getInvType(pl);
//			if(t.equals("radio station")){
//				Inventory inv = pl.getOpenInventory().getTopInventory();
//				GUIHolder holder = (GUIHolder) inv.getHolder();
//				holder.getGui().refreshInstance(pl);
//			}
//		}
//	}
//	
//	public static String getInvType(Inventory inv){
//		if(GUIUtils.getGUI(inv) != null) {
//			GUIHolder holder = (GUIHolder) inv.getHolder();
//			String mode = (String) holder.getProperty("playerradios_type");
//			return mode != null ? mode : "none";
//		}
//		return "none";
//	}
//	
//	public static String getInvType(Player p){
//		if(p.getOpenInventory()!=null && p.getOpenInventory().getTopInventory()!=null){
//			return getInvType(p.getOpenInventory().getTopInventory());
//		}else{
//			return "none";
//		}
//	}

}
