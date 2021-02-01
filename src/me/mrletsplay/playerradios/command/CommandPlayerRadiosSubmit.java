package me.mrletsplay.playerradios.command;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.ImportResult;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.Tools;
import me.mrletsplay.playerradios.util.song.Song;

public class CommandPlayerRadiosSubmit extends BukkitCommand {
	
	public CommandPlayerRadiosSubmit() {
		super("submit");
		setDescription("Downloads a song file from a URL and adds it to the library");
		setUsage("/playerradios submit <URL>");
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
		
		if(args.length!=1) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		if((!Config.enable_submit && !sender.hasPermission(Config.PERM_SUBMIT_WHEN_DISABLED)) || (Config.submit_needs_perm && !sender.hasPermission(Config.PERM_SUBMIT))) {
			sender.sendMessage(Config.getMessage("no-permission"));
			return;
		}
		
		if(Main.tempProcessThread != null) {
			p.sendMessage(Config.getMessage("process-already-running"));
			return;
		}
		
		Main.tempProcessThread = new Thread(() -> {
			try {
				URL url = new URL(args[0]);
				if(Tools.getFileSize(url) / 1024 <= Config.submit_max_file_size) {
					File dlFolder = new File(Main.pl.getDataFolder(), "/import/download/");
					p.sendMessage(Config.getMessage("submit.downloading"));
					File f = Tools.downloadWithFileName(url, dlFolder);
					p.sendMessage(Config.getMessage("submit.reading"));
					ImportResult ss = SongManager.tryAllImport(f);
					f.delete();
					dlFolder.delete();
					
					if(ss!=null) {
						for(Song s : ss.songs) {
							s.setID(-1);
							SongManager.getSongs().add(s);
						}
						SongManager.registerNewSongs();
						p.sendMessage(Config.getMessage("submit.success").replace("%count%", ss.songs.size()+"").replace("%format%", ss.format));
					}else {
						p.sendMessage(Config.getMessage("submit.invalid-file"));
					}
				}else {
					p.sendMessage(Config.getMessage("submit.file-too-big"));
				}
			}catch(MalformedURLException e) {
				p.sendMessage(Config.getMessage("submit.invalid-url"));
			} catch (IOException e) {
				sendCommandInfo(event.getSender());
			}
			Main.tempProcessThread = null;
		},  "PlayerRadios-Process-Thread");
		Main.tempProcessThread.start();
	}

}
