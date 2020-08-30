package me.mrletsplay.playerradios.util.songloader;

public class SongLoadingException extends RuntimeException {

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
