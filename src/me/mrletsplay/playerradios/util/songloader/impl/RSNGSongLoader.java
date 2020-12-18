package me.mrletsplay.playerradios.util.songloader.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;
import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.playerradios.util.Tools;
import me.mrletsplay.playerradios.util.song.Layer;
import me.mrletsplay.playerradios.util.song.Note;
import me.mrletsplay.playerradios.util.song.Song;
import me.mrletsplay.playerradios.util.songloader.SongLoader;
import me.mrletsplay.playerradios.util.songloader.SongLoadingException;

public class RSNGSongLoader implements SongLoader {

	@Override
	public String getName() {
		return "rsng";
	}

	@Override
	public List<Song> loadSongs(File file) throws IOException {
		try (BufferedReader r = new BufferedReader(new FileReader(file))) {
			int id = -1;
			int ln = 0;
			String name = null;
			float tps = -1;
			String author = "";
			String oAuthor = "";
			boolean useCustomSounds = false;
			try {
				String sInfo;
				while ((sInfo = r.readLine()) != null) {
					ln++;
					if (!(sInfo.startsWith("//") || sInfo.equals(""))) {
						break;
					}
				}
				String[] spl = sInfo.split(";");
				for (String s : spl) {
					String[] spl2 = s.split("=");
					if (spl2.length == 2) {
						String type = spl2[0].trim();
						String val = spl2[1].trim();
						if (type.equalsIgnoreCase("id")) {
							id = Integer.parseInt(val);
						} else if (type.equalsIgnoreCase("name")) {
							name = val;
						} else if (type.equalsIgnoreCase("tps")) {
							tps = Float.parseFloat(val);
						} else if (type.equalsIgnoreCase("author")) {
							author = val;
						} else if (type.equalsIgnoreCase("original-author")) {
							oAuthor = val;
						} else if (type.equalsIgnoreCase("use-custom-sounds")) {
							useCustomSounds = Tools.stringToBoolean(val);
						}
					} else {
						throw new SongLoadingException("Failed to load song from " + file.getName() + ". Error at line: " + ln);
					}
				}
			} catch (Exception e) {
				throw new SongLoadingException("Failed to load song header of " + file.getName());
			}
			if (name == null || tps == -1) {
				if (name == null)
					throw new SongLoadingException("Failed to load song \"" + file.getName() + "\"! (Invalid song header): Name not set");
				if (tps == -1)
					throw new SongLoadingException("Failed to load song \"" + file.getName() + "\"! (Invalid song header): TPS not set");
			}
			HashMap<String, String> customSounds = new HashMap<>();
			if (useCustomSounds) {
				try {
					String cSounds;
					while ((cSounds = r.readLine()) != null) {
						ln++;
						if (!(cSounds.startsWith("//") || cSounds.equals(""))) {
							break;
						}
					}
					String[] spl = cSounds.split(";");
					for (String s : spl) {
						String[] spl2 = s.split("=");
						if (spl2.length == 2) {
							String type = spl2[0].trim();
							String val = spl2[1].trim();
							customSounds.put(type, val);
						} else {
							throw new SongLoadingException("Failed to load song from " + file.getName() + ". Error at line: " + ln);
						}
					}
				} catch (Exception e) {
					throw new SongLoadingException("Failed to load custom instruments of " + file.getName());
				}
			}
			String line;
			int currTick = 0;
			int hLayer = 0;
			HashMap<Integer, Layer> layers = new HashMap<>();
			while ((line = r.readLine()) != null) {
				int currLayer = 0;
				if (line.equals("") || line.startsWith("//")) {
					continue;
				}
				ln++;
				try {
					String[] nSpl = line.split(";");
					for (String n : nSpl) {
						String[] spl2 = n.split(",");
						String type = spl2[0].trim();
						if (type.equalsIgnoreCase("pause") || type.equalsIgnoreCase("p")) {
							if (spl2.length != 2) 
								throw new SongLoadingException("Invalid pause at line " + ln);
							currTick += Integer.parseInt(spl2[1].trim());
						} else if (type.equalsIgnoreCase("note") || type.equalsIgnoreCase("n")) {
							if (spl2.length < 3 || spl2.length > 4) 
								throw new SongLoadingException("Invalid note at line " + ln);
							VersionedSound s;
							String cSound = null;
							try {
								s = Tools.getSound(Byte.parseByte(spl2[1].trim()));
							} catch (NumberFormatException e) {
								s = Tools.getSound(
										Tools.getSoundID(VersionedSound.valueOf(spl2[1].trim().toUpperCase())));
							}
							if (s == null) {
								cSound = customSounds.get(spl2[1]);
							}
							if (s == null && cSound == null) {
								s = Tools.getSound(0);
							}
							int note = Integer.parseInt(spl2[2].trim());
							int volume = 10;
							if (spl2.length == 4) {
								volume = Integer.parseInt(spl2[3].trim());
							}
							Layer l = layers.getOrDefault(currLayer, new Layer());
							l.setNote(currTick, s != null ? new Note(s, note, volume) : new Note(cSound, note, volume));
							layers.put(currLayer, l);
						}
						currLayer++;
					}
					if (currLayer > hLayer) {
						hLayer = currLayer;
					}
					currTick++;
				} catch (Exception e) {
					throw new SongLoadingException("Failed to load song. Caught exception at line " + ln, e);
				}
			}
			return Collections.singletonList(new Song(id, currTick, hLayer, name, layers, tps,
					(author.equals("") ? null : author), (oAuthor.equals("") ? null : oAuthor), customSounds, null));
		}
	}

	@Override
	public void saveSongs(File file, Song... songs) throws IOException {
		if (songs.length != 1)
			throw new UnsupportedOperationException("Can't save multiple songs to one file");
		Song song = songs[0];
		IOUtils.createFile(file);
		try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
			w.write("//Automatically generated song file");
			w.newLine();
			w.write("name=" + song.getName()
					+ ";tps=" + song.getTPS()
					+ ";author=" + (song.getAuthor() != null ? song.getAuthor() : "")
					+ ";original-author=" + (song.getOriginalAuthor() != null ? song.getOriginalAuthor() : "")
					+ (song.getCustomSounds() != null && !song.getCustomSounds().isEmpty() ? ";use-custom-sounds=true"
							: ""));
			w.newLine();
			if (song.getCustomSounds() != null && !song.getCustomSounds().isEmpty()) {
				String csLine = "";
				for (String snd : song.getCustomSounds().keySet()) {
					csLine += snd + "=" + song.getCustomSounds().get(snd) + ";";
				}
				w.write(csLine.substring(0, csLine.length() - 1));
				w.newLine();
			}
			int skipTicks = 0;
			for (int tick = 0; tick < song.getLength(); tick++) {
				if (!song.areNotesAt(tick)) {
					skipTicks++;
					continue;
				}
				String line = "";
				if (skipTicks > 0) {
					line += "p," + skipTicks + ";";
					skipTicks = 0;
				}
				for (int layer = 0; layer < song.getHeight(); layer++) {
					Layer l = song.getLayers().get(layer);
					if (l == null)
						continue;
					Note n = l.getNote(tick);
					if (n != null) {
						if (!n.isCustom()) {
							line += "n," + Tools.getSoundID(n.getSound()) + "," + n.getNote() + ","
									+ (n.getVolume() > 0 ? n.getVolume() : l.getVolume()) + ";";
						} else {
							line += "n," + n.getCustomSound() + "," + n.getNote() + ","
									+ (n.getVolume() > 0 ? n.getVolume() : l.getVolume()) + ";";
						}
					}
				}
				w.write(line.substring(0, line.length() - 1));
				w.newLine();
			}
		}
	}

	@Override
	public boolean supportsSongArchives() {
		return false;
	}

}
