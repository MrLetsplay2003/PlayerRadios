package me.mrletsplay.playerradios.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;

public class CommandPlayerRadiosReload extends BukkitCommand {
	
	public CommandPlayerRadiosReload() {
		super("reload");
		addAlias("rl");
		setDescription("Reloads PlayerRadios");
		setUsage("/pr reload");
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		
		if(!sender.hasPermission(Config.PERM_RELOAD)) {
			Main.sendCommandHelp(sender, null);
			return;
		}
		
		Config.config.reload(false);
		Bukkit.getPluginManager().disablePlugin(Main.pl);
		Bukkit.getPluginManager().enablePlugin(Main.pl);
		sender.sendMessage(Config.getMessage("reload-complete"));
	}

}
