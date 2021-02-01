package me.mrletsplay.playerradios.command.force;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;

public class CommandPlayerRadiosForce extends BukkitCommand {
	
	public CommandPlayerRadiosForce() {
		super("force");
		
		addSubCommand(new CommandForceStation());
		addSubCommand(new CommandForceVolume());
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		sendCommandInfo(event.getSender());
	}

}
