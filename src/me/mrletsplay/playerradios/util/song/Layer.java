package me.mrletsplay.playerradios.util.song;

import java.util.HashMap;

public class Layer {

	private HashMap<Integer, Note> notes;
	private int volume, stereo;
	
	public Layer() {
		this.notes = new HashMap<>();
		this.volume = 10;
		this.stereo = 100;
	}
	
	public Note getNote(int tick) {
		return notes.get(tick);
	}
	
	public void setNote(int tick, Note n) {
		notes.put(tick, n);
	}
	
	public void setVolume(int volume) {
		this.volume = volume;
	}
	
	public int getVolume() {
		return volume;
	}
	
	public void setStereo(int stereo) {
		this.stereo = stereo;
	}
	
	public int getStereo() {
		return stereo;
	}
	
}
