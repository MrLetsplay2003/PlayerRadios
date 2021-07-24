package me.mrletsplay.playerradios.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.WordUtils;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import me.mrletsplay.mrcore.bukkitimpl.ItemUtils;
import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedDyeColor;
import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedMaterial;
import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedSound;
import me.mrletsplay.playerradios.Config;

public class Tools {

	private static DecimalFormat df = new DecimalFormat("##.##");
	
	private static final Map<Integer, VersionedSound> SOUNDS;
	
	static {
		SOUNDS = new HashMap<>();
		SOUNDS.put(0, VersionedSound.BLOCK_NOTE_BLOCK_HARP);
		SOUNDS.put(1, VersionedSound.BLOCK_NOTE_BLOCK_BASS);
		SOUNDS.put(2, VersionedSound.BLOCK_NOTE_BLOCK_BASEDRUM);
		SOUNDS.put(3, VersionedSound.BLOCK_NOTE_BLOCK_SNARE);
		SOUNDS.put(4, VersionedSound.BLOCK_NOTE_BLOCK_HAT);
		SOUNDS.put(5, VersionedSound.BLOCK_NOTE_BLOCK_GUITAR);
		SOUNDS.put(6, VersionedSound.BLOCK_NOTE_BLOCK_FLUTE);
		SOUNDS.put(7, VersionedSound.BLOCK_NOTE_BLOCK_BELL);
		SOUNDS.put(8, VersionedSound.BLOCK_NOTE_BLOCK_CHIME);
		SOUNDS.put(9, VersionedSound.BLOCK_NOTE_BLOCK_XYLOPHONE);
		SOUNDS.put(10, VersionedSound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE);
		SOUNDS.put(11, VersionedSound.BLOCK_NOTE_BLOCK_COW_BELL);
		SOUNDS.put(12, VersionedSound.BLOCK_NOTE_BLOCK_DIDGERIDOO);
		SOUNDS.put(13, VersionedSound.BLOCK_NOTE_BLOCK_BIT);
		SOUNDS.put(14, VersionedSound.BLOCK_NOTE_BLOCK_BANJO);
		SOUNDS.put(15, VersionedSound.BLOCK_NOTE_BLOCK_PLING);
	}

	public static ItemStack plus(VersionedDyeColor col) {
		ItemStack i = ItemUtils.blankBanner(col);
		BannerMeta m = (BannerMeta) i.getItemMeta();
		m.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
		DyeColor c = col.getBukkitDyeColor();
		m.addPattern(new Pattern(c, PatternType.BORDER));
		m.addPattern(new Pattern(c, PatternType.STRIPE_TOP));
		m.addPattern(new Pattern(c, PatternType.STRIPE_BOTTOM));
		i.setItemMeta(m);
		return i;
	}

	public static ItemStack minus(VersionedDyeColor col) {
		ItemStack i = ItemUtils.blankBanner(col);
		BannerMeta m = (BannerMeta) i.getItemMeta();
		m.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		DyeColor c = col.getBukkitDyeColor();
		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
		m.addPattern(new Pattern(c, PatternType.BORDER));
		i.setItemMeta(m);
		return i;
	}
	
	@Deprecated
	public static ItemStack bannerStack(VersionedDyeColor color) {
		return ItemUtils.createVersioned(VersionedMaterial.getBanner(color));
	}

	public static void changeInv(Inventory oldInv, Inventory newInv) {
		int i = 0;
		for (ItemStack it : newInv.getContents()) {
			oldInv.setItem(i, it);
			i++;
		}
	}

	public static boolean stringToBoolean(String s) {
		if (s.equalsIgnoreCase("true") || Config.true_keywords.contains(s)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static VersionedSound getSound(int instrument) {
		return SOUNDS.get(instrument);
	}

	public static short highestSoundID() {
		return SOUNDS.keySet().stream().max(Integer::compare).map(Integer::shortValue).get();
	}
	
	public static int getSoundID(VersionedSound sound) {
		return SOUNDS.entrySet().stream().filter(en -> en.getValue().equals(sound)).map(en -> en.getKey()).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid sound"));
	}
	
	public static String httpPost(URL url, String... params) throws IOException {
		String urlParameters = Arrays.stream(params).collect(Collectors.joining("&"));
		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();           
		conn.setDoOutput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
		conn.setUseCaches(false);
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.write(postData);
		InputStream in = conn.getInputStream();
		byte[] dat = new byte[1024];
		in.read(dat);
		return new String(dat).trim();
	}
	
	public static String httpGet(URL url, String... params) throws IOException {
		URL url2 = new URL(url.toString()+"?"+Arrays.stream(params).collect(Collectors.joining("&")));
		InputStream in = url2.openStream();
		byte[] dat = new byte[1024];
		in.read(dat);
		return new String(dat).trim();
	}
	
	public static String formatTime(int ms) {
		String[] formats = Config.getMessage("time-format").split(",");
		if(ms<60000) {
			return ms/1000+formats[0];
		}else if(ms<60*60000) {
			return ms/60000+formats[1];
		}else {
			return ms/(60*60000)+formats[2];
		}
	}

	public static String validateName(String s) {
		String tR = "";
		List<Character> chars = Arrays.asList(new Character[] { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' });
		for (char c : s.toCharArray()) {
			if (chars.contains(c)) {
				tR += "-";
			} else {
				tR += c;
			}
		}
		return tR;
	}

	public static String timeTaken(long from, long to, boolean s) {
		if (!s) {
			return "" + (to - from);
		} else {
			return df.format((to - from) / 1000D);
		}
	}

	public static List<String> split(String s, int len, String col) {
		String[] spl = WordUtils.wrap(col + s, 50).split(System.getProperty("line.separator"));
		List<String> fn = new ArrayList<>();
		for (int i1 = 0; i1 < spl.length; i1++) {
			fn.add(col + spl[i1]);
		}
		return fn;
	}

	public static void download(URL url, File file) throws IOException {
		file.getParentFile().mkdirs();
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		FileOutputStream fos = new FileOutputStream(file);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	}

	public static File downloadWithFileName(URL url, File toFolder) throws IOException {
		toFolder.mkdirs();
		HttpURLConnection hc = (HttpURLConnection) url.openConnection();
		String raw = hc.getHeaderField("Content-Disposition");
		String fileName;
		if (raw != null && raw.indexOf("=") != -1) {
			fileName = raw.split("=")[1];
		} else {
			int nm = 0;
			while (new File(toFolder, (fileName = nm + "")).exists())
				nm++;
		}
		File f = new File(toFolder, fileName);
		download(url, f);
		return f;
	}

	public static int getFileSize(URL url) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.getInputStream();
			return conn.getContentLength();
		} catch (IOException e) {
			return -1;
		} finally {
			if(conn != null) conn.disconnect();
		}
	}
	
	public static double similarity(String f, String f2){
		char[] ch = f.toCharArray();
		char[] ch2 = f2.toCharArray();
		int a = (ch.length>ch2.length?0:1);
		char[] b = a==0?ch:ch2;
		char[] s = a==1?ch:ch2;
		double mostAccPerc = -Double.MAX_VALUE;
		for(int i = 0; i <= b.length - s.length; i++){
			int p = 0;
			int p2 = 0;
			for(int c = i; c < i+s.length; c++){
				if(s[c-i] == b[c]){
					p2++;
				}

				if(p2>p){
					p = p2;
				}
			}
			double perc = ((double)p/(double)s.length)*(1-(Math.abs(f2.length()-f.length())/(double)(f2.length()>f.length()?f2.length():f.length())));
			if(perc > mostAccPerc){
				mostAccPerc = perc;
			}
		}
		return mostAccPerc;
	}

	public static void addItemsSafely(Player p, ItemStack... items) {
		HashMap<Integer, ItemStack> lo = p.getInventory().addItem(items);
		for(ItemStack  i : lo.values()) {
			p.getWorld().dropItem(p.getLocation(), i);
		}
	}
	
	public static boolean isUUID(String s) {
		return s.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
	}
	
}
