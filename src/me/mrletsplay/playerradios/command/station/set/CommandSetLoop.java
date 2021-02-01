package me.mrletsplay.playerradios.command.station.set;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.Tools;

public class CommandSetLoop extends BukkitCommand {
	
	public CommandSetLoop() {
		super("loop");
		setDescription("Enable/Disable looping");
		setUsage("/playerradios station set loop <station> <true/false>");
		
		setTabCompleter(event -> {
			CommandSender s = ((BukkitCommandSender) event.getSender()).getBukkitSender();
			if(!(s instanceof Player)) return Collections.emptyList();
			Player p = (Player) s;
			
			if(event.getArgs().length == 0) {
				return StationManager.getRadioStationsByPlayer(p).stream()
						.map(r -> String.valueOf(r.getID()))
						.collect(Collectors.toList());
			}else if (event.getArgs().length == 1) {
				return Arrays.asList("true", "false");
			}
			
			return Collections.emptyList();
		});
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		if(!(sender instanceof Player)) {
			sender.sendMessage("§cOnly players can use this command");
			return;
		}

		Player p = (Player) sender;
		String[] args = event.getArguments();
		
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
			return;
		}
		
		if(args.length != 2) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		int rID;
		try {
			rID = Integer.parseInt(args[0]);
		}catch (NumberFormatException e) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		RadioStation r = StationManager.getRadioStation(rID);
		
		if(!r.isOwner(p) && !p.hasPermission(Config.PERM_EDIT_OTHER)) {
			p.sendMessage(Config.getMessage("station.not-your-station"));
			return;
		}
		
		boolean bool = Tools.stringToBoolean(args[1]);
		r.setLoop(bool);
		p.sendMessage(Config.getMessage(bool ? "station.set.loop.enable" : "station.set.loop.disable"));
	}

}
