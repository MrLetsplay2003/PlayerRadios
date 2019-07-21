package me.mrletsplay.playerradios.util.songloader.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

public class SNGSongLoader implements SongLoader {

	@Override
	public String getName() {
		return "sng";
	}

	@Override
	public List<Song> loadSongs(File file) throws IOException {
		if (file.length() == 0)
			return Collections.emptyList();
		try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			List<Song> songs = new ArrayList<>();
			byte[] sngT = read(in, 4);
			String fType = new String(sngT);
			boolean isFinished = false;
			if (!fType.equals("sngF"))
				throw new SongLoadingException("File provided is not an SNG-File");
			while (!isFinished) {
				HashMap<String, String> customSounds = new HashMap<>();
				int id = in.readShort();
				String name = in.readUTF();
				String author = in.readUTF();
				String oAuthor = in.readUTF();
				int length = in.readInt();
				float tps = in.readShort() / 100f;
				int height = in.readUnsignedByte();
				HashMap<Integer, Layer> layers = new HashMap<>();
				for(int i = 0; i < height; i++) {
					layers.put(i, new Layer());
				}
				int currTick = 0;
				while (true) {
					int currLayer = 0;
					int note = in.readUnsignedByte();
					int nType = note >> 6;
					boolean con = ((note >> 5) & 0x01) == 1;
					int data1 = note & 0x1F;
					if (nType == 0) {
						// Note
						int data = in.readUnsignedByte();
						Layer l = layers.get(currLayer);
						while (l.getNote(currTick) != null) {
							currLayer++;
							l = layers.get(currLayer);
						}
						l.setVolume(100);
						VersionedSound s = Tools.getSound(data);
						String cSound = null;
						if (s == null)
							cSound = customSounds.get("" + data);
						if (s == null && cSound == null)
							throw new SongLoadingException("Invalid instrument id");
						l.setNote(currTick, s != null ? new Note(s, data1) : new Note(cSound, data1, -1));
					} else if (nType == 1) {
						// Pause
						int data2 = in.readUnsignedByte();
						short sTicks = (short) ((data1 << 8) + data2);
						currTick += (sTicks);
					} else if (nType == 2) {
						// song/file end
						if (data1 == 1)
							isFinished = true;
						break;
					} else if (nType == 3) {
						// Custom Instrument
						int data2 = in.readUnsignedByte();
						short cID = (short) ((data1 << 8) + data2);
						String sound = in.readUTF();
						customSounds.put("" + cID, sound);
					}
					if (con)
						currTick++;
				}
				songs.add(new Song(id, length, height, name, layers, tps, (author.equals("") ? null : author),
						(oAuthor.equals("") ? null : oAuthor), customSounds, file));
			}
			return songs;
		}
	}

	@Override
	public void saveSongs(File file, Song... songs) throws IOException {
		IOUtils.createFile(file);
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
			out.write("sngF".getBytes());
			for (int song = 0; song < songs.length; song++) {
				if (Thread.interrupted())
					return;
				Song s = songs[song];
				out.writeShort(s.getID());
				out.writeUTF(s.getName());
				out.writeUTF((s.getAuthor() != null ? s.getAuthor() : ""));
				out.writeUTF((s.getOriginalAuthor() != null ? s.getOriginalAuthor() : ""));
				out.writeInt(s.getLength());
				out.writeShort((int) (s.getTPS() * 100));
				out.write(s.getHeight());
				short skipTicks = 0;
				short cInstrID = (short) (Tools.highestSoundID() + 1);
				HashMap<String, Short> cSoundIDs = new HashMap<>();
				if (s.getCustomSounds() != null) {
					for (String snd : s.getCustomSounds().keySet()) {
						out.write(0xc0 + ((cInstrID >> 8) & 0x1f));
						out.write(cInstrID & 0xff);
						out.writeUTF(s.getCustomSounds().get(snd));
						cSoundIDs.put(s.getCustomSounds().get(snd), cInstrID);
						cInstrID++;
					}
				}
				for (int tick = 0; tick < s.getLength(); tick++) {
					if (!s.areNotesAt(tick)) {
						skipTicks++;
						continue;
					}

					if (skipTicks > 0) {
						out.write(0x40 + ((skipTicks >> 8) & 0x1f)); // ---xxxxx --------
						out.write(skipTicks & 0xff);// -------- xxxxxxxx
						skipTicks = 0;
					}

					List<Note> ns = s.getNotesAt(tick);
					for (int i = 0; i < ns.size() - 1; i++) {
						Note n = ns.get(i);
						int note = (n.getNote() & 0x1F);
						int data;
						if (n.isCustom()) {
							data = cSoundIDs.get(n.getCustomSound());
						} else {
							data = Tools.getSoundID(n.getSound());
						}
						out.write(note);
						out.write(data);
					}
					// Write last note of tick
					Note n = ns.get(ns.size() - 1);
					int note = (n.getNote() & 0x3f) + (1 << 5);
					int data;
					if (n.isCustom()) {
						if (!cSoundIDs.containsKey(n.getCustomSound())) {
							file.delete();
							throw new SongLoadingException("Failed to save song \"" + s.getName() + "\": Custom sound \""
									+ n.getCustomSound() + "\" not set");
						}
						data = cSoundIDs.get(n.getCustomSound());
					} else {
						data = Tools.getSoundID(n.getSound());
					}
					out.write(note);
					out.write(data);
				}
				out.write(song == songs.length - 1 ? 0x81 : 0x80);
			}
		}
	}

	private static byte[] read(DataInputStream in, int am) throws IOException {
		byte[] b = new byte[am];
		in.read(b);
		return b;
	}

	@Override
	public boolean supportsSongArchives() {
		return true;
	}

}
