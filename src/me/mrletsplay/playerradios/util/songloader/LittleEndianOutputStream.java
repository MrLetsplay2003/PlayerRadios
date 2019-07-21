package me.mrletsplay.playerradios.util.songloader;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LittleEndianOutputStream extends DataOutputStream {

	public LittleEndianOutputStream(OutputStream out) {
		super(out);
	}

	public void writeLEShort(short val) throws IOException {
		write(val & 0xFF);
		write(val >> 8);
	}

	public void writeLEInt(int val) throws IOException {
		write(val & 0xFF);
		write(val >> 8 & 0xFF);
		write(val >> 16 & 0xFF);
		write(val >> 24);
	}

	public void writeLEString(String val) throws IOException {
		writeLEInt(val.length());
		for (char c : val.toCharArray()) {
			writeByte(c);
		}
	}

}
