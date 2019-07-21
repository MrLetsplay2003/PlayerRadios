package me.mrletsplay.playerradios.util.songloader;

import me.mrletsplay.mrcore.misc.FriendlyException;

public class SongLoadingException extends FriendlyException {

	private static final long serialVersionUID = -1228973036987518399L;

	public SongLoadingException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public SongLoadingException(String reason) {
		super(reason);
	}

	public SongLoadingException(Throwable cause) {
		super(cause);
	}

}
