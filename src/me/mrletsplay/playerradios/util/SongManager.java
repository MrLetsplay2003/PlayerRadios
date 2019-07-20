package me.mrletsplay.playerradios.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.CustomConfig;
import me.mrletsplay.mrcore.config.FileCustomConfig;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.song.Song;
import me.mrletsplay.playerradios.util.songloader.SongLoader;

public class SongManager {
	
	private static List<SongLoader> songLoaders = Arrays.asList(
				new me.mrletsplay.playerradios.util.songloader.impl.NBSSongLoader()
			);
	private static List<Song> songs;
	private static File sSets = new File(Main.pl.getDataFolder(),"/song-settings/");
	
	public static void init() {
		File nbs = new File(Main.pl.getDataFolder(),"/import/nbs/");
		nbs.mkdirs();
		File sng_r = new File(Main.pl.getDataFolder(),"/import/rsng/");
		sng_r.mkdirs();
		File sng = new File(Main.pl.getDataFolder(), "/songs/");
		sng.mkdirs();
		File sngI = new File(Main.pl.getDataFolder(),"/import/sng/");
		sngI.mkdirs();
		
		for(SongLoader l : songLoaders) {
			l.getSongImportFolder().mkdirs();
			l.getSongExportFolder().mkdirs();
		}
		
		sSets.mkdirs();
		
		songs = new ArrayList<>();
		Main.pl.getLogger().info("Loading song(s)...");
		long t = System.currentTimeMillis();
		int nbsF = 0, rsngF = 0, sngF = 0, sngFI = 0, sets = 0;
		for(File fl : nbs.listFiles()) {
			if(!fl.isDirectory()) {
				Song s = tryNBSImport(fl);
				if(s==null) {
					Main.pl.getLogger().info("Skipping import of \""+fl.getName()+"\"...");
					continue;
				}
				if(s!=null) {
					songs.add(s);
					nbsF++;
					fl.delete();
				}
			}
		}
		for(File fl : sng_r.listFiles()) {
			if(!fl.isDirectory()) {
				Song s = tryRSNGImport(fl);
				if(s==null) {
					Main.pl.getLogger().info("Skipping import of \""+fl.getName()+"\"...");
					continue;
				}
				if(s!=null) {
					songs.add(s);
					rsngF++;
					fl.delete();
				}
			}
		}
		for(File fl : sngI.listFiles()) {
			if(!fl.isDirectory()) {
				List<Song> ss2 = trySNGImport(fl);
				if(ss2==null) {
					Main.pl.getLogger().info("Skipping import of \""+fl.getName()+"\"...");
					continue;
				}
				for(Song s : ss2) {
					if(s!=null) {
						s.setID(-1);
						songs.add(s);
						sngFI++;
						fl.delete();
					}
				}
			}
		}
		for(File fl : sng.listFiles()) {
			if(!fl.isDirectory()) {
				List<Song> ss2 = trySNGImport(fl);
				if(ss2==null) {
					Main.pl.getLogger().info("Skipping import of \""+fl.getName()+"\"...");
					continue;
				}
				for(Song s : ss2) {
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
							try {
								SNGSongLoader.saveSongs(s);
								s.setSongFile(SNGSongLoader.getSongFile(s));
							} catch (Exception e) {
								e.printStackTrace();
							}
							sets++;
						}
						songs.add(s);
						sngF++;
						if(s.getID()==-1) {
							fl.delete();
						}
					}
				}
			}
		}
		Main.pl.getLogger().info("Loaded "+songs.size()+" song(s) with "+sets+" song-setting files ("+nbsF+" NBS, "+rsngF+" RSNG, "+sngF+" SNG, "+sngFI+" imported SNG) ("+Tools.timeTaken(t, System.currentTimeMillis(), true)+"s)");
	}
	
	public static List<Song> getSongs() {
		return songs;
	}
	
	private static boolean hasConfiguration(int songID) {
		return new File(sSets, songID+".yml").exists();
	}
	
	public static File getConfigFile(int songID) {
		return new File(sSets, songID+".yml");
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
					SNGSongLoader.saveSongs(s);
					s.setSongFile(SNGSongLoader.getSongFile(s));
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
	
	public static List<Song> trySNGImport(File f) {
		try {
			return SNGSongLoader.loadSongs(f);
		}catch(Exception e) {
			Main.pl.getLogger().info("Failed to load SNG song from file \""+f.getName()+"\"");
			e.printStackTrace();
			return null;
		}
	}
	
	public static Song tryRSNGImport(File f) {
		try {
			return RSNGSongLoader.loadSong(f);
		}catch(Exception e) {
			Main.pl.getLogger().info("Failed to load RSNG song from file \""+f.getName()+"\"");
			e.printStackTrace();
			return null;
		}
	}
	
	public static Song tryNBSImport(File f) {
		try {
			return NBSSongLoader.loadSong(f);
		}catch(Exception e) {
			Main.pl.getLogger().info("Failed to load NBS song from file \""+f.getName()+"\"");
			e.printStackTrace();
			return null;
		}
	}
	
	public static ImportResult tryAllImport(File f) {
		List<Song> ss = new ArrayList<>();
		try {
			Song s = NBSSongLoader.loadSong(f);
			if(s!=null) {
				ss.add(s);
				return new ImportResult("NBS", ss);
			}
		}catch(Exception e) {}
		try {
			ss = SNGSongLoader.loadSongs(f);
			return new ImportResult("SNG", ss);
		}catch(Exception e) {}
		try {
			Song s = RSNGSongLoader.loadSong(f);
			if(s!=null) {
				ss.add(s);
				return new ImportResult("RSNG", ss);
			}
		}catch(Exception e) {}
		return null;
	}
	
}
