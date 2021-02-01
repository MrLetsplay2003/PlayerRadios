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

public class CommandSetName extends BukkitCommand {
	
	public CommandSetName() {
		super("name");
		setDescription("Change the name of your station");
		setUsage("/playerradios station set name <station> <name>");
		
		setTabCompleter(event -> {
			if(event.getArgs().length != 0) return Collections.emptyList();
			
			CommandSender s = ((BukkitCommandSender) event.getSender()).getBukkitSender();
			if(!(s instanceof Player)) return Collections.emptyList();
			Player p = (Player) s;
			
			return StationManager.getRadioStationsByPlayer(p).stream()
					.map(r -> String.valueOf(r.getID()))
					.collect(Collectors.toList());
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
		
		if(args.length < 2) {
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
		
		String name = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
		if(name.length() > Config.max_station_name_length) {
			p.sendMessage(Config.getMessage("station.name-too-long"));
			return;
		}
		
		String oName = r.getName();
		r.setName(name);
		p.sendMessage(Config.getMessage("station.set.name", "old-name", oName, "new-name", name));
	}

}
