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
	}

//	public static ItemStack createItem(NMSMaterial m, int am, String name, String... lore) {
//		MaterialDefinition d = m.getCurrentMaterialDefinition();
//		ItemStack i = new ItemStack(d.getMaterial(), am, d.getDamage());
//		ItemMeta me = i.getItemMeta();
//		if(name!=null) me.setDisplayName(name);
//		me.setLore(Arrays.stream(lore).filter(l -> !l.equals("")).collect(Collectors.toList()));
//		i.setItemMeta(me);
//		return i;
//	}
//
//	public static ItemStack createItem(Material m, int am, int dam, String name, String... lore) {
//		ItemStack i = new ItemStack(m, am, (short) dam);
//		ItemMeta me = i.getItemMeta();
//		if(name!=null) me.setDisplayName(name);
//		me.setLore(Arrays.stream(lore).filter(l -> !l.equals("")).collect(Collectors.toList()));
//		i.setItemMeta(me);
//		return i;
//	}
//
//	public static ItemStack createItem(ItemStack it, String name, String... lore) {
//		ItemStack i = new ItemStack(it);
//		ItemMeta me = i.getItemMeta();
//		if(name!=null) me.setDisplayName(name);
//		me.setLore(Arrays.stream(lore).filter(l -> !l.equals("")).collect(Collectors.toList()));
//		i.setItemMeta(me);
//		return i;
//	}
//	
//	public static ItemStack blank(DyeColor col) {
//		ItemStack i = bannerStack(col);
//		BannerMeta m = (BannerMeta) i.getItemMeta();
//		m.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
//		i.setItemMeta(m);
//		return i;
//	}
//
//	public static ItemStack arrowRight() {
//		return arrowRight(DyeColor.WHITE);
//	}
//
//	public static ItemStack arrowLeft() {
//		return arrowLeft(DyeColor.WHITE);
//	}

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
//
//	public static ItemStack arrowRight(DyeColor col) {
//		ItemStack i = bannerStack(col);
//		BannerMeta m = (BannerMeta) i.getItemMeta();
//		m.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
//		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.RHOMBUS_MIDDLE));
//		m.addPattern(new Pattern(col, PatternType.STRIPE_LEFT));
//		m.addPattern(new Pattern(col, PatternType.SQUARE_TOP_LEFT));
//		m.addPattern(new Pattern(col, PatternType.SQUARE_BOTTOM_LEFT));
//		i.setItemMeta(m);
//		return i;
//	}
//
//	public static ItemStack arrowLeft(DyeColor col) {
//		ItemStack i = bannerStack(col);
//		BannerMeta m = (BannerMeta) i.getItemMeta();
//		m.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
//		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.RHOMBUS_MIDDLE));
//		m.addPattern(new Pattern(col, PatternType.STRIPE_RIGHT));
//		m.addPattern(new Pattern(col, PatternType.SQUARE_TOP_RIGHT));
//		m.addPattern(new Pattern(col, PatternType.SQUARE_BOTTOM_RIGHT));
//		i.setItemMeta(m);
//		return i;
//	}
//
//	public static ItemStack letterC(DyeColor col) {
//		ItemStack i = bannerStack(col);
//		BannerMeta m = (BannerMeta) i.getItemMeta();
//		m.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
//		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
//		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
//		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT));
//		m.addPattern(new Pattern(col, PatternType.STRIPE_MIDDLE));
//		m.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
//		m.addPattern(new Pattern(col, PatternType.BORDER));
//		i.setItemMeta(m);
//		return i;
//	}
	
	@Deprecated
	public static ItemStack bannerStack(VersionedDyeColor color) {
//		if(Main.itemVersion == 1) {
//			return new ItemStack(Material.valueOf("BANNER"), 1, color.getDyeData());
//		}
//		switch(color) {
//			case BLACK:
//				return new ItemStack(Material.BLACK_BANNER);
//			case BLUE:
//				return new ItemStack(Material.BLUE_BANNER);
//			case BROWN:
//				return new ItemStack(Material.BROWN_BANNER);
//			case CYAN:
//				return new ItemStack(Material.CYAN_BANNER);
//			case GRAY:
//				return new ItemStack(Material.GRAY_BANNER);
//			case GREEN:
//				return new ItemStack(Material.GREEN_BANNER);
//			case LIGHT_BLUE:
//				return new ItemStack(Material.LIGHT_BLUE_BANNER);
//			case LIME:
//				return new ItemStack(Material.LIME_BANNER);
//			case MAGENTA:
//				return new ItemStack(Material.MAGENTA_BANNER);
//			case ORANGE:
//				return new ItemStack(Material.ORANGE_BANNER);
//			case PINK:
//				return new ItemStack(Material.PINK_BANNER);
//			case PURPLE:
//				return new ItemStack(Material.PURPLE_BANNER);
//			case RED:
//				return new ItemStack(Material.RED_BANNER);
//			case SILVER:
//				return new ItemStack(Material.LIGHT_GRAY_BANNER);
//			case WHITE:
//				return new ItemStack(Material.WHITE_BANNER);
//			case YELLOW:
//				return new ItemStack(Material.YELLOW_BANNER);
//		}
		return ItemUtils.createVersioned(VersionedMaterial.getBanner(color));
//		throw new UnsupportedOperationException("Something definitely went wrong");
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
	
//	public static Material versionMaterial(String post18, String post113) {
//		switch(Main.itemVersion) {
//			case 1: // 1.8 - 1.12
//				return Material.getMaterial(post18);
//			case 2: // 1.13+
//				return Material.getMaterial(post113);
//		}
//		throw new UnsupportedOperationException("Something definitely went wrong");
//	}
	
	public static VersionedSound getSound(int instrument) {
//		if (Main.noteblockVersion == 0) {
//			switch (instrument) {
//				case 0:
//					return Sound.valueOf("NOTE_PIANO");
//				case 1:
//					return Sound.valueOf("NOTE_BASS_GUITAR");
//				case 2:
//					return Sound.valueOf("NOTE_BASS_DRUM");
//				case 3:
//					return Sound.valueOf("NOTE_SNARE_DRUM");
//				case 4:
//					return Sound.valueOf("NOTE_STICKS");
//			}
//			return null;
//		}
//
//		switch (instrument) {
//			case 0:
//				return Sound.valueOf("BLOCK_NOTE_HARP");
//			case 1:
//				return Sound.valueOf("BLOCK_NOTE_BASS");
//			case 2:
//				return Sound.valueOf("BLOCK_NOTE_BASEDRUM");
//			case 3:
//				return Sound.valueOf("BLOCK_NOTE_SNARE");
//			case 4:
//				return Sound.valueOf("BLOCK_NOTE_HAT");
//		}
//
//		if (Main.noteblockVersion == 2) {
//			switch (instrument) {
//				case 5:
//					return Sound.valueOf("BLOCK_NOTE_GUITAR");
//				case 6:
//					return Sound.valueOf("BLOCK_NOTE_FLUTE");
//				case 7:
//					return Sound.valueOf("BLOCK_NOTE_BELL");
//				case 8:
//					return Sound.valueOf("BLOCK_NOTE_CHIME");
//				case 9:
//					return Sound.valueOf("BLOCK_NOTE_XYLOPHONE");
//			}
//		}
		return SOUNDS.get(instrument);
	}

	public static short highestSoundID() {
		return 9;
	}

//	public static int getSoundID(String sound) {
//		if (sound.equalsIgnoreCase("NOTE_PIANO") || sound.equalsIgnoreCase("BLOCK_NOTE_HARP")) {
//			return 0;
//		} else if (sound.equalsIgnoreCase("NOTE_BASS_GUITAR") || sound.equals("BLOCK_NOTE_BASS")) {
//			return 1;
//		} else if (sound.equalsIgnoreCase("NOTE_BASS_DRUM") || sound.equalsIgnoreCase("BLOCK_NOTE_BASEDRUM")) {
//			return 2;
//		} else if (sound.equalsIgnoreCase("NOTE_SNARE_DRUM") || sound.equalsIgnoreCase("BLOCK_NOTE_SNARE")) {
//			return 3;
//		} else if (sound.equalsIgnoreCase("NOTE_STICKS") || sound.equalsIgnoreCase("BLOCK_NOTE_HAT")) {
//			return 4;
//		} else if (sound.equalsIgnoreCase("BLOCK_NOTE_GUITAR")) {
//			return 5;
//		} else if (sound.equalsIgnoreCase("BLOCK_NOTE_FLUTE")) {
//			return 6;
//		} else if (sound.equalsIgnoreCase("BLOCK_NOTE_BELL")) {
//			return 7;
//		} else if (sound.equalsIgnoreCase("BLOCK_NOTE_CHIME")) {
//			return 8;
//		} else if (sound.equalsIgnoreCase("BLOCK_NOTE_XYLOPHONE")) {
//			return 9;
//		} else {
//			return -1;
//		}
//		
//	}
	
	public static int getSoundID(VersionedSound sound) {
		return SOUNDS.entrySet().stream().filter(en -> en.getValue().equals(sound)).map(en -> en.getKey()).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid sound"));
	}
	
	public static String getL() {
//		try {
//			byte[] lc = new byte[1024];
//			InputStream in = Main.pl.getResource("lic.uid");
//			if(in==null) return null;
//			in.read(lc);
//			in.close();
//			String lc2 = new String(lc);
//			return lc2;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}
		return null;
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

	@Deprecated
	public static float getPitch(int id) {
		switch (id) {
		case 0:
			return 0.5F;
		case 1:
			return 0.53F;
		case 2:
			return 0.56F;
		case 3:
			return 0.6F;
		case 4:
			return 0.63F;
		case 5:
			return 0.67F;
		case 6:
			return 0.7F;
		case 7:
			return 0.76F;
		case 8:
			return 0.8F;
		case 9:
			return 0.84F;
		case 10:
			return 0.9F;
		case 11:
			return 0.94F;
		case 12:
			return 1.0F;
		case 13:
			return 1.06F;
		case 14:
			return 1.12F;
		case 15:
			return 1.18F;
		case 16:
			return 1.26F;
		case 17:
			return 1.34F;
		case 18:
			return 1.42F;
		case 19:
			return 1.5F;
		case 20:
			return 1.6F;
		case 21:
			return 1.68F;
		case 22:
			return 1.78F;
		case 23:
			return 1.88F;
		case 24:
			return 2.0F;
		default:
			return 0.0F;
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
			conn.disconnect();
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
