package me.mrletsplay.playerradios.command.force;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.PlayerManager;

public class CommandForceVolume extends BukkitCommand {
	
	public CommandForceVolume() {
		super("volume");
		setDescription("Changes a player's volume");
		setUsage("/pr force volume <volume>");
		
		setTabCompleter((sender, command, label, args) -> {
			if(args.length == 0) {
				return Bukkit.getOnlinePlayers().stream()
						.map(pl -> pl.getName())
						.collect(Collectors.toList());
			}else if (args.length == 1) {
				return Arrays.asList("100", "0");
			}
			
			return Collections.emptyList();
		});
	}

	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		String[] args = event.getArguments();
		
		if(!sender.hasPermission(Config.PERM_FORCE)) {
			Main.sendCommandHelp(sender, null);
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
		
		int volume;
		try {
			volume = Integer.parseInt(args[1]);
		}catch(NumberFormatException e) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		if(volume < 0 || volume > 100) {
			sender.sendMessage(Config.getMessage("force.volume.invalid-volume"));
			return;
		}
		
		PlayerManager.setVolume(pl, volume);
		sender.sendMessage(Config.getMessage("force.volume.changed", "player", pl.getName(), "volume", ""+volume));
	}

}
