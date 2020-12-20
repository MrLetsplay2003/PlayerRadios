package me.mrletsplay.playerradios.command;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommand;
import me.mrletsplay.mrcore.bukkitimpl.command.BukkitCommandSender;
import me.mrletsplay.mrcore.command.CommandInvokedEvent;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.song.Song;

public class CommandPlayerRadiosSearch extends BukkitCommand {
	
	public CommandPlayerRadiosSearch() {
		super("search");
		setDescription("Searches through the library of songs");
		setUsage("/playerradios search <query>");
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
		
		if(!Config.allow_create_stations && !p.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
			p.sendMessage(Config.getMessage("creation-disabled"));
			return;
		}
		
		if(args.length == 0) {
			sendCommandInfo(event.getSender());
			return;
		}
		
		String name = String.join(" ", args);
		final String term = name.toLowerCase();
		List<Song> ss = SongManager.getSongs().stream()
				.filter(s -> s.getAuthor().toLowerCase().contains(term)
						|| (s.getName() != null && s.getName().toLowerCase().contains(term))
						|| (s.getOriginalAuthor() != null && s.getOriginalAuthor().toLowerCase().contains(term)))
				.collect(Collectors.toList());
		p.sendMessage(Config.getMessage("search-results"));
		if(ss.isEmpty()) {
			p.sendMessage(Config.getMessage("search-results-empty"));
			return;
		}
		
		for(int i = 0; i < (ss.size() < Config.result_amount?ss.size():Config.result_amount); i++) {
			Song s = ss.get(i);
			p.sendMessage(Config.getMessage("search-results-entry").replace("%song-id%", ""+s.getID()).replace("%song-author%", (s.getOriginalAuthor()!=null?s.getOriginalAuthor():Config.default_author)).replace("%song-name%", s.getName()).replace("%song-by%", (s.getAuthor()!=null?s.getAuthor():Config.default_author)));
		}
	}

}
