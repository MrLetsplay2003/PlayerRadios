package me.mrletsplay.playerradios;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.FileCustomConfig;
import me.mrletsplay.playerradios.util.Tools;

public class PlayerManager {
	
	public static File plFile = new File(Main.pl.getDataFolder(), "data.yml");
	public static FileCustomConfig pls = ConfigLoader.loadFileConfig(plFile);
	
	public static void save() {
		pls.saveToFile();
	}
	
	@SuppressWarnings("deprecation")
	public static void init() {
		for(String key : pls.getKeys()) {
			if(Config.use_uuids && !Tools.isUUID(key)) {
				String pl = Bukkit.getOfflinePlayer(key).getUniqueId().toString();
				pls.set(pl+".lastOnline", pls.getLong(key+".lastOnline"));
				pls.set(pl+".lastListened", pls.getLong(key+".lastListened"));
				pls.unset(key);
			}else if(!Config.use_uuids && Tools.isUUID(key)) {
				String pl = Bukkit.getOfflinePlayer(UUID.fromString(key)).getName();
				pls.set(pl+".lastOnline", pls.getLong(key+".lastOnline"));
				pls.set(pl+".lastListened", pls.getLong(key+".lastListened"));
				pls.unset(key);
			}
		}
		save();
	}
	
	public static long getLastOnlineTime(String pl) {
		return pls.getLong(pl+".lastOnline", System.currentTimeMillis(), false);
	}
	
	public static void set(Player p) {
		pls.set((Config.use_uuids?p.getUniqueId().toString():p.getName())+".lastOnline", System.currentTimeMillis());
		save();
	}
	
	public static void setLastListened(Player p, int id, boolean save) {
		if(id>=0) {
			pls.set((Config.use_uuids?p.getUniqueId().toString():p.getName())+".lastListened", id);
		}else {
			pls.unset((Config.use_uuids?p.getUniqueId().toString():p.getName())+".lastListened");
		}
		if(save) save();
	}
	
	public static int getLastListened(Player p) {
		return pls.getInt((Config.use_uuids?p.getUniqueId().toString():p.getName())+".lastListened", -1, false);
	}
	
	public static int setVolume(Player p, int volume) {
		int v = Math.max(Math.min(volume, 100), 0);
		pls.set((Config.use_uuids?p.getUniqueId().toString():p.getName())+".volume", v);
		save();
		return v;
	}
	
	public static int getVolume(Player p) {
		return pls.getInt((Config.use_uuids?p.getUniqueId().toString():p.getName())+".volume", 100, false);
	}
	
}
