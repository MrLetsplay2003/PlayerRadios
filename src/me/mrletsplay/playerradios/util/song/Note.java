package me.mrletsplay.playerradios.util.song;

import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;

public class Note {

	private VersionedSound sound;
	
	private int note;
	private int volume;
	private int panning;
	private short pitch;
	
	private boolean isCustom;
	private String customSound;
	
	public Note(VersionedSound sound, int note, int volume, int panning, short pitch) {
		this.sound = sound;
		this.note = note;
		this.volume = volume;
		this.panning = panning;
		this.pitch = pitch;
		this.isCustom = false;
	}
	
	public Note(VersionedSound sound, int note) {
		this(sound, note, -1, 100, (short) 0);
	}
	
	public Note(VersionedSound sound, int note, int volume) {
		this(sound, note, volume, 100, (short) 0);
	}
	
	public Note(String customSound, int note, int volume) {
		this.sound = null;
		this.note = note;
		this.volume = volume;
		this.isCustom = true;
		this.customSound = customSound;
	}
	
	public VersionedSound getSound() {
		return sound;
	}
	
	public int getNote() {
		return note;
	}
	
	public int getVolume() {
		return volume;
	}
	
	public int getPanning() {
		return panning;
	}
	
	public short getFinePitch() {
		return pitch;
	}
	
	public boolean isCustom() {
		return isCustom;
	}
	
	public String getCustomSound() {
		return customSound;
	}
	
}
