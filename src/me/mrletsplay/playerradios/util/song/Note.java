package me.mrletsplay.playerradios.util.song;

import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;

public class Note {

	private VersionedSound sound;
	private int note;
	private int volume;
	private boolean isCustom;
	private String customSound;
	
	public Note(VersionedSound sound, int note) {
		this.sound = sound;
		this.note = note;
		this.volume = -1;
		this.isCustom = false;
	}
	
	public Note(VersionedSound sound, int note, int volume) {
		this.sound = sound;
		this.note = note;
		this.volume = volume;
		this.isCustom = false;
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
	
	public boolean isCustom() {
		return isCustom;
	}
	
	public String getCustomSound() {
		return customSound;
	}
	
}
