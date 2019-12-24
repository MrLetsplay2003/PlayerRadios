package me.mrletsplay.playerradios;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.RadioStations;
import me.mrletsplay.playerradios.util.UpdateChecker;
import me.mrletsplay.playerradios.util.action.CancelTask;
import me.mrletsplay.playerradios.util.action.PlayerAction;
import me.mrletsplay.playerradios.util.action.PlayerActions;
import me.mrletsplay.playerradios.util.action.impl.PlayerRenameStationAction;
import net.md_5.bungee.api.ChatColor;

public class Events implements Listener {
	
//	public static HashMap<Player, Integer[]> typing = new HashMap<>();
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		if(e.getPlayer().hasPermission(Config.PERM_NOTIFY_UPDATE)){
			if(Config.enable_update_check && Config.update_check_on_join){
				UpdateChecker.Result res = UpdateChecker.checkForUpdate();
				if(res.updAvailable) {
					UpdateChecker.sendUpdateMessage(res, e.getPlayer());
				}
			}
		}
		if(Config.world_list_blacklist != Config.world_list.contains(e.getPlayer().getWorld().getName())) {
			int rID = PlayerManager.getLastListened(e.getPlayer());
			if(rID >= 0) {
				RadioStation r = StationManager.getRadioStation(rID);
				if(r!=null && r.isRunning()) {
					r.addPlayerListening(e.getPlayer());
				}
			}else if(Config.default_station>=0) {
				RadioStation r = StationManager.getRadioStation(Config.default_station);
				if(r!=null && r.isRunning()) {
					r.addPlayerListening(e.getPlayer());
				}
			}
		}
		PlayerManager.set(e.getPlayer());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		RadioStation r = RadioStations.getRadioStationListening(e.getPlayer());
		if(r!=null) {
			StationManager.stop(e.getPlayer());
			if(Config.save_last_listened) PlayerManager.setLastListened(e.getPlayer(), r.getID(), true);
		}else {
			if(Config.save_last_listened) PlayerManager.setLastListened(e.getPlayer(), -1, true);
		}
		CancelTask.cancelForPlayer(e.getPlayer());
		PlayerManager.set(e.getPlayer());
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if(PlayerActions.hasAction(e.getPlayer())) {
			PlayerAction a = PlayerActions.getAction(e.getPlayer());
			if(a instanceof PlayerRenameStationAction) {
				PlayerActions.removeAction(e.getPlayer());
				PlayerRenameStationAction action = (PlayerRenameStationAction) a;
				action.removeCancelTask();
				RadioStation r = StationManager.getRadioStation(action.getStationID());
				if(e.getMessage().length()<=Config.max_station_name_length) {
					String oName = r.getName();
					String nm = e.getMessage();
					if(Config.allow_color && (!Config.color_needs_permission || e.getPlayer().hasPermission(Config.PERM_ALLOW_COLOR))) {
						nm = ChatColor.translateAlternateColorCodes('&', nm);
					}
					r.setName(nm);
					e.getPlayer().sendMessage(Config.getMessage("station.set.name").replace("%old-name%", oName).replace("%new-name%", nm));
				}else {
					e.getPlayer().sendMessage(Config.getMessage("station.name-too-long"));
				}
				Bukkit.getScheduler().runTask(Main.pl, () -> e.getPlayer().openInventory(GUIs.getStationGUI(e.getPlayer(), action.getStationID(), 0)));
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		Player p = e.getPlayer();
		if(Config.world_list_blacklist == Config.world_list.contains(p.getWorld().getName())) {
			RadioStation r = RadioStations.getRadioStationListening(p);
			if(r!=null) {
				StationManager.stop(e.getPlayer());
				if(Config.save_last_listened) PlayerManager.setLastListened(e.getPlayer(), r.getID(), true);
			}else {
				if(Config.save_last_listened) PlayerManager.setLastListened(e.getPlayer(), -1, true);
			}
		}else if(Config.world_list_blacklist == Config.world_list.contains(e.getFrom().getName())){
			int rID = PlayerManager.getLastListened(e.getPlayer());
			if(rID >= 0) {
				RadioStation r = StationManager.getRadioStation(rID);
				if(r!=null && r.isRunning()) {
					r.addPlayerListening(e.getPlayer());
				}
			}
		}
	}
	
}
