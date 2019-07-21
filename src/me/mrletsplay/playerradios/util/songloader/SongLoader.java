package me.mrletsplay.playerradios.util.songloader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.Tools;
import me.mrletsplay.playerradios.util.song.Song;

public interface SongLoader {

	public String getName();

	public default String getFileExtension() {
		return getName();
	}
	
	public default File getSongImportFolder() {
		return new File(Main.pl.getDataFolder(), "/import/" + getName() + "/");
	}
	
	public default File getSongExportFolder() {
		return new File(Main.pl.getDataFolder(), "/export/" + getName() + "/");
	}
	
	public default File getSongExportFile(Song song) {
		String fN = Tools.validateName(song.getName());
		return new File(getSongExportFolder(), fN + "." + song.getID() + "." + getFileExtension());
	}
	
	public List<Song> loadSongs(File file) throws IOException;
	
	public void saveSongs(File file, Song... songs) throws IOException;
	
	public boolean supportsSongArchives();
	
}
