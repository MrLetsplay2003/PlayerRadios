package me.mrletsplay.playerradios.util.songloader;

import java.io.File;
import java.io.IOException;

import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.song.Song;

public interface SongLoader {

	public String getName();
	
	public default File getSongImportFolder() {
		return new File(Main.pl.getDataFolder(), "/import/" + getName() + "/");
	}
	
	public default File getSongExportFolder() {
		return new File(Main.pl.getDataFolder(), "/export/" + getName() + "/");
	}
	
	public Song loadSong(File file) throws IOException;
	
	public void saveSong(Song song, File file) throws IOException;
	
}
