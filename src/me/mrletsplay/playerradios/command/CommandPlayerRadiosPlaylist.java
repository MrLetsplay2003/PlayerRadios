package me.mrletsplay.playerradios.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.RadioStations;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.song.Song;

public class CommandPlayerRadiosPlaylist extends BukkitCommand {
	
	public CommandPlayerRadiosPlaylist() {
		super("playlist");
		setDescription("Shows the playlist of the station you're currently listening to");
		setUsage("/playerradios playlist");
	}
	
	@Override
	public void action(CommandInvokedEvent event) {
		CommandSender sender = ((BukkitCommandSender) event.getSender()).getBukkitSender();
		if(!(sender instanceof Player)) {
			sender.sendMessage("§cOnly players can use this command");
			return;
		}
		
		Player p = (Player) sender;
		
		if(Config.world_list_blacklist == Config.world_list.contains(p.getWorld().getName())) {
			p.sendMessage(Config.getMessage("world-blacklisted"));
			return;
		}
		
		RadioStation r = RadioStations.getRadioStationListening(p);
		if(r == null) {
			p.sendMessage(Config.getMessage("not-listening"));
			return;
		}
		
		p.sendMessage(Config.getMessage("station.playlist").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()).replace("%loop%", ""+r.isLooping()));
		if(r.getPlaylist().isEmpty()) {
			p.sendMessage(Config.getMessage("station.playlist-empty").replace("%station-id%", ""+r.getID()).replace("%station-name%", ""+r.getName()));
		}
		
		int i = 0;
		for(Integer e : r.getPlaylist()) {
			Song s = SongManager.getSongByID(e);
			if(s != null) {
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
