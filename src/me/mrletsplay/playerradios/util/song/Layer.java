package me.mrletsplay.playerradios.util.song;

import java.util.HashMap;

public class Layer {

	private HashMap<Integer, Note> notes = new HashMap<>();
	private int volume;
	
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
	
}
