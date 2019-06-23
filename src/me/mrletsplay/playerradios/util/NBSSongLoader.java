package me.mrletsplay.playerradios.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;
import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;

public class NBSSongLoader {

	//Reference to original: https://github.com/xxmicloxx/NoteBlockAPI/blob/master/src/main/java/com/xxmicloxx/NoteBlockAPI/NBSDecoder.java
	
	public static Song loadSong(File fIn) throws Exception {
		DataInputStream in = new DataInputStream(new FileInputStream(fIn));
		try {
			HashMap<Integer, Layer> layers = new HashMap<>();
			short length = readShort(in);
			short songHeight = readShort(in);
			String title = readString(in);
			if(title.equals("")) {
				title = fIn.getName().substring(0, fIn.getName().length()-4)+(Config.show_automatically_named?" (Automatically named)":"");
			}
			String author = readString(in); // Author
			String oAuthor = readString(in); //Original author
			if(oAuthor.equals("") && Config.detect_original_author) {
				String[] spl = title.split(" - ");
				if(spl.length>=2) {
					oAuthor = spl[0];
					title = title.substring(oAuthor.length()+3);
				}else {
					spl = title.split("-");
					if(spl.length>=2) {
						oAuthor = spl[0];
						title = title.substring(oAuthor.length()+1);
					}
				}
			}
			readString(in); // Description
			float speed = readShort(in)/ 100f;
			in.readBoolean(); // Auto-save
			in.readByte(); // Auto-save duration
			in.readByte(); // Time signature
			readInt(in); // Minutes spent
			readInt(in); // Left clicks
			readInt(in); // Right clicks
			readInt(in); // Blocks added
			readInt(in); // Blocks removed
			readString(in); // Midi/schematic file name
			short tick = -1;
			while (true) {
				short jTicks = readShort(in); // jumps till next tick
				if (jTicks == 0) {
					break;
				}
				tick += jTicks;
				if(tick>length) {
					length = tick;
				}
				short layer = -1;
				while (true) {
					short jLayers = readShort(in);
					if (jLayers == 0) {
						break;
					}
					layer += jLayers;
					if(layer>songHeight) {
						songHeight = layer;
					}
					byte instrument = in.readByte();
					byte note = in.readByte();
					Layer l = layers.get((int)layer);
					if(l==null) {
						l = new Layer();
					}
					VersionedSound s = Tools.getSound(instrument);
					if(s==null) {
						s = Tools.getSound(0);
					}
					int p = note-33;
					if(Config.use_alternate_nbs_import) {
						while(p>24) {
							p-=24;
						}
						while(p<0) {
							p+=24;
						}
					}
					l.setNote(tick, new Note(s, p));
					layers.put((int)layer, l);
				}
			}
			if(tick>length) { //Should never be the case
				length = tick;
			}
			try {
				for(int i = 0; i< songHeight; i++) {
					Layer l = layers.get(i);
					if(l!=null) {
						readString(in); //Layer name
						l.setVolume((int) (in.readByte()/10f)); //Same as (vol/100f)*10
					}
					layers.put(i, l);
				}
			}catch(Exception e) {
				//Main.pl.getLogger().info("Failed to load layer names/volumes of \""+title+"\" ("+fIn.getName()+"), ignoring...");
				for(int i = 0; i< songHeight; i++) {
					Layer l = layers.get(i);
					if(l!=null) {
						l.setVolume(10);
					}
					layers.put(i, l);
				}
			}
			//Custom instruments are ignored
			in.close();
			return new Song(-1, length, songHeight, title, layers, speed, (author.equals("")?null:author), (oAuthor.equals("")?null:oAuthor), null, null);
		}catch(Exception e) {
			in.close();
			throw e;
		}
	}
	
	public static File saveSong(Song s) throws Exception {
		String sName = Tools.validateName(s.getName());
		File f = new File(Main.pl.getDataFolder(),"/export/nbs/"+sName+"."+s.getID()+".nbs");
		saveSong(s, f);
		return f;
	}
	
	public static void saveSong(Song s, File f) throws Exception {
		f.getParentFile().mkdirs();
		DataOutputStream out = new DataOutputStream(new FileOutputStream(f));
		writeShort(out, (short) s.getLength());
		writeShort(out, (short) s.getHeight());
		writeString(out, s.getName());
		String author = (s.getAuthor()!=null?s.getAuthor():"");
		writeString(out, author);
		String oAuthor = (s.getOriginalAuthor()!=null?s.getOriginalAuthor():"");
		writeString(out, oAuthor);
		writeString(out, ""); //Description
		writeShort(out, (short) (s.getTPS()*100));
		out.writeBoolean(false); //Auto-save
		out.write(0); //Auto-save duration
		out.write(4); //Time signature
		writeInt(out, 1); //Minutes spent
		writeInt(out, 1); //Left clicks
		writeInt(out, 1); //Right clicks
		writeInt(out, 1); //Blocks added
		writeInt(out, 1); //Blocks removed
		writeString(out, ""); //Midi/schematic file name
		int skipTicks = 0;
		for(int tick = 0; tick < s.getLength(); tick++) {
			if(!s.areNotesAt(tick)) {
				skipTicks++;
				continue;
			}
			writeShort(out, (short) (skipTicks+1));
			skipTicks = 0;
			short skipLayers = 0;
			for(int layer = 0; layer < s.getHeight(); layer++) {
				Layer l = s.getLayers().get(layer);
				if(l==null) {
					continue;
				}
				Note n = l.getNote(tick);
				if(n==null) {
					skipLayers++;
					continue;
				}
				writeShort(out, (short) ((short) skipLayers+1));
				skipLayers = 0;
				out.write(Tools.getSoundID(n.getSound()));
				out.write(n.getNote()+33);
			}
			writeShort(out, (short) 0);
		}
		writeShort(out, (short) 0);
		for(int i = 0; i < s.getHeight(); i++) {
			writeString(out, "");
			out.write(100);
		}
		out.write(0);
		out.close();
	}

	private static short readShort(DataInputStream dis) throws IOException {
		int byte1 = dis.readUnsignedByte();
		int byte2 = dis.readUnsignedByte();
		return (short) (byte1 + (byte2 << 8));
	}
	
	private static void writeShort(DataOutputStream out, short val) throws IOException {
		out.write(val&0xFF);
		out.write(val>>8);
	}

	private static int readInt(DataInputStream dis) throws IOException {
		int byte1 = dis.readUnsignedByte();
		int byte2 = dis.readUnsignedByte();
		int byte3 = dis.readUnsignedByte();
		int byte4 = dis.readUnsignedByte();
		return (byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24));
	}
	
	private static void writeInt(DataOutputStream out, int val) throws IOException {
		out.write(val&0xFF);
		out.write(val>>8&0xFF);
		out.write(val>>16&0xFF);
		out.write(val>>24);
	}

	private static String readString(DataInputStream dis) throws IOException {
		int length = readInt(dis);
		StringBuilder sb = new StringBuilder(length);
		for (; length > 0; --length) {
			char c = (char) dis.readByte();
			if (c == (char) 0x0D) {
				c = ' ';
			}
			sb.append(c);
		}
		return sb.toString();
	}
	
	private static void writeString(DataOutputStream out, String val) throws IOException {
		writeInt(out, val.length());
		for(char c : val.toCharArray()) {
			out.writeByte(c);
		}
	}

}
