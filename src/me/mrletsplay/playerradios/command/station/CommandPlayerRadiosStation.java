package me.mrletsplay.playerradios.command.station;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.command.station.playlist.CommandStationPlaylist;
import me.mrletsplay.playerradios.command.station.set.CommandStationSet;

public class CommandPlayerRadiosStation extends BukkitCommand {
	
	public CommandPlayerRadiosStation() {
		super("station");
		setDescription("Manage your PlayerRadios station");
		
		addSubCommand(new CommandStationCreate());
		addSubCommand(new CommandStationList());
		addSubCommand(new CommandStationPlaylist());
		addSubCommand(new CommandStationSet());
		addSubCommand(new CommandStationSkip());
		addSubCommand(new CommandStationStart());
		addSubCommand(new CommandStationStop());
		addSubCommand(new CommandStationDelete());
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		sendCommandInfo(event.getSender());
	}

}
