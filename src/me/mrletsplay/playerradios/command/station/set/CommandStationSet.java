package me.mrletsplay.playerradios.command.station.set;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;

public class CommandStationSet extends BukkitCommand {
	
	public CommandStationSet() {
		super("set");
		setDescription("Change your station's settings");
		
		addSubCommand(new CommandSetName());
		addSubCommand(new CommandSetLoop());
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		sendCommandInfo(event.getSender());
	}

}
