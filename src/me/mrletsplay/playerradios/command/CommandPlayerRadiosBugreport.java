package me.mrletsplay.playerradios.command;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.PasteText;

public class CommandPlayerRadiosBugreport extends BukkitCommand {
	
	public CommandPlayerRadiosBugreport() {
		super("bugreport");
		setDescription("Shows the current version of PlayerRadios and checks for an update");
		setUsage("/playerradios bugreport");
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		if(!(sender instanceof Player)) {
			sender.sendMessage("§cOnly players can use this command");
			return;
		}
		
		Player p = (Player) sender;
		
		if(!p.hasPermission(Config.PERM_ALLOW_BUGREPORT)) {
			sender.sendMessage(Config.getMessage("no-permission"));
			return;
		}
		
		try {
			List<String> config = Files.readAllLines(Config.config.getConfigFile().toPath());
			List<String> stations = Files.readAllLines(StationManager.stationFile.toPath());
			p.sendMessage(Config.getMessage("bugreport.success", "link", PasteText.glotSafe(
					"config.yml",
					config.stream().collect(Collectors.joining("\n")),
					"stations.yml",
					stations.stream().collect(Collectors.joining("\n")))));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
