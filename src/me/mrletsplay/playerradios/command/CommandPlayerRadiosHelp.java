package me.mrletsplay.playerradios.command;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;

public class CommandPlayerRadiosHelp extends BukkitCommand {
	
	public CommandPlayerRadiosHelp() {
		super("help");
		setDescription("Shows help (about a specific topic)");
		setUsage("/playerradios help <topic>");
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		CommandPlayerRadios.INSTANCE.sendCommandInfo(event.getSender());
	}

}
