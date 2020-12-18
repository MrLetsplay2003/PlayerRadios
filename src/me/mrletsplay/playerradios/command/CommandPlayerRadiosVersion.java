package me.mrletsplay.playerradios.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.mrcore.command.option.CommandOption;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.UpdateChecker;
import me.mrletsplay.playerradios.util.UpdateChecker.Result;

public class CommandPlayerRadiosVersion extends BukkitCommand {
	
	private static final CommandOption<?> NO_CHECK = createCommandOption("n", "no-check");
	
	public CommandPlayerRadiosVersion() {
		super("version");
		setDescription("Shows the current version of PlayerRadios and checks for an update");
		addOption(NO_CHECK);
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		if(!(sender instanceof Player)) {
			sender.sendMessage("§cOnly players can use this command");
			return;
		}
		
		Player p = (Player) sender;
		
		if (!p.hasPermission(Config.PERM_NOTIFY_UPDATE)) {
			Main.sendCommandHelp(p, null);
			return;
		}
		
		p.sendMessage("Current PlayerRadios version: §7" + Main.pluginVersion);
		if(Config.enable_update_check && Config.update_check_on_command && !event.isOptionPresent(NO_CHECK)) {
			Result r = UpdateChecker.checkForUpdate();
			if(r.updAvailable) {
				UpdateChecker.sendUpdateMessage(r, p);
			}else {
				p.sendMessage("§aYou are using the newest version of PlayerRadios");
			}
		}
	}

}
