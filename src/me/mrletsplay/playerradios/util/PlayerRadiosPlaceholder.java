package me.mrletsplay.playerradios.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.song.Song;

public class PlayerRadiosPlaceholder extends PlaceholderExpansion {

	@Override
	public String onPlaceholderRequest(Player p, String ident) {
		if(p==null) return "";
		RadioStation r = RadioStations.getRadioStationListening(p);
		if(ident.equals("player_listening_station")) {
			return r!=null?r.getName():Config.getMessage("placeholderapi.listening.station.none");
		}
		if(ident.equals("player_listening_station_owner")) {
			return r!=null?r.getOwnerName():Config.getMessage("placeholderapi.listening.station.owner.none");
		}
		if(ident.equals("player_listening_song")) {
			if(r==null) return Config.getMessage("placeholderapi.listening.song.none");
			Song s = r.getCurrentSong();
			return s!=null?Config.getMessage("placeholderapi.listening.song.format",
							"original-author", s.getOriginalAuthor()!=null?s.getOriginalAuthor():Config.default_author, 
							"author", s.getAuthor()!=null?s.getAuthor():Config.default_author,
							"song-name", s.getName(),
							"song-id", String.valueOf(s.getID()))
					: Config.getMessage("placeholderapi.listening.song.none");
			}
		if(ident.equals("player_listening_time")) {
			return r!=null?Tools.formatTime(r.getListenMs(p)):Config.getMessage("placeholderapi.listening.time.none");
		}
		return null;
	}

	public static void hook() {
		new PlayerRadiosPlaceholder().register();
	}

	@Override
	public @NotNull String getAuthor() {
		return "MrLetsplay2003";
	}

	@Override
	public @NotNull String getIdentifier() {
		return "playerradios";
	}

	@Override
	public @NotNull String getVersion() {
		return Main.pluginVersion;
	}

}
