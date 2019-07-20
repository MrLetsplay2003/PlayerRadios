package me.mrletsplay.playerradios.util.songloader.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

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

public class NBSSongLoader implements SongLoader {

	@Override
	public String getName() {
		return "nbs";
	}

	@Override
	public Song loadSong(File file) throws IOException {
		try (LittleEndianInputStream dIn = new LittleEndianInputStream(new FileInputStream(file))) {
			HashMap<Integer, Layer> layers = new HashMap<>();
			short length = dIn.readLEShort();
			short songHeight = dIn.readLEShort();
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
			short tick = -1;
			while (true) {
				short jTicks = dIn.readLEShort(); // jumps to next tick
				if (jTicks == 0) {
					break;
				}
				tick += jTicks;
				if (tick > length) {
					length = tick;
				}
				short layer = -1;
				while (true) {
					short jLayers = dIn.readLEShort();
					if (jLayers == 0) {
						break;
					}
					layer += jLayers;
					if (layer > songHeight) {
						songHeight = layer;
					}
					byte instrument = dIn.readByte();
					byte note = dIn.readByte();
					Layer l = layers.get((int) layer);
					if (l == null) {
						l = new Layer();
					}
					VersionedSound s = Tools.getSound(instrument);
					if (s == null) {
						s = Tools.getSound(0);
					}
					int p = note - 33;
					if (Config.use_alternate_nbs_import) {
						while (p > 24) {
							p -= 24;
						}
						while (p < 0) {
							p += 24;
						}
					}
					l.setNote(tick, new Note(s, p));
					layers.put((int) layer, l);
				}
			}
			if (tick > length) { // Should never be the case
				length = tick;
			}
			try {
				for (int i = 0; i < songHeight; i++) {
					Layer l = layers.get(i);
					if (l != null) {
						dIn.readLEString(); // Layer name
						l.setVolume((int) (dIn.readByte() / 10f)); // Same as (vol/100f)*10
					}
					layers.put(i, l);
				}
			} catch (Exception e) {
				for (int i = 0; i < songHeight; i++) {
					Layer l = layers.get(i);
					if (l != null) {
						l.setVolume(10);
					}
					layers.put(i, l);
				}
			}
			// Custom instruments are ignored
			return new Song(-1, length, songHeight, title, layers, speed, (author.equals("") ? null : author),
					(originalAuthor.equals("") ? null : originalAuthor), null, null);
		} catch (IOException e) {
			throw e;
		}
	}

	@Override
	public void saveSong(Song song, File file) throws IOException {
		IOUtils.createFile(file);
		try (LittleEndianOutputStream dO = new LittleEndianOutputStream(new FileOutputStream(file))) {
			dO.writeLEShort((short) song.getLength());
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
					if (l == null) {
						continue;
					}
					Note n = l.getNote(tick);
					if (n == null) {
						skipLayers++;
						continue;
					}
					dO.writeLEShort((short) ((short) skipLayers + 1));
					skipLayers = 0;
					dO.write(Tools.getSoundID(n.getSound()));
					dO.write(n.getNote() + 33);
				}
				dO.writeLEShort((short) 0);
			}
			dO.writeLEShort((short) 0);
			for (int i = 0; i < song.getHeight(); i++) {
				dO.writeLEString(""); // Layer name
				dO.write(100);
			}
			dO.write(0);
		} catch (IOException e) {
			throw e;
		}
	}

}
