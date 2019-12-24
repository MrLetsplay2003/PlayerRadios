package me.mrletsplay.playerradios.util.action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class PlayerActions {
	
	private static Map<UUID, PlayerAction> actions = new HashMap<>();
	
	public static void setAction(Player player, PlayerAction action) {
		actions.put(player.getUniqueId(), action);
	}
	
	public static void removeAction(Player player) {
		actions.remove(player.getUniqueId());
	}
	
	public static boolean hasAction(Player player) {
		return actions.containsKey(player.getUniqueId());
	}
	
	public static PlayerAction getAction(Player player) {
		return actions.get(player.getUniqueId());
	}
	
	public static void clearActions() {
		actions.clear();
	}

}
