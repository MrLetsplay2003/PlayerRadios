package me.mrletsplay.playerradios.util.song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Song {

	private Map<Integer, Layer> layers;
	private int length, ID, height;
	private float tps;
	private String name, author, originalAuthor;
	private Map<String, String> customSounds;
	private File songFile;
	
	public Song(int id, int length, int height, String name, Map<Integer, Layer> layers, float tps, String author, String originalAuthor, Map<String, String> customSounds, File songFile) {
		this.ID = id;
		this.length = length;
		this.height = height;
		this.name = name;
		this.author = author;
		this.originalAuthor = originalAuthor;
		this.layers = layers;
		this.tps = tps;
		this.customSounds = customSounds;
		this.songFile = songFile;
	}
	
	public void setID(int iD) {
		ID = iD;
	}
	
	public int getID() {
		return ID;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public void setOriginalAuthor(String originalAuthor) {
		this.originalAuthor = originalAuthor;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAuthor() {
		return author;
	}
	
	public String getOriginalAuthor() {
		return originalAuthor;
	}
	
	public Map<Integer, Layer> getLayers() {
		return layers;
	}
	
	public float getTPS() {
		return tps;
	}
	
	public int getLength() {
		return length;
	}
	
	public int getHeight() {
		return height;
	}
	
	public Note getNoteAt(int tick, int layer) {
		Layer l = layers.get(layer);
		if(l!=null) {
			return l.getNote(tick);
		}
		return null;
	}
	
	public boolean areNotesAt(int tick){
		for(int l = 0; l < height; l++) {
			Layer la = layers.get(l);
			if(la==null) continue;
			if(la.getNote(tick)!=null) {
				return true;
			}
		}
		return false;
	}
	
	public List<Note> getNotesAt(int tick){
		List<Note> notes = new ArrayList<>();
		for(int l = 0; l < height; l++) {
			Layer la = layers.get(l);
			if(la==null) continue;
			if(la.getNote(tick)!=null) {
				notes.add(la.getNote(tick));
			}
		}
		return notes;
	}
	
	public Map<String, String> getCustomSounds() {
		return customSounds;
	}
	
	public void setSongFile(File songFile) {
		this.songFile = songFile;
	}
	
	public File getSongFile() {
		return songFile;
	}
	
}
