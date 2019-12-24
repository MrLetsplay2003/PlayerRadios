package me.mrletsplay.playerradios.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.mrletsplay.playerradios.Main;
import net.md_5.bungee.api.ChatColor;

public class UpdateChecker {
	
	public static void sendUpdateMessage(Result r, Player... pls) {
		for(Player p : pls){
			p.sendMessage("§aThere's an update available for PlayerRadios");
			p.sendMessage("§c"+Main.pluginVersion+" §r-> §b"+r.updVer+":");
			for(String ln : r.updChlog){
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', ln));
			}
		}
	}

	public static Result checkForUpdate(){
		try {
			URL updUrl = new URL("https://graphite-official.com/api/plugin-data/PlayerRadios/version.txt");
			BufferedReader r = new BufferedReader(new InputStreamReader(updUrl.openStream()));
			String ver = r.readLine();
			boolean uA = !ver.equalsIgnoreCase(Main.pluginVersion);
			List<String> chL = new ArrayList<>();
			if(uA){
				String ln;
				while((ln = r.readLine()) != null){
					chL.add(ln);
				}
			}
			return new Result(uA, ver, chL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static class Result{
		public final boolean updAvailable;
		public final String updVer;
		public final List<String> updChlog;
		
		public Result(boolean updAvailable, String updVer, List<String> chL) {
			this.updAvailable = updAvailable;
			this.updVer = updVer;
			this.updChlog = chL;
		}
	}
	
}
