package me.mrletsplay.playerradios.util.action.impl;

import org.bukkit.entity.Player;

import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.util.action.PlayerAction;
import me.mrletsplay.playerradios.util.action.CancelTask;

public class PlayerRenameStationAction extends PlayerAction {

	private int stationID;
	
	public PlayerRenameStationAction(Player p, int stationID) {
		if(Config.max_type_time > 0) {
			cancelTask = new CancelTask(p);
			cancelTask.schedule(Config.max_type_time * 20);
		}
	}
	
	public int getStationID() {
		return stationID;
	}
	
}
