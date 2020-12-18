package me.mrletsplay.playerradios.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.Main;

public class CommandPlayerRadiosHelp extends BukkitCommand {
	
	public CommandPlayerRadiosHelp() {
		super("help");
		setDescription("Shows help (about a specific topic)");
		setUsage("/pr help <topic>");
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
		
		if(args.length == 0) {
			Main.sendCommandHelp(p, null);
		}else if(args.length == 1) {
			Main.sendCommandHelp(p, args[0]);
		}
	}

}
