package me.mrletsplay.playerradios.command.force;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.RadioStation;

public class CommandForceStation extends BukkitCommand {
	
	public CommandForceStation() {
		super("station");
		setDescription("Forces a player to listen to a specific station");
		setUsage("/playerradios force station <station/none>");
		
		setTabCompleter(event -> {
			if(event.getArgs().length == 0) {
				return Bukkit.getOnlinePlayers().stream()
						.map(pl -> pl.getName())
						.collect(Collectors.toList());
			}else if (event.getArgs().length == 1) {
				List<String> ids = StationManager.getRadioIDs().stream()
						.map(String::valueOf)
						.collect(Collectors.toCollection(ArrayList::new));
				ids.add("none");
				return ids;
			}
			
			return Collections.emptyList();
		});
	}

	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		String[] args = event.getArguments();
		
		if(!sender.hasPermission(Config.PERM_FORCE)) {
			sender.sendMessage(Config.getMessage("no-permission"));
			return;
		}
		
		if(args.length != 2) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		Player pl = Bukkit.getPlayer(args[0]);
		if(pl == null) {
			sender.sendMessage(Config.getMessage("force.unknown-player"));
			return;
		}
		
		
		if(!"none".equalsIgnoreCase(args[1])) {
			RadioStation r;
			try {
				r = StationManager.getRadioStation(Integer.parseInt(args[1]));
			}catch(NumberFormatException e) {
				sendCommandInfo(event.getSender());
				return;
			}
			
			if(r == null) {
				sender.sendMessage(Config.getMessage("force.station.invalid-station"));
				return;
			}
			
			if(!r.isRunning()) {
				sender.sendMessage(Config.getMessage("force.station.not-running"));
				return;
			}
			
			StationManager.stop(pl);
			r.addPlayerListening(pl);
			sender.sendMessage(Config.getMessage("force.station.changed", "player", pl.getName(), "station", r.getName()));
		}else {
			StationManager.stop(pl);
			sender.sendMessage(Config.getMessage("force.station.stopped", "player", pl.getName()));
		}
		
	}

}
