package me.mrletsplay.playerradios.command;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.song.Song;
import me.mrletsplay.playerradios.util.songloader.SongLoader;

public class CommandPlayerRadiosExport extends BukkitCommand {
	
	public CommandPlayerRadiosExport() {
		super("export");
		setDescription("Export a song/multiple songs to files");
		setUsage("/playerradios export <song id/all> <nbs/opennbs/sng/sng-archive/rsng/settings>");
		
		setTabCompleter(event -> {
			if(event.getArgs().length == 0) {
				return Arrays.asList("all");
			}else if(event.getArgs().length == 1) {
				return Arrays.asList("nbs", "opennbs", "sng", "sng-archive", "rsng", "settings");
			}
			
			return Collections.emptyList();
		});
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
		
		if(Config.world_list_blacklist == Config.world_list.contains(p.getWorld().getName())) {
			p.sendMessage(Config.getMessage("world-blacklisted"));
			return;
		}
		
		if(!p.hasPermission(Config.PERM_EXPORT)) {
			sender.sendMessage(Config.getMessage("no-permission"));
			return;
		}
		
		if(args.length != 2) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		try {
			String eMode = args[1];
			SongLoader l = SongManager.getSongLoader(eMode);
			SongLoader aL = eMode.toLowerCase().endsWith("-archive") ? SongManager.getSongLoader(eMode.substring(0, eMode.length() - "-archive".length())) : null;
			
			if(l == null && (aL == null || !aL.supportsSongArchives()) && !eMode.equalsIgnoreCase("settings")) {
				sendCommandInfo(event.getSender());
				return;
			}
			
			if(args[0].equalsIgnoreCase("all")) {
				if(!p.hasPermission(Config.PERM_EXPORT_ALL)) {
					sender.sendMessage(Config.getMessage("no-permission"));
					return;
				}
				
				if(!Main.exportRunning) {
					p.sendMessage(Config.getMessage("export.wait-all").replace("%count%", ""+SongManager.getSongs().size()));
					Main.exportRunning = true;
					if(SongManager.getSongs().size()>=Config.thread_on_process_when) {
						Main.tempProcessThread = new Thread(new Runnable() {
							
							@Override
							public void run() {
								Main.playerExportAll(p, eMode, true);
								Main.tempProcessThread = null;
							}
						}, "PlayerRadios-Process-Thread");
						Main.tempProcessThread.start();
					}else {
						Main.playerExportAll(p, eMode, false);
					}
				}else {
					p.sendMessage(Config.getMessage("process-already-running"));
				}
			}else {
				if(eMode.equalsIgnoreCase("sng-archive")){
					p.sendMessage(Config.getMessage("export.not-available"));
					return;
				}
				Song s = SongManager.getSongByID(Integer.parseInt(args[0]));
				if(s == null) {
					p.sendMessage(Config.getMessage("export.invalid-song"));
					return;
				}
				try {
					p.sendMessage(Config.getMessage("export.wait", "song-name", s.getName()));
					File f = null;
					if(eMode.equalsIgnoreCase("settings")) {
						f = SongManager.getConfigFile(s.getID());
						SongManager.setDefaultSongSettings(s);
					}else {
						f = l.getSongExportFile(s);
						l.saveSongs(f, s);
					}
					p.sendMessage(Config.getMessage("export.done").replace("%file-name%", f.getName()));
				} catch (Exception e) {
					e.printStackTrace();
					p.sendMessage(Config.getMessage("export.failed"));
				}
			}
		}catch(NumberFormatException e) {
			sendCommandInfo(event.getSender());
		}
	}

}
