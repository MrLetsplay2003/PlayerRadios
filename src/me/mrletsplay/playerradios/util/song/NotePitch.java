package me.mrletsplay.playerradios.util.song;

import me.mrletsplay.mrcore.bukkitimpl.versioned.NMSVersion;

public enum NotePitch {
	
	// Post 1.9: 0.5 * pow(2, key/12)
	
	NOTE_0(0, 0.5F, 0.5F),
	NOTE_1(1, 0.53F, 0.52973F),
	NOTE_2(2, 0.56F, 0.56123F),
	NOTE_3(3, 0.6F, 0.59461F),
	NOTE_4(4, 0.63F, 0.62995F),
	NOTE_5(5, 0.67F, 0.66741F),
	NOTE_6(6, 0.7F, 0.70711F),
	NOTE_7(7, 0.76F, 0.74916F),
	NOTE_8(8, 0.8F, 0.7937F),
	NOTE_9(9, 0.84F, 0.84089F),
	NOTE_10(10, 0.9F, 0.89091F),
	NOTE_11(11, 0.94F, 0.94386F),
	NOTE_12(12, 1.0F, 1.0F),
	NOTE_13(13, 1.06F, 1.05945F),
	NOTE_14(14, 1.12F, 1.12245F),
	NOTE_15(15, 1.18F, 1.1892F),
	NOTE_16(16, 1.26F, 1.25993F),
	NOTE_17(17, 1.34F, 1.33484F),
	NOTE_18(18, 1.42F, 1.4142F),
	NOTE_19(19, 1.5F, 1.49832F),
	NOTE_20(20, 1.6F, 1.58741F),
	NOTE_21(21, 1.68F, 1.6818F),
	NOTE_22(22, 1.78F, 1.7818F),
	NOTE_23(23, 1.88F, 1.88775F),
	NOTE_24(24, 2.0F, 2.0F);

	public int note;
	public float pre1_9;
	public float post1_9;

	private NotePitch(int note, float pre1_9, float post1_9) {
		this.note = note;
		this.pre1_9 = pre1_9;
		this.post1_9 = post1_9;
	}

	public static float getPitch(int note) {
		for (NotePitch pitch : values()) {
			if (pitch.note == note) {
				return (NMSVersion.getCurrentServerVersion().isOlderThan(NMSVersion.V1_9_R1)  ? pitch.pre1_9 : pitch.post1_9);
			}
		}

		return 0.0F;
	}
}