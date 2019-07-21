package me.mrletsplay.playerradios.util.songloader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LittleEndianInputStream extends DataInputStream {

	private static final int MAX_STRING_LENGTH = 1024;

	public LittleEndianInputStream(InputStream in) {
		super(in);
	}

	public short readLEShort() throws IOException {
		int byte1 = readUnsignedByte();
		int byte2 = readUnsignedByte();
		return (short) (byte1 + (byte2 << 8));
	}

	public int readLEInt() throws IOException {
		int byte1 = readUnsignedByte();
		int byte2 = readUnsignedByte();
		int byte3 = readUnsignedByte();
		int byte4 = readUnsignedByte();
		return (byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24));
	}

	public String readLEString() throws IOException {
		int length = readLEInt();
		if (length > MAX_STRING_LENGTH) throw new IllegalArgumentException("Invalid string length: " + length); // Prevent heap overflow
		StringBuilder sb = new StringBuilder(length);
		for (; length > 0; --length) {
			char c = (char) readByte();
			if (c == (char) 0x0D)
				c = ' ';
			sb.append(c);
		}
		return sb.toString();
	}

}
