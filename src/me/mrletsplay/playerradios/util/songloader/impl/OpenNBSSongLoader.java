package me.mrletsplay.playerradios.util.songloader.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;
import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.util.Tools;
import me.mrletsplay.playerradios.util.song.Layer;
import me.mrletsplay.playerradios.util.song.Note;
import me.mrletsplay.playerradios.util.song.Song;
import me.mrletsplay.playerradios.util.songloader.LittleEndianInputStream;
import me.mrletsplay.playerradios.util.songloader.LittleEndianOutputStream;
import me.mrletsplay.playerradios.util.songloader.SongLoader;
import me.mrletsplay.playerradios.util.songloader.SongLoadingException;

public class OpenNBSSongLoader implements SongLoader {

	@Override
	public String getName() {
		return "opennbs";
	}

	@Override
	public String getFileExtension() {
		return "nbs";
	}

	@Override
	public List<Song> loadSongs(File file) throws IOException {
		try (LittleEndianInputStream dIn = new LittleEndianInputStream(new FileInputStream(file))) {
			HashMap<Integer, Layer> layers = new HashMap<>();
			int length = 0;
			short fTB = dIn.readLEShort();
			if(fTB != 0) throw new SongLoadingException("File is not an opennbs file (maybe it's a normal nbs file)");
			byte nbsV = dIn.readByte();
			if(nbsV > 5) throw new SongLoadingException("Invalid/Unsupported OpenNBS version: " + nbsV);
			dIn.readByte(); // Vanilla instrument count / First custom instrument index
			if(nbsV >= 3) dIn.readLEShort(); // Song length

			short songHeight = dIn.readLEShort();
			for(int i = 0; i < songHeight; i++) {
				layers.put(i, new Layer());
			}

			String title = dIn.readLEString();
			if (title.equals(""))
				title = file.getName().substring(0, file.getName().lastIndexOf('.'))
						+ (Config.show_automatically_named ? " (Automatically named)" : "");
			String author = dIn.readLEString();
			String originalAuthor = dIn.readLEString();
			if (originalAuthor.equals("") && Config.detect_original_author) {
				String[] spl = title.split(" - ", 2);
				if (spl.length == 2) {
					originalAuthor = spl[0];
					title = spl[1];
				} else {
					spl = title.split("-", 2);
					if (spl.length == 2) {
						originalAuthor = spl[0];
						title = spl[1];
					}
				}
			}

			dIn.readLEString(); // Description
			float speed = dIn.readLEShort() / 100f;
			dIn.readBoolean(); // Auto-save
			dIn.readByte(); // Auto-save duration
			dIn.readByte(); // Time signature
			dIn.readLEInt(); // Minutes spent
			dIn.readLEInt(); // Left clicks
			dIn.readLEInt(); // Right clicks
			dIn.readLEInt(); // Blocks added
			dIn.readLEInt(); // Blocks removed
			dIn.readLEString(); // Midi/schematic file name

			if(nbsV >= 4) {
				dIn.readBoolean(); // TODO: Looping on/off
				dIn.readByte(); // Max loop count
				dIn.readLEShort(); // Loop start tick
			}

			short tick = -1;
			while (true) {
				short jTicks = dIn.readLEShort(); // jumps to next tick
				if (jTicks == 0) {
					break;
				}
				tick += jTicks;
				length = Math.max(length, tick);
				short layer = -1;
				while (true) {
					short jLayers = dIn.readLEShort();
					if (jLayers == 0) {
						break;
					}
					layer += jLayers;
					songHeight = (short) Math.max(songHeight, layer);
					byte instrument = dIn.readByte();
					byte note = dIn.readByte();
					Layer l = getLayer(layers, layer);
					VersionedSound s = Tools.getSound(instrument);
					if (s == null) throw new SongLoadingException("Invalid instrument id");
					int p = note - 33;
					if (Config.use_alternate_nbs_import) {
						while (p > 24) {
							p -= 12;
						}
						while (p < 0) {
							p += 12;
						}
					}

					if(nbsV >= 4) {
						int velocity = dIn.readByte();
						int panning = dIn.readByte();
						short pitch = dIn.readLEShort();
						l.setNote(tick, new Note(s, p, velocity, panning, pitch));
					}else {
						l.setNote(tick, new Note(s, p));
					}
				}
			}

			try {
				for (int i = 0; i < songHeight; i++) {
					Layer l = getLayer(layers, i);
					dIn.readLEString(); // Layer name
					l.setVolume(dIn.readByte());
					if(nbsV >= 2) {
						l.setStereo(dIn.readByte()); // Layer Stereo
					}else {
						l.setStereo(100);
					}
				}
			} catch (Exception e) {
				for (int i = 0; i < songHeight; i++) {
					Layer l = getLayer(layers, i);
					l.setVolume(100);
					l.setStereo(100);
				}
			}

			// TODO: Custom instruments are ignored
			return Collections.singletonList(
					new Song(-1, length, songHeight, title, layers, speed, (author.equals("") ? null : author),
							(originalAuthor.equals("") ? null : originalAuthor), null, null));
		}
	}

	private Layer getLayer(Map<Integer, Layer> layers, int layer) {
		Layer l = layers.get(layer);
		if(l == null) {
			l = new Layer();
			layers.put(layer, l);
		}
		return l;
	}

	@Override
	public void saveSongs(File file, Song... songs) throws IOException {
		if(songs.length != 1) throw new UnsupportedOperationException("Can't save multiple songs to one file");
		Song song = songs[0];
		IOUtils.createFile(file);
		try (LittleEndianOutputStream dO = new LittleEndianOutputStream(new FileOutputStream(file))) {
			dO.writeLEShort((short) 0); // opennbs identifier
			dO.writeByte(2); // opennbs version
			dO.writeByte(Tools.highestSoundID() + 1); // First custom instrument index
			dO.writeLEShort((short) song.getHeight());
			dO.writeLEString(song.getName());
			String author = (song.getAuthor() != null ? song.getAuthor() : "");
			dO.writeLEString(author);
			String oAuthor = (song.getOriginalAuthor() != null ? song.getOriginalAuthor() : "");
			dO.writeLEString(oAuthor);
			dO.writeLEString(""); // Description
			dO.writeLEShort((short) (song.getTPS() * 100));
			dO.writeBoolean(false); // Auto-save
			dO.write(0); // Auto-save duration
			dO.write(4); // Time signature
			dO.writeLEInt(1); // Minutes spent
			dO.writeLEInt(1); // Left clicks
			dO.writeLEInt(1); // Right clicks
			dO.writeLEInt(1); // Blocks added
			dO.writeLEInt(1); // Blocks removed
			dO.writeLEString(""); // Midi/schematic file name
			int skipTicks = 0;
			for (int tick = 0; tick < song.getLength(); tick++) {
				if (!song.areNotesAt(tick)) {
					skipTicks++;
					continue;
				}
				dO.writeLEShort((short) (skipTicks + 1));
				skipTicks = 0;
				short skipLayers = 0;
				for (int layer = 0; layer < song.getHeight(); layer++) {
					Layer l = song.getLayers().get(layer);
					Note n = l.getNote(tick);
					if (n == null) {
						skipLayers++;
						continue;
					}
					dO.writeLEShort((short) (skipLayers + 1));
					skipLayers = 0;
					dO.write(Tools.getSoundID(n.getSound()));
					dO.write(n.getNote() + 33);
				}
				dO.writeLEShort((short) 0);
			}
			dO.writeLEShort((short) 0);
			for (int i = 0; i < song.getHeight(); i++) {
				Layer l = song.getLayers().get(i);
				dO.writeLEString(""); // Layer name
				dO.write(l.getVolume());
				dO.write(l.getStereo());
			}
			dO.write(0);
		}
	}

	@Override
	public boolean supportsSongArchives() {
		return false;
	}

}
