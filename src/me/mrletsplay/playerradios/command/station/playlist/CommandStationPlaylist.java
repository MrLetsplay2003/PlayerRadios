package me.mrletsplay.playerradios.command.station.playlist;

import java.util.Collections;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.event.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.song.Song;

public class CommandStationPlaylist extends BukkitCommand {
	
	public CommandStationPlaylist() {
		super("playlist");
		setDescription("Show or modify your station's playlist");
		setUsage("/playerradios station playlist <station>");

		addSubCommand(new CommandPlaylistAdd());
		addSubCommand(new CommandPlaylistRemove());
		
		setTabCompleter(event -> {
			CommandSender s = ((BukkitCommandSender) event.getSender()).getBukkitSender();
			if(!(s instanceof Player)) return Collections.emptyList();
			Player p = (Player) s;
			
			if(event.getArgs().length == 0) {
				return StationManager.getRadioStationsByPlayer(p).stream()
						.map(r -> String.valueOf(r.getID()))
						.collect(Collectors.toList());
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
		
		if(Config.disable_commands && Config.disable_commands_all) {
			p.sendMessage(Config.getMessage("commands-disabled"));
			return;
		}
		
		if(!Config.allow_create_stations && !p.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
			p.sendMessage(Config.getMessage("creation-disabled"));
		}
		
		if(args.length != 1) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		int rID;
		try {
			rID = Integer.parseInt(args[0]);
		}catch (NumberFormatException e) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		RadioStation r = StationManager.getRadioStation(rID);
		
		if(!r.isOwner(p) && !p.hasPermission(Config.PERM_EDIT_OTHER)) {
			p.sendMessage(Config.getMessage("station.not-your-station"));
			return;
		}
		
		p.sendMessage(Config.getMessage("station.playlist").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()).replace("%loop%", ""+r.isLooping()));
		if(r.getPlaylist().isEmpty()) {
			p.sendMessage(Config.getMessage("station.playlist-empty").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()));
			return;
		}
		
		int i = 0;
		for(Integer e : r.getPlaylist()) {
			Song s = SongManager.getSongByID(e);
			if(s!=null) {
				if(r.getCurrentIndex()>=0 && r.getCurrentIndex()==i && r.isRunning()) {
					p.sendMessage(Config.getMessage("station.playlist-entry-playing").replace("%index%", ""+i).replace("%song-id%", ""+s.getID()).replace("%song-name%", s.getName()));
				}else {
					p.sendMessage(Config.getMessage("station.playlist-entry").replace("%index%", ""+i).replace("%song-id%", ""+s.getID()).replace("%song-name%", s.getName()));
				}
			}
			i++;
		}
	}

}
