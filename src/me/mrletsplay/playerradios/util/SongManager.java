package me.mrletsplay.playerradios.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.CustomConfig;
import me.mrletsplay.mrcore.config.FileCustomConfig;
import me.mrletsplay.mrcore.misc.ErroringNullableOptional;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.song.Song;
import me.mrletsplay.playerradios.util.songloader.SongLoader;
import me.mrletsplay.playerradios.util.songloader.impl.NBSSongLoader;
import me.mrletsplay.playerradios.util.songloader.impl.OpenNBSSongLoader;
import me.mrletsplay.playerradios.util.songloader.impl.RSNGSongLoader;
import me.mrletsplay.playerradios.util.songloader.impl.SNGSongLoader;

public class SongManager {
	
	private static List<SongLoader> songLoaders = Arrays.asList(
				new NBSSongLoader(),
				new SNGSongLoader(),
				new RSNGSongLoader(),
				new OpenNBSSongLoader()
			);
	private static List<Song> songs = new ArrayList<>();
	private static File
		songsFolder = new File(Main.pl.getDataFolder(), "/songs/"),
		songSettingsFolder = new File(Main.pl.getDataFolder(),"/song-settings/");
	
	public static void init() {
		for(SongLoader l : songLoaders) {
			l.getSongImportFolder().mkdirs();
			l.getSongExportFolder().mkdirs();
		}
		
		songSettingsFolder.mkdirs();
		
		long t = System.currentTimeMillis();
		songs = new ArrayList<>();
		Main.pl.getLogger().info("Loading song(s)...");
		Map<String, Long> times = new LinkedHashMap<>();
		Map<String, Integer> counts = new LinkedHashMap<>();
		
		for(SongLoader l : songLoaders) {
			long tS = System.currentTimeMillis();
			int count = 0;
			for(File fl : l.getSongImportFolder().listFiles()) {
				if(!fl.isDirectory()) {
					ErroringNullableOptional<List<Song>, Exception> r = tryImport(l, fl);
					if(!r.isPresent()) {
						Main.pl.getLogger().info("Failed to load file \"" + fl.getName() + "\" using loader \"" + l.getName() + "\", skipping");
						r.getException().printStackTrace();
						continue;
					}
					fl.delete();
					List<Song> ss = r.get();
					ss.forEach(s -> s.setID(-1)); // Reassign ids
					songs.addAll(ss);
					count += ss.size();
				}
			}
			times.put(l.getName(), System.currentTimeMillis() - tS);
			counts.put(l.getName(), count);
		}
		
		int sngC = 0, sngSC = 0;
		SongLoader sngL = getSongLoader("sng");
		for(File fl : songsFolder.listFiles()) {
			if(!fl.isDirectory()) {
				ErroringNullableOptional<List<Song>, Exception> ss2 = tryImport(sngL, fl);
				if(!ss2.isPresent()) {
					Main.pl.getLogger().info("Skipping loading of \""+fl.getName()+"\"...");
					ss2.getException().printStackTrace();
					continue;
				}
				for(Song s : ss2.get()) {
					if(s!=null) {
						if(s.getID()!=-1 && hasConfiguration(s.getID())) {
							File cfgFile = getConfigFile(s.getID());
							CustomConfig cfg = ConfigLoader.loadFileConfig(cfgFile);
							String name = cfg.getString(s.getID()+".name");
							String originalAuthor = cfg.getString(s.getID()+".original-author");
							String author = cfg.getString(s.getID()+".author");
							s.setName(name);
							s.setOriginalAuthor(originalAuthor);
							s.setAuthor(author);
							fl.delete();
							cfgFile.delete();
							try {
								File sF = getSongFile(s);
								sngL.saveSongs(sF, s);
								s.setSongFile(sF);
							} catch (Exception e) {
								e.printStackTrace();
							}
							sngSC++;
						}
						songs.add(s);
						sngC++;
						if(s.getID()==-1) {
							fl.delete();
						}
					}
				}
			}
		}
		
		Main.pl.getLogger().info("Loaded " + songs.size() + " song(s) with " + sngSC + " song-setting files (loaded " + sngC + " sng, imported " +
				counts.entrySet().stream().map(en -> en.getValue() + " " + en.getKey()).collect(Collectors.joining(", "))
				+ ") in " + Tools.timeTaken(t, System.currentTimeMillis(), true) + "s");
	}
	
	public static List<Song> getSongs() {
		return songs;
	}
	
	public static List<SongLoader> getSongLoaders() {
		return songLoaders;
	}
	
	public static SongLoader getSongLoader(String name) {
		return songLoaders.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	private static boolean hasConfiguration(int songID) {
		return getConfigFile(songID).exists();
	}
	
	public static File getConfigFile(int songID) {
		return new File(songSettingsFolder, songID+".yml");
	}
	
	public static void setDefaultSongSettings(Song s) {
		File cfgFile = getConfigFile(s.getID());
		try {
			cfgFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		FileCustomConfig cfg = ConfigLoader.loadFileConfig(cfgFile);
		cfg.set(s.getID()+".name", s.getName());
		cfg.set(s.getID()+".original-author", s.getOriginalAuthor()!=null?s.getOriginalAuthor():Config.default_author);
		cfg.set(s.getID()+".author", s.getAuthor()!=null?s.getAuthor():Config.default_author);
		cfg.saveToFile();
	}
	
	public static int countSongs() {
		File nbs = new File(Main.pl.getDataFolder(),"/import/nbs/");
		nbs.mkdirs();
		File sng_r = new File(Main.pl.getDataFolder(),"/import/rsng/");
		sng_r.mkdirs();
		File sng = new File(Main.pl.getDataFolder(),"/songs/");
		sng.mkdirs();
		File sngI = new File(Main.pl.getDataFolder(),"/import/sng/");
		sngI.mkdirs();
		return nbs.listFiles().length + sng_r.listFiles().length + sngI.listFiles().length + sng.listFiles().length;
	}
	
	public static File getSongFile(Song s) {
		String sName = Tools.validateName(s.getName());
		File f = new File(songsFolder, sName + "." + s.getID() + ".sng");
		return f;
	}
	
	public static void registerNewSongs() {
		int ch = 0;
		Main.pl.getLogger().info("Converting song(s)...");
		long t = System.currentTimeMillis();
		int highestID = getHighestID();
		List<Integer> fIDs = getFreeIDs();
		for(Song s : songs) {
			if(s.getID()==-1 || (Config.fix_song_ids && getSongsByID(s.getID()).size()>1)) {
				int id;
				if(fIDs.isEmpty()) {
					id = ++highestID;
				}else {
					id = fIDs.remove(0);
				}
				if(s.getSongFile()!=null && s.getSongFile().exists()) {
					s.getSongFile().delete();
				}
				s.setID(id);
				try {
					SongLoader l = getSongLoader("sng");
					File sF = getSongFile(s);
					l.saveSongs(sF, s);
					s.setSongFile(sF);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(Config.songsettings_auto && !hasConfiguration(s.getID())) {
					setDefaultSongSettings(s);
				}
				ch++;
			}
		}
		Main.pl.getLogger().info("Converted "+ch+" song(s) ("+Tools.timeTaken(t, System.currentTimeMillis(), true)+"s)");
	}
	
	public static Song getSongByID(int id) {
		for(Song s : songs) {
			if(s.getID()==id) {
				return s;
			}
		}
		return null;
	}
	
	private static List<Song> getSongsByID(int id) {
		List<Song> ss = new ArrayList<>();
		for(Song s : songs) {
			if(s.getID()==id) {
				ss.add(s);
			}
		}
		return ss;
	}

	public static int newID() {
		int id = 0;
		while(getSongByID(id)!=null) {
			id++;
		}
		return id;
	}
	
	public static int getHighestID() {
		int id = 0;
		for(Song s : songs) {
			if(s.getID()>id) {
				id = s.getID();
			}
		}
		return id;
	}
	
	public static List<Integer> getFreeIDs(){
		List<Integer> ids = new ArrayList<>();
		for(int i = 0; i < getHighestID(); i++) {
			if(getSongByID(i)==null) {
				ids.add(i);
			}
		}
		return ids;
	}
	
	public static ErroringNullableOptional<List<Song>, Exception> tryImport(SongLoader loader, File f) {
		try {
			return ErroringNullableOptional.ofErroring(loader.loadSongs(f));
		}catch(Exception e) {
//			Main.pl.getLogger().info("Failed to load " + loader.getName() + " song from file \""+f.getName()+"\"");
//			e.printStackTrace();
			return ErroringNullableOptional.ofErroring(e);
		}
	}
	
	public static ImportResult tryAllImport(File f) {
		for(SongLoader l : songLoaders) {
			ErroringNullableOptional<List<Song>, Exception> r = tryImport(l, f);
			if(!r.isPresent()) continue;
			return new ImportResult(l.getName(), r.get());
		}
		return null;
	}
	
}
