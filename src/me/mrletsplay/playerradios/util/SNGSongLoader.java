package me.mrletsplay.playerradios.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.song.Layer;
import me.mrletsplay.playerradios.util.song.Note;
import me.mrletsplay.playerradios.util.song.Song;

public class SNGSongLoader {
	
	public static List<Song> loadSongs(File f) throws Exception{
		DataInputStream in = new DataInputStream(new FileInputStream(f));
		try {
			List<Song> songs = new ArrayList<>();
			if(f.length()==0) {
				in.close();
				return songs;
			}
			byte[] sngT = read(in, 4);
			String fType = new String(sngT);
			boolean isFinished = false;
			if(fType.equals("sngF")) {
				while(!isFinished) {
					HashMap<String, String> customSounds = new HashMap<>();
					int id = in.readShort();
					String name = in.readUTF();
					String author = in.readUTF();
					String oAuthor = in.readUTF();
					int length = in.readInt();
					float tps = in.readShort()/100f;
					int height = in.readUnsignedByte();
					HashMap<Integer, Layer> layers = new HashMap<>();
					int currTick = 0;
					while(true) {
						int currLayer = 0;
						int note = in.readUnsignedByte();
						int nType = note>>6;
						boolean con = ((note>>5)&0x01)==1;
						int data1 = note&0x1F;
						if(nType == 0) {
							//Note
							int data = in.readUnsignedByte();
							Layer l = getLayer(layers, currLayer);
							while(l.getNote(currTick)!=null) {
								currLayer++;
								l = getLayer(layers, currLayer);
							}
							l.setVolume(100);
							VersionedSound s = Tools.getSound(data);
							String cSound = null;
							if(s==null) {
								cSound = customSounds.get(""+data);
							}
							if(s==null && cSound == null) {
								s = Tools.getSound(0);
							}
							l.setNote(currTick, s!=null?new Note(s, data1):new Note(cSound, data1, -1));
							layers.put(currLayer, l);
						}else if(nType == 1) {
							//Pause
							int data2 = in.readUnsignedByte();
							short sTicks = (short) ((data1<<8)+data2);
							currTick+=(sTicks);
						}else if(nType == 2) {
							//song/file end
							if(data1==1) {
								isFinished = true;
							}
							break;
						}else if(nType == 3) {
							//Custom Instrument
							int data2 = in.readUnsignedByte();
							short cID = (short) ((data1<<8)+data2);
							String sound = in.readUTF();
							customSounds.put(""+cID, sound);
						}
						if(con) {
							currTick++;
						}
					}
					songs.add(new Song(id, length, height, name, layers, tps, (author.equals("")?null:author), (oAuthor.equals("")?null:oAuthor), customSounds, f));
					if(isFinished) {
						in.close();
						return songs;
					}
				}
			}else {
				in.close();
				throw new IllegalArgumentException("File provided is not an SNG-File");
			}
			return songs;
		}catch(Exception e) {
			in.close();
			throw e;
		}
	}
	
	public static void saveSongs(Song... ss) throws Exception {
		for(Song s : ss) {
			File f = getSongFile(s);
			saveSongs(f, true, s);
		}
	}
	
	public static File getSongFile(Song s) {
		String sName = Tools.validateName(s.getName());
		File f = new File(Main.pl.getDataFolder(),"/songs/"+sName+"."+s.getID()+".sng");
		return f;
	}
	
	public static File saveSong_Export(Song s) throws Exception {
		String sName = Tools.validateName(s.getName());
		File f = new File(Main.pl.getDataFolder(),"/export/sng/"+sName+"."+s.getID()+".sng");
		saveSongs(f, true, s);
		return f;
	}
	
	public static File saveSongs_Export(Song... s) throws Exception {
		File f = new File(Main.pl.getDataFolder(),"/export/sng/all-songs-archive.sng");
		saveSongs(f, true, s);
		return f;
	}
	
	public static void saveSongs(File f, boolean keepIDs, Song... ss) throws Exception {
		f.getParentFile().mkdirs();
		if(ss.length==0) {
			return;
		}
		DataOutputStream out = new DataOutputStream(new FileOutputStream(f));
		out.write("sngF".getBytes());
		for(int song = 0; song < ss.length; song++) {
			if(Thread.interrupted()) {
				out.close();
				return;
			}
			Song s = ss[song];
			out.writeShort(keepIDs?s.getID():-1);
			out.writeUTF(s.getName());
			out.writeUTF((s.getAuthor()!=null?s.getAuthor():""));
			out.writeUTF((s.getOriginalAuthor()!=null?s.getOriginalAuthor():""));
			out.writeInt(s.getLength());
			out.writeShort((int) (s.getTPS()*100));
			out.write(s.getHeight());
			short skipTicks = 0;
			short cInstrID = (short) (Tools.highestSoundID()+1);
			HashMap<String, Short> cSoundIDs = new HashMap<>();
			if(s.getCustomSounds()!=null) {
				for(String snd : s.getCustomSounds().keySet()) {
					out.write(0xc0+((cInstrID>>8) & 0x1f));
					out.write(cInstrID&0xff);
					out.writeUTF(s.getCustomSounds().get(snd));
					cSoundIDs.put(s.getCustomSounds().get(snd), cInstrID);
					cInstrID++;
				}
			}
			for(int tick = 0; tick < s.getLength(); tick++) {
				if(!s.areNotesAt(tick)) {
					skipTicks++;
					continue;
				}
				
				if(skipTicks>0) {
					out.write(0x40+((skipTicks>>8) & 0x1f)); //---xxxxx --------
					out.write(skipTicks&0xff);//-------- xxxxxxxx
					skipTicks = 0;
				}
				
				List<Note> ns = s.getNotesAt(tick);
				for(int i = 0; i < ns.size()-1; i++) {
					Note n = ns.get(i);
					int note = (n.getNote() & 0x1F);
					int data;
					if(n.isCustom()) {
						data = cSoundIDs.get(n.getCustomSound());
					}else {
						data = Tools.getSoundID(n.getSound());
					}
					out.write(note);
					out.write(data);
				}
				//Write last note of tick
				Note n = ns.get(ns.size()-1);
				int note = (n.getNote() & 0x3f) + (1<<5);
				int data;
				if(n.isCustom()) {
					if(!cSoundIDs.containsKey(n.getCustomSound())) {
						Main.pl.getLogger().info("Failed to save song \""+s.getName()+"\": Custom sound \""+n.getCustomSound()+"\" not set");
						out.close();
						f.delete();
						return;
					}
					data = cSoundIDs.get(n.getCustomSound());
				}else {
					data = Tools.getSoundID(n.getSound());
				}
				out.write(note);
				out.write(data);
			}
			if(song==ss.length-1) {
				out.write(0x81);
			}else {
				out.write(0x80);
			}
		}
		out.close();
	}
	
	private static byte[] read(DataInputStream in, int am) throws IOException {
		byte[] b = new byte[am];
		in.read(b);
		return b;
	}
	
	private static Layer getLayer(HashMap<Integer, Layer> ls, int l) {
		Layer la = ls.get(l);
		if(la!=null) {
			return la;
		}else {
			return new Layer();
		}
	}
	
}
