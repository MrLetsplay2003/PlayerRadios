package me.mrletsplay.playerradios.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.mrletsplay.playerradios.Main;

public class RadioStations {

	public static List<RadioStation> stations = new ArrayList<>();
	
	public static void tick() {
		for(RadioStation s : stations) {
			if(Main.enabled && s.isRunning()){
				s.tick();
			}
		}
	}
	
	public static RadioStation getRadioStationListening(Player p) {
		for(RadioStation s : stations) {
			if(s.getPlayers().contains(p)) {
				return s;
			}
		}
		return null;
	}
	
}
