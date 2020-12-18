package me.mrletsplay.playerradios.command.station;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.RadioStation;

public class CommandStationList extends BukkitCommand {
	
	public CommandStationList() {
		super("list");
		setDescription("Lists your stations");
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		if(!(sender instanceof Player)) {
			sender.sendMessage("§cOnly players can use this command");
			return;
		}

		Player p = (Player) sender;
		
		if(Config.world_list_blacklist == Config.world_list.contains(p.getWorld().getName())) {
			p.sendMessage(Config.getMessage("world-blacklisted"));
			return;
		}
		
		if(Config.disable_commands && Config.disable_commands_all) {
			p.sendMessage(Config.getMessage("commands-disabled"));
			return;
		}
		
		if(!Config.allow_create_stations && !p.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
			p.sendMessage(Config.getMessage("creation-disabled"));
		}
		
		p.sendMessage(Config.getMessage("stations"));
		List<RadioStation> ss = StationManager.getRadioStationsByPlayer(p);
		if(ss.isEmpty()) {
			p.sendMessage(Config.getMessage("stations-empty"));
			return;
		}
		
		for(RadioStation r : ss) {
			p.sendMessage(Config.getMessage("stations-entry", "station-id", ""+r.getID(), "station-name", r.getName()));
		}
	}

}
