package me.mrletsplay.playerradios;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.RadioStations;

/**
 * Note: This API is far from finished, it's just a BETA.
 * You can use other classes from this plugin as well, but they aren't documented yet.
 * This API will be expanded in the future to allow for more functionality
 * @author MrLetsplay
 *
 */
public class PlayerRadiosAPI {

	/**
	 * Creates a radio station and automatically adds it to the station GUI
	 * @param owner The owner of the station, use null for none
	 * @param name The name of the station, use null for none
	 * @return The station that was created
	 */
	public static RadioStation createStation(Player owner, String name) {
		return StationManager.createStation(owner, name);
	}
	
	
	/**
	 * Deletes a radio station
	 * @param id The radio station's id
	 */
	public static void deleteRadioStation(int id) {
		StationManager.deleteRadioStation(id);
	}
	
	/**
	 * Gets a radio station by its id
	 * @param id The radio station's id
	 * @return The radio station with that id
	 */
	public static RadioStation getStationByID(int id) {
		return StationManager.getRadioStation(id);
	}
	
	/**
	 * Gets all radio stations with a specific name
	 * @param name The radio station's name
	 * @return The radio stations with that name
	 */
	public static List<RadioStation> getStationsByName(String name, boolean ignoreCase){
		return RadioStations.stations.stream().filter(s -> ignoreCase?s.getName().equalsIgnoreCase(name):s.getName().equals(name)).collect(Collectors.toList());
	}
	
	/**
	 * Gets all radio stations with a specific owner
	 * @return All radio stations of that player
	 */
	public static List<RadioStation> getStationsByOwner(OfflinePlayer owner){
		return StationManager.getRadioStationsByPlayer(owner);
	}
	
	/**
	 * Gets all radio stations with a specific owner
	 * Using this function is not recommended. Use getStationsByOwner() instead
	 * @param ownerName The name of the owner
	 * @return All radio stations of that player
	 */
	@Deprecated
	public static List<RadioStation> getStationsByOwnerName(String ownerName){
		return StationManager.getRadioStationsByPlayerName(ownerName);
	}
	
	/**
	 * Gets the radio station a player is listening to
	 * @param p The player
	 * @return The radio station the player is listening to, or null if none
	 */
	public static RadioStation getRadioStationListening(Player p) {
		return RadioStations.getRadioStationListening(p);
	}
	
	/**
	 * Gets the radio station list GUI
	 * @param page The page of the GUI
	 * @param owner A specific owner to get. null (PlayerRadiosAPI.Constants.GUI_STATION_LIST_ALL.getValue()) for all
	 * @param p The player as who to get the GUI, null for none
	 * @return The specified page of the GUI, or none if that page doesn't exist
	 */
	public static Inventory getStationListGUI(int page, String owner, Player p) {
		return GUIs.getStationsGUI(p, owner, page);
	}
	
	/**
	 * Gets a station's settings GUI
	 * @param id The station's id
	 * @param pIndex The index of the playlist, -1 for default
	 * @param p The player as who to get the GUI, null for none
	 * @return The station settings GUI for that station
	 */
	public static Inventory getStationSettingsGUI(int id, int pIndex, Player p) {
		return GUIs.getStationGUI(p, id, pIndex);
	}
	
	/**
	 * @deprecated Use {@link #getSongGUI(Player, int, int, int, String)} instead
	 */
	@Deprecated
	public static Inventory getSongGUI(int page, int id, int index, String sortBy) {
		return GUIs.getSongGUI(null, page, id, index, sortBy);
	}
	
	/**
	 * Gets the song selection GUI
	 * @param p The player to get the GUI for
	 * @param page The page of the GUI
	 * @param id The station's id for which the song is selected
	 * @param index The index at which the song should be added to the playlist
	 * @param sortBy The sorting mode to use. (See "PlayerRadiosAPI.Constants" class for modes)
	 * @return The song GUI at that specific page, or null if that page doesn't exist
	 */
	public static Inventory getSongGUI(Player p, int page, int id, int index, String sortBy) {
		return GUIs.getSongGUI(p, page, id, index, sortBy);
	}
	
	/**
	 * @deprecated Use {@link #getStationChangeIconInv(Player, int)} instead
	 */
	@Deprecated
	public static Inventory getStationChangeIconInv(int id) {
		return GUIs.getStationIconGUI(null, id);
	}
	
	/**
	 * Gets the icon change GUI for a station
	 * @param p The player to get the GUI for
	 * @param id The radio station's id
	 * @return The change icon GUI for that station
	 */
	public static Inventory getStationChangeIconInv(Player p, int id) {
		return GUIs.getStationIconGUI(p, id);
	}
	
	/**
	 * Forces a (song) tick on all stations
	 */
	public static void forceStationTick() {
		RadioStations.tick();
	}
	
	/**
	 * Plays a radio station to a player
	 * @param p The player to play the station to
	 * @param r The station to play
	 * @return The radio station previously playing or null if none was playing
	 */
	public static RadioStation play(Player p, RadioStation station) {
		RadioStation pl = RadioStations.getRadioStationListening(p);
		StationManager.stop(p);
		station.addPlayerListening(p);
		return pl;
	}
	
	/**
	 * Stops the playback of a radio station to a player
	 * @param player The player to stop playing to
	 * @return The station previously playing or null if none was playing
	 */
	public static RadioStation stop(Player p) {
		RadioStation pl = RadioStations.getRadioStationListening(p);
		StationManager.stop(p);
		return pl;
	}
	
	public static enum Constants{
		
		GUI_STATION_LIST_ALL(null),
		GUI_SONG_LIST_SORTBY_ALPHABET("alphabet"),
		GUI_SONG_LIST_SORTBY_ID("id"),
		GUI_SONG_LIST_SORTBY_AUTHOR("author");
		
		private String val;
		
		private Constants(String val) {
			this.val = val;
		}
		
		public String getValue() {
			return val;
		}
		
	}
	
}
