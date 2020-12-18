package me.mrletsplay.playerradios.command.station;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;

public class CommandPlayerRadiosStation extends BukkitCommand {
	
	public CommandPlayerRadiosStation() {
		super("station");
		setDescription("Manage your PlayerRadios station");
		
		addSubCommand(new CommandStationCreate());
		addSubCommand(new CommandStationList());
		addSubCommand(new CommandStationPlaylist());
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		sendCommandInfo(event.getSender());
	}

}
