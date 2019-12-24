package me.mrletsplay.playerradios;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.CustomConfig;
import me.mrletsplay.mrcore.config.FileCustomConfig;

public class Config {
	
	public static FileCustomConfig config = ConfigLoader.loadFileConfig(new File(Main.pl.getDataFolder(), "config.yml"));
	public static CustomConfig messages;
	
	public static boolean enable_update_check, update_check_on_join, update_check_on_command, use_fixed_playback, 
		use_alternate_nbs_import, detect_original_author, fix_song_ids, allow_color, color_needs_permission, autodelete,
		use_uuids = Bukkit.getServer().getOnlineMode(), allow_create_stations, enable_particles, 
		show_automatically_named, thread_on_process, enable_submit, submit_needs_perm, remove_icon_items, auto_close_song_gui,
		world_list_blacklist, disable_commands, disable_commands_all, songsettings_auto, save_last_listened, allow_station_name_edit;
	
	public static int max_station_name_length, max_stations_per_player, sleep_ticks, result_amount, max_type_time, 
		thread_on_process_when, default_station, submit_max_file_size, gui_refresh_interval_ms, autodelete_days;
	
	public static String inventory_name, default_author, language;
	public static List<String> true_keywords, world_list;
	
	private static final String PERM_BASE = "playerradios.";
	
	public static final String PERM_NOTIFY_UPDATE = PERM_BASE+"notify-update",
							   PERM_CREATE_WHEN_DISABLED = PERM_BASE+"create-when-disabled",
							   PERM_EDIT_OTHER = PERM_BASE+"edit-other",
							   PERM_EXPORT = PERM_BASE+"export",
							   PERM_EXPORT_ALL = PERM_BASE+"export.all",
							   PERM_RELOAD = PERM_BASE+"reload",
							   PERM_SUBMIT = PERM_BASE+"submit",
							   PERM_SUBMIT_WHEN_DISABLED = PERM_BASE+"submit-when-disabled",
							   PERM_ALLOW_COLOR = PERM_BASE+"color",
							   PERM_ALLOW_BUGREPORT = PERM_BASE+"allow-bugreport",
							   PERM_RENAME_WHEN_DISABLED = PERM_BASE+"rename-when-disabled";
	
	public static void save(){
		config.saveToFile();
	}
	
	public static void init(){
		config.setHeader(" Plugin version: "+Main.pluginVersion+"\n");
		config.setComment("use-uuids", " Should always be enabled (will automatically be disabled if server is in offline/cracked mode)");
		config.setComment("sleep-ticks", " Determines the amount of ticks to wait after song end. If you don't know what to set this to, just leave it as is");
		config.setComment("show-automatically-named", " If enabled, songs that are automatically named by file name (because they don't contain a title in the NBS file) will have the suffix \"(Automatically named)\"");
		config.setComment("use-alternate-nbs-import", " Disable this, if you think that the imported NBS songs sound weird. I added this because a lot of notes failed to import correctly because of a too high/low pitch. This alters the pitch so it's in a vaild range");
		config.setComment("detect-original-author", " If set to true, the plugin will try to detect an author for songs that don't have one set");
		config.setComment("default-original-author", " The default author to use when no author was found");
		config.setComment("use-thread-on-process", " Whether to use a thread when importing/exporting/loading songs. The \"use-thread-on-process\" option defines starting at how many songs a thread should be used");
		config.setComment("use-fixed-playback-system", " Fixes the playback of songs with some tempos + Will increase song performance (Songs will be run in external thread = Songs will not be influenced by server lag). Disable this if you're having issues");
		config.setComment("default-station", " The default station that should play automatically when joining on the server. Set to -1 for none");
		config.setComment("language", " The language (file) to be used by the plugin. Will generate a new file if it doesn't exist");
		config.setComment("true-keywords", " Alternative keywords for \"true\" to be accepted by the plugin");
		config.setComment("fix-song-ids", " Enable this and reload to fix all song ids (Disable it again afterwards, slows down the plugin). Recommended when updating from a version older than 2.4");
		config.setComment("submit.enable", " When enabled, users can submit their own songs via \"/pr submit <Link>\". If \"needs-permission\" is set to true, users need the permission \""+PERM_SUBMIT+"\" to do so");
		config.setComment("remove-icon-items", " If enabled, players will not get the item they set as station icon returned");
		config.setComment("convert-uuids", " Automatically try to convert uuids when use-uuids is switched. Disable this if you're having issues");
		config.setComment("auto-close-song-gui", " If enabled, PlayerRadios will automatically close the song add gui when you've added a song");
		config.setComment("disable-commands.do", " If enabled, players won't be able to use /pr station <...> commands");
		config.setComment("disable-commands.all", " This will disable all commands (including /pr <search/playlist/...>) except for admin commands (/pr <reload/version/...>) and /pr help");
		config.setComment("autodelete", " If enabled, stations will automatically get deleted after the user has been offline/inactive for a certain amount of time (You can enable \"dontautodelete\" in the stations file for a station to not be auto-deleted)");
		
		enable_update_check = config.getBoolean("update-check.enable", true, true);
		update_check_on_join = config.getBoolean("update-check.on-join", true, true);
		update_check_on_command = config.getBoolean("update-check.on-command", true, true);
		max_station_name_length = config.getInt("max-station-name-length", 32, true);
		max_stations_per_player = config.getInt("max-stations-per-player", 5, true);
		use_uuids = use_uuids && config.getBoolean("use-uuids", true, true);
		allow_create_stations = config.getBoolean("enable-user-stations", true, true);
		enable_particles = config.getBoolean("enable-particles", true, true);
		use_fixed_playback = config.getBoolean("use-fixed-playback-system", true, true);
		sleep_ticks = config.getInt("sleep-ticks", 50, true);
		show_automatically_named = config.getBoolean("show-automatically-named", false, true);
		result_amount = config.getInt("result-amount", 5, true);
		use_alternate_nbs_import = config.getBoolean("use-alternate-nbs-import", true, true);
		max_type_time = config.getInt("max-type-time-seconds", 10, true);
		detect_original_author = config.getBoolean("detect-original-author", true, true);
		default_author = config.getString("default-original-author", "Unknown", true);
		thread_on_process = config.getBoolean("use-thread-on-process.enable", true, true);
		thread_on_process_when = config.getInt("use-thread-on-process.when", 1000, true);
		default_station = config.getInt("default-station", -1, true);
		language = config.getString("language", "en", true);
		true_keywords = config.getStringList("true-keywords", Arrays.asList("yes", "enable", "on"), true);
		fix_song_ids = config.getBoolean("fix-song-ids", false, true);
		enable_submit = config.getBoolean("submit.enable", true, true);
		submit_needs_perm = config.getBoolean("submit.needs-permission", true, true);
		submit_max_file_size = config.getInt("submit.max-file-size-kb", 2048, true);
		remove_icon_items = config.getBoolean("remove-icon-items", true, true);
		allow_color = config.getBoolean("color.enable", true, true);
		color_needs_permission = config.getBoolean("color.needs-permission", true, true);
		gui_refresh_interval_ms = config.getInt("gui-refresh-interval-ms", 1000, true);
		auto_close_song_gui = config.getBoolean("auto-close-song-gui", false, true);
		world_list = config.getStringList("world-list.list", Arrays.asList("aBlacklistedWorld","anotherBlacklistedWorld"), true);
		world_list_blacklist = config.getBoolean("world-list.blacklist-mode", true, true);
		disable_commands = config.getBoolean("disable-commands.do", true, true);
		disable_commands_all = config.getBoolean("disable-commands.all", false, true);
		autodelete = config.getBoolean("autodelete.enable", true, true);
		autodelete_days = config.getInt("autodelete.time-in-days", 7, true);
		songsettings_auto = config.getBoolean("song-settings.auto-add-new-songs", false, true);
		save_last_listened = config.getBoolean("save-last-listened", true, true);
		config.addDefault("prefix", "§8[§6PlayerRadios§8]");
		config.addDefault("inventory-name", "%prefix%");
		config.addDefault("default-station-name", "Default station");
		allow_station_name_edit = config.getBoolean("allow-station-name-edit", true, true);
		
		config.applyDefaults();
		config.saveToFile();
		save();
		
		inventory_name = getAndTranslate("config", "inventory-name");
		
		importLangFile("de");
		importLangFile("ru");
		
		File langFile = new File(Main.pl.getDataFolder(), "/lang/"+language+".yml");
		messages = getMessageConfig(langFile);
	}
	
	private static void importLangFile(String lang) {
		if(!new File(Main.pl.getDataFolder(), "/lang/"+lang+".yml").exists()) {
			Main.pl.saveResource("lang/"+lang+".yml", false);
		}
	}
	
	private static CustomConfig getMessageConfig(File f) {
		FileCustomConfig c = ConfigLoader.loadFileConfig(f);
		c.addDefault("creation-disabled", "%prefix% §cThe creation of stations is currently disabled");
		c.addDefault("time-format", "s,min,h");
		c.addDefault("still-loading", "%prefix% §cPlayerRadios is still loading");
		c.addDefault("stations", "%prefix% §7Your stations");
		c.addDefault("stations-entry", "§8%station-id%: §7%station-name%");
		c.addDefault("stations-empty", "%prefix% §7You don't own any stations");
		c.addDefault("station.too-many-stations", "%prefix% §cYou cannot have more than %max-stations% stations");
		c.addDefault("station-created.1", "%prefix% §aYour station §7\"%station-name%\" §a(Station §7%id%§a) has been created");
		c.addDefault("station-created.2", "%prefix% §6Use §7/pr station %id% §6to manage your station");
		c.addDefault("station.playlist", "%prefix% §7Station \"%station-name%\" (%station-id%)'s Playlist (Looping: %loop%):");
		c.addDefault("station.playlist-entry", "§6%index%: §7%song-name% (Song ID: %song-id%)");
		c.addDefault("station.playlist-entry-playing", "§6%index%: §a%song-name% (Song ID: %song-id%)");
		c.addDefault("station.playlist-empty", "%prefix% §7Station \"%station-name%\" (%station-id%)'s playlist is empty");
		c.addDefault("station.song-added", "%prefix% §aSong added to playlist");
		c.addDefault("station.song-doesnt-exist", "%prefix% §cThat song doesn't exist");
		c.addDefault("station.song-removed", "%prefix% §aSong removed from playlist");
		c.addDefault("station.song-not-on-playlist", "%prefix% §cThat song is not on the playlist");
		c.addDefault("station.not-your-station", "%prefix% §cYou don't own that station");
		c.addDefault("station.doesnt-exist", "%prefix% §cThat station doesn't exist");
		c.addDefault("station.started", "%prefix% §aStation started");
		c.addDefault("station.stopped", "%prefix% §cStation stopped");
		c.addDefault("station.cannot-modify", "%prefix% §cYou cannot modify the station's playlist while it's running");
		c.addDefault("station.name-too-long", "%prefix% §cThe station name cannot be longer than %max-name-length% characters");
		c.addDefault("station.rename", "%prefix% §7Please type in a new name for your station:");
		c.addDefault("station.rename-not-allowed", "%prefix% §cRenaming stations is not allowed");
		c.addDefault("station.rename-cancelled", "%prefix% §cRename action timed out. Try typing a little faster next time");
		c.addDefault("station.set.name", "%prefix% §aYour station has been renamed from §7%old-name% §ato §7%new-name%");
		c.addDefault("station.set.loop.enable", "%prefix% §aLooping enabled");
		c.addDefault("station.set.loop.disable", "%prefix% §cLooping disabled");
		c.addDefault("station-changed", "%prefix% §aNow listening to: §7%station-name%");
		c.addDefault("station.track-skipped", "%prefix% §aTrack skipped");
		c.addDefault("station.already-running", "%prefix% §cYour station is already running");
		c.addDefault("station.not-running", "%prefix% §cYour station is currently not running");
		c.addDefault("station.deleted", "%prefix% §aThe station §7%station-name% (%station-id%) §ahas been deleted");
		c.addDefault("station.gui.about.info", "§eInfo");
		c.addDefault("station.gui.about.status", "§7Your station is currently: ");
		c.addDefault("station.gui.about.status-online", "§aOnline");
		c.addDefault("station.gui.about.status-offline", "§cOffline");
		c.addDefault("station.gui.about.playing", "§7Currently playing: %song-name%");
		c.addDefault("station.gui.about.playing-color", "§3");
		c.addDefault("station.gui.about.playing-nothing", "Nothing");
		c.addDefault("station.gui.about.players", "§7There are §3%players% §7players listening to your station");
		c.addDefault("station.gui.about.players-no", "no");
		c.addDefault("station.gui.about.songs", "§7The playlist contains §3%songs% §7songs");
		c.addDefault("station.gui.about.songs-no", "no");
		c.addDefault("station.gui.about.listen", "§3Left click to listen to station");
		c.addDefault("station.gui.about.stop-listening", "§3Click to stop listening");
		c.addDefault("station.gui.about.change-icon", "§3Right click to change icon");
		c.addDefault("station.gui.loop.enabled", "§aLooping enabled");
		c.addDefault("station.gui.loop.disabled", "§cLooping disabled");
		c.addDefault("station.gui.skip-song", "§7Skip current song");
		c.addDefault("station.gui.start", "§aStart your station");
		c.addDefault("station.gui.stop", "§cStop your station");
		c.addDefault("station.gui.playlist-empty", "§cYour station's playlist is empty");
		c.addDefault("station.gui.rename", "§7Rename your station");
		c.addDefault("station.gui.song.add", "§7Add song to playlist");
		c.addDefault("station.gui.song.remove", "§7Remove song from playlist");
		c.addDefault("station.gui.song.previous", "§7Previous song");
		c.addDefault("station.gui.song.next", "§7Next song");
		c.addDefault("station.gui.song.current", "§7Currently selected");
		c.addDefault("station.gui.song.currently-playing", "§aThis song is currently playing");
		c.addDefault("station.gui.delete", "§cDelete station");
		c.addDefault("station.gui.running", "§cYou cannot do that while your station is running");
		c.addDefault("station.gui.not-running", "§cYour station is currently not running");
		c.addDefault("station.gui.drop-here", "§7Drop your item here");
		c.addDefault("station.gui.play-mode.message", "§7Playing %mode% §7given songs");
		c.addDefault("station.gui.play-mode.whitelist", "§8all of");
		c.addDefault("station.gui.play-mode.blacklist", "§8all except");
		c.addDefault("gui.back", "§cBack");
		c.addDefault("gui.next-page", "§7Next page");
		c.addDefault("gui.previous-page", "§7Previous page");
		c.addDefault("gui.create-station", "§aCreate station");
		c.addDefault("gui.all-stations", "§7All stations");
		c.addDefault("gui.your-stations", "§7Your stations");
		c.addDefault("gui.stop-listening", "§cStop listening");
		c.addDefault("gui.about-station.online", "§aCurrently broadcasting");
		c.addDefault("gui.about-station.offline", "§cCurrently offline");
		c.addDefault("gui.about-station.playing", "§7Currently playing: %song-name%");
		c.addDefault("gui.about-station.playing-color", "§3");
		c.addDefault("gui.about-station.playing-nothing", "Nothing");
		c.addDefault("gui.about-station.listen-time", "§7Listening for: §3%time%");
		c.addDefault("gui.about-station.owner", "§7Owner: §3%owner%");
		c.addDefault("gui.about-station.id", "§7ID: §3%id%");
		c.addDefault("gui.about-station.manage", "§3Right-click to manage");
		c.addDefault("gui.songs.sorting-by", "§7Sorting by: %mode%");
		c.addDefault("gui.songs.mode-alphabet", "Alphabet");
		c.addDefault("gui.songs.mode-id", "ID");
		c.addDefault("gui.songs.mode-author", "Author");
		c.addDefault("gui.songs.song.author", "§8Original author: §7%author%");
		c.addDefault("gui.songs.song.by", "§8By: §7%maker%");
		c.addDefault("gui.songs.song.id", "§8Song ID: §7%id%");
		c.addDefault("song-changed", "%prefix% §8[§7%station-name%§8]: §aNow playing: §7%song-name%");
		c.addDefault("station-broadcast", "%prefix% §8[§7%station-name%§8]: §7%message%");
		c.addDefault("stopped-listening", "%prefix% §cStopped listening");
		c.addDefault("not-listening", "%prefix% §cYou aren't listening to any stations");
		c.addDefault("stopped-broadcasting", "%prefix% §cThe station §7%station-name% §chas stopped broadcasting");
		c.addDefault("not-broadcasting", "%prefix% §cThe station §7%station-name% §cis currently not broadcasting");
		c.addDefault("search-results", "%prefix% §7Top search results:");
		c.addDefault("search-results-entry", "§8%song-id%: §7%song-author% - %song-name% (by: %song-by%)");
		c.addDefault("search-results-empty", "§cNo results found");
		c.addDefault("export.wait", "%prefix% §6Exporting §7%song-name%§6...");
		c.addDefault("export.done", "%prefix% §aExported as §7%file-name%");
		c.addDefault("export.failed", "%prefix% §cExport failed. Check console for more info");
		c.addDefault("export.wait-all", "%prefix% §6Exporting §7%count% §6song(s)...");
		c.addDefault("export.done-all", "%prefix% §aExported §7%count% §asong(s) in §7%time%s §a(§7%failed% §afailed)");
		c.addDefault("export.error-line", "%prefix% §cError while exporting song §7%song%§c: §r%error%");
		c.addDefault("export.error-line-all", "%prefix% §cError while exporting songs: §r%error%");
		c.addDefault("export.not-available", "%prefix% §6Exporting as an archive is not available for single songs");
		c.addDefault("export.invalid-song", "%prefix% §cThat song doesn't exist");
		c.addDefault("reload-complete", "%prefix% §aReload complete");
		c.addDefault("submit.invalid-url", "%prefix% §cInvalid url");
		c.addDefault("submit.file-too-big", "%prefix% §cFile too big");
		c.addDefault("submit.invalid-file", "%prefix% §cThe file you're trying to import is not a valid song file (NBS, SNG, RSNG)");
		c.addDefault("submit.downloading", "%prefix% §6Downloading file...");
		c.addDefault("submit.reading", "%prefix% §6Reading song(s) from file...");
		c.addDefault("submit.success", "%prefix% §aImported §7%count% §asong(s) successfully (Detected format §7%format%§a)");
		c.addDefault("bugreport.success", "%prefix% §aBug report created: §7%link%§a. Send me a private message at §7https://www.spigotmc.org/conversations/add?to=MrLetsplay §awith that link and the bug you found");
		c.addDefault("process-already-running", "§cThere's already a process running. Please wait until it's finished");
		c.addDefault("world-blacklisted", "%prefix% §cPlayerRadios is disabled in this world");
		c.addDefault("commands-disabled", "%prefix% §cPlayerRadios commands are disabled. Please run /pr to and use the GUI instead");
//		c.addDefault("songinfo.song-doesnt-exist", "%prefix% §cThat song doesn't exist");
//		c.addDefault("songinfo.", "%prefix% §cThat song doesn't exist");
		
		c.addDefault("placeholderapi.listening.station.none", "None");
		c.addDefault("placeholderapi.listening.station.owner.none", "-");
		c.addDefault("placeholderapi.listening.time.none", "-");
		c.addDefault("placeholderapi.listening.song.none", "None");
		c.addDefault("placeholderapi.listening.song.format", "%original-author% - %song-name% (by %author%) | %song-id%");
		
		c.addDefault("help.header", "%prefix% §8| §7Help §8| §7%topic%");
		c.addDefault("help.layout", "§7§l/%command%%args% §r§8- %description%");
		c.addDefault("help.use-topic-stations", "§7Use §7§l/pr help stations §r§7for help about stations");
		c.addDefault("help.invalid-topic", "§7There's no help available for \"%topic%\"");
		c.addDefault("help.pr", "Opens the station selection GUI");
		c.addDefault("help.pr-help", "Shows the command help (about a specific topic)");
		c.addDefault("help.pr-playlist", "Shows the playlist of the station you're currently listening to");
		c.addDefault("help.pr-version", "Shows the current PlayerRadios version and checks for an update (if enabled)");
		c.addDefault("help.pr-export", "Exports a song to the specified format/Exports the song settings");
		c.addDefault("help.pr-reload", "Reloads PlayerRadios");
		c.addDefault("help.pr-submit", "Submit/Upload a song to the server's song library");
		c.addDefault("help.pr-search", "Search for a song");
		c.addDefault("help.pr-bugreport", "Create a bug report");
		c.addDefault("help.pr-station-create", "Create a station");
		c.addDefault("help.pr-station-list", "Lists all your stations");
		c.addDefault("help.pr-station", "Opens the station's management GUI");
		c.addDefault("help.pr-station-playlist", "Shows a station's playlist");
		c.addDefault("help.pr-station-playlist-add", "Add a song to a station's playlist");
		c.addDefault("help.pr-station-playlist-remove", "Remove a song from a station's playlist");
		c.addDefault("help.pr-station-set", "Change a station's setting");
		c.addDefault("help.pr-station-start-stop", "Start/Stop a station");
		c.addDefault("help.pr-station-skip", "Skip to the next track in a station's playlist");
		c.addDefault("help.pr-station-delete", "Delete a station");
		
		c.applyDefaults();
		c.saveToFile();
		return c;
	}
	
	public static String getAndTranslate(String file, String path) {
		String msg = null;
		if(file.equalsIgnoreCase("messages")) {
			msg = messages.getString(path);
		}else if(file.equalsIgnoreCase("config")) {
			msg = config.getString(path);
		}
		if(msg==null) return "missing: "+file+"/"+path;
		return ChatColor.translateAlternateColorCodes('&', msg
				.replace("%max-name-length%", max_station_name_length+"")
				.replace("%max-stations%", max_stations_per_player+"")
				.replace("%prefix%", config.getString("prefix")));
	}
	
	public static String getMessage(String msg) {
		return getAndTranslate("messages", msg);
	}
	
	public static String getMessage(String msg, String... params) {
		if(params.length%2!=0) return null;
		String msg2 = getAndTranslate("messages", msg);
		for(int i = 0; i < params.length; i+=2) {
			msg2 = msg2.replace("%"+params[i]+"%", params[i+1]);
		}
		return msg2;
	}
	
	public static String getHelpMessage(String cmd, String label, String args, String... params) {
		String s = getMessage("help.layout")
				.replace("%description%", getMessage("help."+cmd))
				.replace("%command%", label)
				.replace("%args%", args);
		if(params.length%2 != 0) return s;
		for(int i = 0; i < params.length; i+=2) {
			s.replace("%"+params[i]+"%", params[i+1]);
		}
		return s;
	}
	
}
