package me.mrletsplay.playerradios.command.station;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.StationManager;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.song.Song;

public class CommandStationPlaylist extends BukkitCommand {
	
	public CommandStationPlaylist() {
		super("playlist");
		setDescription("Show or modify your station's playlist");

		addSubCommand(new CommandStationPlaylistAdd());
		addSubCommand(new CommandStationPlaylistRemove());
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
