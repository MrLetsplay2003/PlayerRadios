package me.mrletsplay.playerradios.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.GUIs;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.command.force.CommandPlayerRadiosForce;
import me.mrletsplay.playerradios.command.station.CommandPlayerRadiosStation;

public class CommandPlayerRadios extends BukkitCommand {

	public CommandPlayerRadios() {
		super("playerradios");
		addAlias("pr");
		setDescription("Opens the PlayerRadios GUI");
		setUsage("/pr");
		
		addSubCommand(new CommandPlayerRadiosVersion());
		addSubCommand(new CommandPlayerRadiosReload());
		addSubCommand(new CommandPlayerRadiosBugreport());
		addSubCommand(new CommandPlayerRadiosHelp());
		addSubCommand(new CommandPlayerRadiosSubmit());
		addSubCommand(new CommandPlayerRadiosSearch());
		addSubCommand(new CommandPlayerRadiosPlaylist());
		addSubCommand(new CommandPlayerRadiosExport());
		addSubCommand(new CommandPlayerRadiosStation());
		addSubCommand(new CommandPlayerRadiosForce());
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
		
		if(args.length != 0) {
			Main.sendCommandHelp(p, null);
			return;
		}
		
		p.openInventory(GUIs.getStationsGUI(p, null));
	}

}
