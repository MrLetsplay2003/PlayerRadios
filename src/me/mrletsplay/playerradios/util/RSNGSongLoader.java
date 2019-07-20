package me.mrletsplay.playerradios.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;

import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;
import me.mrletsplay.playerradios.Main;
import me.mrletsplay.playerradios.util.song.Layer;
import me.mrletsplay.playerradios.util.song.Note;
import me.mrletsplay.playerradios.util.song.Song;

public class RSNGSongLoader {

	public static Song loadSong(File f) throws Exception {
		BufferedReader r = new BufferedReader(new FileReader(f));
		try {
			int id = -1;
			int ln = 0;
			String name = null;
			float tps = -1;
			String author = "";
			String oAuthor = "";
			boolean useCustomSounds = false;
			try {
				String sInfo;
				while((sInfo=r.readLine())!=null) {
					ln++;
					if(!(sInfo.startsWith("//") || sInfo.equals(""))) {
						break;
					}
				}
				String[] spl = sInfo.split(";");
				for(String s : spl) {
					String[] spl2 = s.split("=");
					if(spl2.length==2) {
						String type =  spl2[0].trim();
						String val = spl2[1].trim();
						if(type.equalsIgnoreCase("id")) {
							id = Integer.parseInt(val);
						}else if(type.equalsIgnoreCase("name")) {
							name = val;
						}else if(type.equalsIgnoreCase("tps")) {
							tps = Float.parseFloat(val);
						}else if(type.equalsIgnoreCase("author")) {
							author = val;
						}else if(type.equalsIgnoreCase("original-author")) {
							oAuthor = val;
						}else if(type.equalsIgnoreCase("use-custom-sounds")) {
							useCustomSounds = Tools.stringToBoolean(val);
						}
					}else{
						Main.pl.getLogger().info("Failed to load song from "+f.getName()+". Error at line: "+ln);
						r.close();
						return null;
					}
				}
			}catch(Exception e) {
				Main.pl.getLogger().info("Failed to load song header of "+f.getName());
				e.printStackTrace();
				r.close();
				return null;
			}
			if(name == null || tps==-1) {
				Main.pl.getLogger().info("Failed to load song \""+f.getName()+"\"! (Invalid song header)");
				if(name==null) Main.pl.getLogger().info("Name not set");
				if(tps==-1) Main.pl.getLogger().info("TPS not set");
			}
			HashMap<String, String> customSounds = new HashMap<>();
			if(useCustomSounds) {
				try {
					String cSounds;
					while((cSounds=r.readLine())!=null) {
						ln++;
						if(!(cSounds.startsWith("//") || cSounds.equals(""))) {
							break;
						}
					}
					String[] spl = cSounds.split(";");
					for(String s : spl) {
						String[] spl2 = s.split("=");
						if(spl2.length==2) {
							String type =  spl2[0].trim();
							String val = spl2[1].trim();
							customSounds.put(type, val);
						}else{
							Main.pl.getLogger().info("Failed to load song from "+f.getName()+". Error at line: "+ln);
							r.close();
							return null;
						}
					}
				}catch(Exception e) {
					Main.pl.getLogger().info("Failed to load custom instruments of "+f.getName());
					e.printStackTrace();
					r.close();
					return null;
				}
			}
			String line;
			int currTick = 0;
			int hLayer = 0;
			HashMap<Integer, Layer> layers = new HashMap<>();
			while((line = r.readLine()) != null) {
				int currLayer = 0;
				if(line.equals("") || line.startsWith("//")) {
					continue;
				}
				ln++;
				try {
					String[] nSpl = line.split(";");
					for(String n : nSpl) {
						String[] spl2 = n.split(",");
						String type = spl2[0].trim();
						if(type.equalsIgnoreCase("pause") || type.equalsIgnoreCase("p")) {
							if(spl2.length!=2) {
								Main.pl.getLogger().info("Invalid pause at line "+ln);
								r.close();
								return null;
							}
							currTick += Integer.parseInt(spl2[1].trim());
						}else if(type.equalsIgnoreCase("note") || type.equalsIgnoreCase("n")) {
							if(spl2.length<3 || spl2.length>4) {
								Main.pl.getLogger().info("Invalid note at line "+ln);
								r.close();
								return null;
							}
							VersionedSound s;
							String cSound = null;
							try {
								s = Tools.getSound(Byte.parseByte(spl2[1].trim()));
							}catch(NumberFormatException e) {
								s = Tools.getSound(Tools.getSoundID(VersionedSound.valueOf(spl2[1].trim().toUpperCase())));
							}
							if(s==null) {
								cSound = customSounds.get(spl2[1]);
							}
							if(s==null && cSound == null) {
								s = Tools.getSound(0);
							}
							int note = Integer.parseInt(spl2[2].trim());
							int volume = 10;
							if(spl2.length==4) {
								volume = Integer.parseInt(spl2[3].trim());
							}
							Layer l = getLayer(layers, currLayer);
							l.setNote(currTick, s != null?new Note(s, note, volume):new Note(cSound, note, volume));
							layers.put(currLayer, l);
						}
						currLayer++;
					}
					if(currLayer>hLayer) {
						hLayer = currLayer;
					}
					currTick++;
				}catch(Exception e) {
					Main.pl.getLogger().info("Failed to load song. Caught exception at line "+ln+". If this is not your mistake, please report this bug with the below stack trace");
					e.printStackTrace();
					r.close();
					return null;
				}
			}
			r.close();
			return new Song(id, currTick, hLayer, name, layers, tps, (author.equals("")?null:author), (oAuthor.equals("")?null:oAuthor), customSounds, null);
		}catch(Exception e) {
			r.close();
			throw e;
		}
	}
	
	public static File saveSong(Song s) throws Exception {
		String sName = Tools.validateName(s.getName());
		File f = new File(Main.pl.getDataFolder(),"/export/rsng/"+sName+"."+s.getID()+".rsng");
		saveSong(s, f);
		return f;
	}
	
	public static void saveSong(Song s, File f) throws Exception {
		f.getParentFile().mkdirs();
		BufferedWriter w = new BufferedWriter(new FileWriter(f));
		w.write("//Automatically generated song file");
		w.newLine();
		w.write("name="+s.getName()+";tps="+s.getTPS()+";author="+(s.getAuthor()!=null?s.getAuthor():"")+";original-author="+(s.getOriginalAuthor()!=null?s.getOriginalAuthor():"")+(s.getCustomSounds()!=null&&!s.getCustomSounds().isEmpty()?";use-custom-sounds=true":""));
		w.newLine();
		if(s.getCustomSounds()!=null&&!s.getCustomSounds().isEmpty()) {
			String csLine = "";
			for(String snd : s.getCustomSounds().keySet()) {
				csLine+=snd+"="+s.getCustomSounds().get(snd)+";";
			}
			w.write(csLine.substring(0, csLine.length()-1));
			w.newLine();
		}
		int skipTicks = 0;
		for(int tick = 0; tick < s.getLength(); tick++) {
			if(!s.areNotesAt(tick)) {
				skipTicks++;
				continue;
			}
			String line = "";
			if(skipTicks>0) {
				line+="p,"+skipTicks+";";
				skipTicks=0;
			}
			for(int layer = 0; layer < s.getHeight(); layer++) {
				Layer l = s.getLayers().get(layer);
				if(l==null) continue;
				Note n = l.getNote(tick);
				if(n!=null) {
					if(!n.isCustom()) {
						line+="n,"+Tools.getSoundID(n.getSound())+","+n.getNote()+","+(n.getVolume()>0?n.getVolume():l.getVolume())+";";
					}else {
						line+="n,"+n.getCustomSound()+","+n.getNote()+","+(n.getVolume()>0?n.getVolume():l.getVolume())+";";
					}
				}
			}
			w.write(line.substring(0, line.length()-1));
			w.newLine();
		}
		w.close();
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
