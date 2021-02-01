package me.mrletsplay.playerradios.command.station;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.RadioStation;

public class CommandStationCreate extends BukkitCommand {
	
	public CommandStationCreate() {
		super("create");
		setDescription("Create a station");
		setUsage("/playerradios station create <name>");
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
		
		if(args.length == 0) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		String name = String.join(" ", args);
		if(name.length() > Config.max_station_name_length) {
			p.sendMessage(Config.getMessage("station.name-too-long"));
			return;
		}
		
		if(StationManager.getRadioStationsByPlayer(p).size() >= Config.max_stations_per_player) {
			p.sendMessage(Config.getMessage("station.too-many-stations"));
			return;
		}
		
		RadioStation r = StationManager.createStation(p, name);
		p.sendMessage(Config.getMessage("station-created.1", "id", ""+r.getID(), "station-name", r.getName()));
		p.sendMessage(Config.getMessage("station-created.2"));
	}

}
