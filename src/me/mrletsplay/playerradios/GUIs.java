package me.mrletsplay.playerradios;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUI;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIBuildEvent;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIBuildPageItemEvent;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIBuilder;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIBuilderMultiPage;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIElement;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIElementAction;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIElementActionEvent;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.GUIMultiPage;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.ItemSupplier;
import me.mrletsplay.mrcore.bukkitimpl.GUIUtils.StaticGUIElement;
import me.mrletsplay.mrcore.bukkitimpl.ItemUtils;
import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedDyeColor;
import me.mrletsplay.mrcore.bukkitimpl.versioned.VersionedMaterial;
import me.mrletsplay.mrcore.misc.StringUtils;
import me.mrletsplay.playerradios.util.RadioStation;
import me.mrletsplay.playerradios.util.RadioStations;
import me.mrletsplay.playerradios.util.Song;
import me.mrletsplay.playerradios.util.SongManager;
import me.mrletsplay.playerradios.util.Tools;

public class GUIs {
	
	public static final GUIMultiPage<RadioStation> STATIONS_GUI;

	public static final GUIMultiPage<Integer> STATION_GUI;
	public static final GUIMultiPage<Song> SONGS_GUI;
	public static final GUI STATION_ICON_GUI;
	
	static {
		STATIONS_GUI = buildStationsGUI();
		STATION_GUI = buildStationGUI();
		SONGS_GUI = buildSongGUI();
		STATION_ICON_GUI = buildStationIconGUI();
	}
	
	public static Inventory getStationsGUI(Player p, String owner) {
		return getStationsGUI(p, owner, 0);
	}
	
	public static Inventory getStationsGUI(Player p, String owner, int page) {
		Map<String, Object> map = new HashMap<>();
		map.put("owner", owner);
		return STATIONS_GUI.getForPlayer(p, page, Main.pl, map);
	}
	
	public static Inventory getStationGUI(Player p, int rID, int idx) {
		Map<String, Object> map = new HashMap<>();
		map.put("station_id", rID);
		map.put("index", idx);
		return STATION_GUI.getForPlayer(p, Main.pl, map);
	}
	
	public static Inventory getSongGUI(Player p, int page, int rID, int idx, String sortingMode) {
		Map<String, Object> map = new HashMap<>();
		map.put("station_id", rID);
		map.put("index", idx);
		map.put("sort_by", sortingMode);
		return SONGS_GUI.getForPlayer(p, Main.pl, map);
	}
	
	public static Inventory getStationIconGUI(Player p, int rID) {
		Map<String, Object> map = new HashMap<>();
		map.put("station_id", rID);
		return STATION_ICON_GUI.getForPlayer(p, Main.pl, map);
	}
	
	private static GUIMultiPage<RadioStation> buildStationsGUI() {
		GUIBuilderMultiPage<RadioStation> builder = new GUIBuilderMultiPage<>(Config.inventory_name, 6);
		builder.setSupplier(new ItemSupplier<RadioStation>() {
			
			@Override
			public GUIElement toGUIElement(GUIBuildPageItemEvent<RadioStation> event, RadioStation it) {
				Player p = event.getPlayer();
				List<String> lore = new ArrayList<>();
				lore.add((it.isRunning()?Config.getMessage("gui.about-station.online"):Config.getMessage("gui.about-station.offline")));
				List<String> spl = StringUtils.wrapString(Config.getMessage("gui.about-station.playing").replace("%song-name%", Config.getMessage("gui.about-station.playing-color")+(it.getCurrentSong()!=null?it.getCurrentSong().getName():Config.getMessage("gui.about-station.playing-nothing"))), 50);
				for(String s : spl) {
					lore.add(Config.getMessage("gui.about-station.playing-color")+s);
				}
				RadioStation r = RadioStations.getRadioStationListening(p);
				if(r!=null && r.equals(it)) lore.add(Config.getMessage("gui.about-station.listen-time", "time", Tools.formatTime(r.getListenMs(p))));
				lore.add(Config.getMessage("gui.about-station.owner").replace("%owner%", it.getOwnerName()));
				lore.add(Config.getMessage("gui.about-station.id").replace("%id%", ""+it.getID()));
				if(p!=null && it.isOwner(p)) lore.add(Config.getMessage("gui.about-station.manage"));
				GUIElement el = new StaticGUIElement(ItemUtils.createItem(it.getIcon(), "§7Station | "+it.getName(), lore.toArray(new String[lore.size()]))).setAction(new GUIElementAction() {
					
					@Override
					public void onAction(GUIElementActionEvent event) {
						Player p = event.getPlayer();
						if(event.getClickType().equals(ClickType.LEFT)) {
							if(it.isRunning()) {
								StationManager.stop(p);
								it.addPlayerListening(p);
								p.sendMessage(Config.getMessage("station-changed").replace("%station-name%", it.getName()));
							}else{
								p.sendMessage(Config.getMessage("not-broadcasting").replace("%station-name%", it.getName()));
							}
						}else if(event.getClickType().equals(ClickType.RIGHT)) {
							if(it.isOwner(p)) {
								Inventory inv = getStationGUI(p, it.getID(), 0);
								p.openInventory(inv);
							}else {
								p.sendMessage(Config.getMessage("station.not-your-station"));
							}
						}
					}
				});
				return el;
			}
			
			@Override
			public List<RadioStation> getItems(GUIBuildEvent event) {
				String owner = (String) event.getGUIHolder().getProperty(Main.pl, "owner");
				if(owner == null){
					return RadioStations.stations;
				}else{
					return StationManager.getRadioStationsByPlayerName(owner);
				}
			}
		});
		
		GUIElement gPane = new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.BLACK_STAINED_GLASS_PANE, 1, "§0"));
		for(int i = 5*9; i < 6*9; i++) {
			builder.addElement(i, gPane);
		}
		
		builder.addElement(45, new StaticGUIElement(ItemUtils.createItem(Material.BARRIER, 1, 0, Config.getMessage("gui.stop-listening"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				Player p = event.getPlayer();
				if(StationManager.stop(p)) {
					p.sendMessage(Config.getMessage("stopped-listening"));
				}else {
					p.sendMessage(Config.getMessage("not-listening"));
				}
			}
		}));
		
		builder.addElement(49, new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.GREEN_STAINED_CLAY, 1, Config.getMessage("gui.create-station"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				Player p = event.getPlayer();
				if(Config.allow_create_stations || p.hasPermission(Config.PERM_CREATE_WHEN_DISABLED)) {
					String sName = Config.getAndTranslate("config","default-station-name").replace("%player%", p.getName());
					if(StationManager.getRadioStationsByPlayer(p).size()<Config.max_stations_per_player) {
						RadioStation r = StationManager.createStation(p, sName);
						p.openInventory(getStationGUI(p, r.getID(), 0));
					}else {
						p.sendMessage(Config.getMessage("station.too-many-stations"));
					}
				}else {
					p.sendMessage(Config.getMessage("creation-disabled"));
				}
			}
		}));
		
		builder.addElement(50, new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.GREEN_BANNER, 1, Config.getMessage("gui.all-stations"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				event.getGUIHolder().setProperty(Main.pl, "owner", null);
				event.refreshInstance();
			}
		}));
		
		builder.addElement(51, new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.ORANGE_BANNER, 1, Config.getMessage("gui.your-stations"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				event.getGUIHolder().setProperty(Main.pl, "owner", event.getPlayer().getName());
				event.refreshInstance();
			}
		}));
		
		builder.addPreviousPageItem(52, ItemUtils.createItem(ItemUtils.arrowLeft(), Config.getMessage("gui.previous-page")));
		builder.addPreviousPageItem(53, ItemUtils.createItem(ItemUtils.arrowRight(), Config.getMessage("gui.next-page")));
		
		builder.addPageSlotsInRange(0, 44);
		return builder.build();
	}
	
	private static GUIMultiPage<Integer> buildStationGUI() {
		GUIBuilderMultiPage<Integer> builder = new GUIBuilderMultiPage<>(Config.inventory_name, 6);
		builder.setSupplier(new ItemSupplier<Integer>() {
			
			@Override
			public GUIElement toGUIElement(GUIBuildPageItemEvent<Integer> event1, Integer item) {
				if(item == null) return new StaticGUIElement(new ItemStack(Material.AIR));
				return new GUIElement() {
					
					@Override
					public ItemStack getItem(GUIBuildEvent event) {
						int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
						int idx = (int) event.getGUIHolder().getProperty(Main.pl, "index");
						RadioStation r = StationManager.getRadioStation(id);
						if(r.isRunning()) idx = r.getCurrentIndex();
						if(idx == -1) idx = 0;
						
//						int aIdx = event1.getRelativeIndex() + idx;
						int aIdx = event1.getAbsoluteIndex();
						int sIdx = idx + aIdx - 3;
//						int skip = (int) event1.getItems().stream().limit(aIdx).filter(i -> i == null).count();
						Song s = SongManager.getSongByID(item);
						if(s!=null) {
							List<String> spl2 = StringUtils.wrapString("§7" + sIdx + " | "+s.getName()+" (ID: "+s.getID()+")", 50); // TODO: Idx
							List<String> lore2 = new ArrayList<>();
							if(spl2.size()>1) {
								for(int i1 = 1; i1 < spl2.size(); i1++) {
									lore2.add("§7"+spl2.get(i1));
								}
							}
							if(r.isRunning() && r.getCurrentIndex() != -1 && r.getCurrentIndex() == sIdx) {
								lore2.add(Config.getMessage("station.gui.song.currently-playing"));
							}
							return ItemUtils.createItem(Material.PAPER, 1, 0, spl2.get(0), lore2.toArray(new String[lore2.size()]));
						}else {
							return new ItemStack(Material.AIR);
						}
					}
				};
			}
			
			@Override
			public List<Integer> getItems(GUIBuildEvent event) {
				int idx = (int) event.getGUIHolder().getProperty(Main.pl, "index");
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				if(r.isRunning()) idx = r.getCurrentIndex();
				if(idx == -1) idx = 0;
				
				// Non-existant songs are null
				List<Integer> songs = new ArrayList<>(7);
				int startIdx = idx - 3; // 3 previous songs, so that the current song is in the middle
				int endIdx = Math.min(idx + 4, r.getPlaylist().size()); // 3 next songs
				while(startIdx < 0) {
					songs.add(null);
					startIdx++;
				}
				for(int i = startIdx; i < endIdx; i++) {
					songs.add(r.getPlaylist().get(i));
				}
				while(songs.size() < 7) songs.add(null);
				return songs;
			}
		});
		GUIElement gPane = new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.BLACK_STAINED_GLASS_PANE, 1, "§0"));
		for(int i = 0; i < 6*9; i++) {
			builder.addElement(i, gPane);
		}
		builder.addElement(10, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				Player p = event.getPlayer();
				List<String> lore = new ArrayList<>();
				lore.add(Config.getMessage("station.gui.about.status")+(r.isRunning()?Config.getMessage("station.gui.about.status-online"):Config.getMessage("station.gui.about.status-offline")));
				List<String> spl = StringUtils.wrapString(Config.getMessage("station.gui.about.playing").replace("%song-name%", Config.getMessage("station.gui.about.playing-color")+(r.getCurrentSong()!=null?r.getCurrentSong().getName():Config.getMessage("station.gui.about.playing-nothing"))), 50);
				for(String s : spl) {
					lore.add(Config.getMessage("station.gui.about.playing-color")+s);
				}
				lore.add(Config.getMessage("station.gui.about.players").replace("%players%", ""+(r.getPlayers().isEmpty()?Config.getMessage("station.gui.about.players-no"):r.getPlayers().size())));
				lore.add(Config.getMessage("station.gui.about.songs").replace("%songs%", ""+(r.getPlaylist().isEmpty()?Config.getMessage("station.gui.about.songs-no"):r.getPlaylist().size())));
				if(p!=null) {
					RadioStation r2 = RadioStations.getRadioStationListening(p);
					if(r.isRunning()) {
						lore.add((r2!=null&&r2.getID()==id)?Config.getMessage("station.gui.about.stop-listening"):Config.getMessage("station.gui.about.listen"));
					}
				}
				lore.add(Config.getMessage("station.gui.about.change-icon"));
				return ItemUtils.createItem(r.getIcon(), Config.getMessage("station.gui.about.info"), lore.toArray(new String[lore.size()]));
			}
			
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				Player p = event.getPlayer();
				if(event.getClickType().equals(ClickType.LEFT)) {
					RadioStation r2 = RadioStations.getRadioStationListening(p);
					if(r2!=null) {
						if(r2.getID()!=r.getID()) {
							if(r.isRunning()) {
								StationManager.stop(p);
								r.addPlayerListening(p);
							}
						}else {
							StationManager.stop(p);
						}
					}else {
						if(r.isRunning()) {
							r.addPlayerListening(p);
						}
					}
					event.getGUI().refreshInstance(event.getPlayer());
//					StationManager.updateStationGUI(r.getID(), fPIndex);
				}else if(event.getClickType().equals(ClickType.RIGHT)) {
					p.openInventory(GUIs.getStationIconGUI(p, id));
				}
			}
		}));
		
		builder.addElement(11, new GUIElement() {

			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(ItemUtils.arrowRight(), Config.getMessage("station.gui.skip-song"), (!r.isRunning()?Config.getMessage("station.gui.not-running"):""));
			}
			
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				r.skipTrack();
			}
		}));
		
		builder.addElement(13, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(
						r.isRunning()?VersionedMaterial.RED_STAINED_CLAY:VersionedMaterial.GREEN_STAINED_CLAY,
						1,
						(r.isRunning() ? Config.getMessage("station.gui.stop") : Config.getMessage("station.gui.start")),
						(!r.isRunning() && r.getPlaylist().isEmpty() && !r.isBlacklistMode() ? Config.getMessage("station.gui.playlist-empty") : "")
					);
			}
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				if(!r.isRunning()) {
					if(!r.getPlaylist().isEmpty() || r.isBlacklistMode()) {
						r.start();
					}
				}else {
					r.stop();
				}
			}
		}));
		
		builder.addElement(15, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(
						VersionedMaterial.getBanner(r.isLooping() ? VersionedDyeColor.GREEN : VersionedDyeColor.LIGHT_GRAY),
						1,
						(r.isLooping()?Config.getMessage("station.gui.loop.enabled"):Config.getMessage("station.gui.loop.disabled"))
					);
			}
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				r.setLoop(!r.isLooping());
			}
		}));
		
		builder.addElement(16, new StaticGUIElement(ItemUtils.createItem(Material.NAME_TAG, 1, 0, Config.getMessage("station.gui.rename"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				Player p = event.getPlayer();
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
//				RadioStation r = StationManager.getRadioStation(id);
				if(Config.allow_station_name_edit || p.hasPermission(Config.PERM_RENAME_WHEN_DISABLED)) {
					p.closeInventory();
					p.sendMessage(Config.getMessage("station.rename"));
					BukkitTask tID = null;
					if(Config.max_type_time>0){
						tID = Bukkit.getScheduler().runTaskLater(Main.pl, new CancelTask(p), Config.max_type_time*20);
					}
					Events.typing.put(p, new Integer[] {id, (tID!=null?tID.getTaskId():-1)});
				}else {
					p.sendMessage(Config.getMessage("station.rename-not-allowed"));
				}
			}
		}));
		
		builder.addElement(28, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(Tools.plus(VersionedDyeColor.WHITE), Config.getMessage("station.gui.song.add"), (r.isRunning()?Config.getMessage("station.gui.running"):""));
			}
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				int idx = (int) event.getGUIHolder().getProperty(Main.pl, "index");
				RadioStation r = StationManager.getRadioStation(id);
				if(!r.isRunning()) event.getPlayer().openInventory(GUIs.getSongGUI(event.getPlayer(), 0, r.getID(), idx, "alphabet"));
			}
		}));
		
		builder.addElement(30, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(ItemUtils.arrowLeft(), Config.getMessage("station.gui.song.previous"), (r.isRunning()?Config.getMessage("station.gui.running"):""));
			}
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int idx = (int) event.getGUIHolder().getProperty(Main.pl, "index");
				if(idx > 0) {
					event.getGUIHolder().setProperty(Main.pl, "index", idx - 1);
					event.refreshInstance();
				}
			}
		}));
		
		builder.addElement(31, new StaticGUIElement(ItemUtils.createItem(Material.HOPPER, 1, 0, Config.getMessage("station.gui.song.current"))));
		
		builder.addElement(32, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(ItemUtils.arrowRight(), Config.getMessage("station.gui.song.next"), (r.isRunning()?Config.getMessage("station.gui.running"):""));
			}
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				int idx = (int) event.getGUIHolder().getProperty(Main.pl, "index");
				if(idx < r.getPlaylist().size() - 1) {
					event.getGUIHolder().setProperty(Main.pl, "index", idx + 1);
					event.refreshInstance();
				}
			}
		}));
		
		builder.addElement(34, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(Tools.minus(VersionedDyeColor.WHITE), Config.getMessage("station.gui.song.remove"), (r.isRunning()?Config.getMessage("station.gui.running"):""));
			}
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				int idx = (int) event.getGUIHolder().getProperty(Main.pl, "index");
				if(!r.isRunning()) {
					if(!r.getPlaylist().isEmpty()) {
						r.getPlaylist().remove(idx);
						if(idx >= r.getPlaylist().size() && !r.getPlaylist().isEmpty()) {
							idx = r.getPlaylist().size()-1;
						}
						event.refreshInstance();
						StationManager.updateStationGUI(id);
					}else {
						event.getPlayer().sendMessage(Config.getMessage("station.song-not-on-playlist"));
					}
				}
			}
		}));
		
		builder.addElement(45, new StaticGUIElement(ItemUtils.createItem(ItemUtils.arrowLeft(VersionedDyeColor.RED), Config.getMessage("gui.back"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				Player p = event.getPlayer();
				p.openInventory(GUIs.getStationsGUI(p, null));
			}
		}));
		
		builder.addElement(49, new GUIElement() {
			
			@Override
			public ItemStack getItem(GUIBuildEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				return ItemUtils.createItem(
						r.isBlacklistMode() ? VersionedMaterial.BLACK_BANNER : VersionedMaterial.WHITE_BANNER,
						1,
						Config.getMessage("station.gui.play-mode.message", "mode",
								r.isBlacklistMode() ?
										Config.getMessage("station.gui.play-mode.blacklist") :
										Config.getMessage("station.gui.play-mode.whitelist")), (r.isRunning()?Config.getMessage("station.gui.running"):"")
					);
			}
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				if(!r.isRunning()) {
					r.setBlacklistMode(!r.isBlacklistMode());
					StationManager.updateStationGUI(r.getID());
				}
			}
		}));
		
		builder.addElement(53, new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.RED_STAINED_CLAY, 1, Config.getMessage("station.gui.delete"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int id = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(id);
				Player p = event.getPlayer();
				p.sendMessage(Config.getMessage("station.deleted").replace("%station-name%", r.getName()).replace("%station-id%", ""+r.getID()));
				StationManager.deleteRadioStation(r.getID());
				p.openInventory(GUIs.getStationsGUI(p, null));
				StationManager.updateStationGUI(id);
				StationManager.updateStationsGUI();
			}
		}));
		
		builder.addPageSlotsInRange(37, 43);
		return builder.build();
	}
	
	private static GUIMultiPage<Song> buildSongGUI() {
		final VersionedMaterial[] disks = new VersionedMaterial[] {
				VersionedMaterial.MUSIC_DISC_BLOCKS,
				VersionedMaterial.MUSIC_DISC_CAT,
				VersionedMaterial.MUSIC_DISC_CHIRP,
				VersionedMaterial.MUSIC_DISC_FAR,
				VersionedMaterial.MUSIC_DISC_MALL,
				VersionedMaterial.MUSIC_DISC_MELLOHI,
				VersionedMaterial.MUSIC_DISC_STAL,
				VersionedMaterial.MUSIC_DISC_STRAD,
		};
		GUIBuilderMultiPage<Song> builder = new GUIBuilderMultiPage<>(Config.inventory_name, 6);
		builder.setSupplier(new ItemSupplier<Song>() {

			@Override
			public GUIElement toGUIElement(GUIBuildPageItemEvent<Song> event, Song item) {
				VersionedMaterial m = disks[event.getAbsoluteIndex() % disks.length];
				List<String> lore = new ArrayList<>();
				List<String> nSpl = Tools.split(item.getName(), 50, "§7");
				for(int in = 1; in < nSpl.size(); in++) {
					lore.add(nSpl.get(in));
				}
				lore.addAll(Tools.split(Config.getMessage("gui.songs.song.author").replace("%author%", (item.getOriginalAuthor()!=null?item.getOriginalAuthor():Config.default_author)), 50, "§7"));
				lore.addAll(Tools.split(Config.getMessage("gui.songs.song.by").replace("%maker%", (item.getAuthor()!=null?item.getAuthor():Config.default_author)), 50, "§7"));
				lore.add(Config.getMessage("gui.songs.song.id").replace("%id%", ""+item.getID()));
				ItemStack stack = ItemUtils.createItem(m, 1, "§7Song | "+nSpl.get(0), lore.toArray(new String[lore.size()]));
				ItemMeta im = stack.getItemMeta();
				im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
				stack.setItemMeta(im);
				return new StaticGUIElement(stack).setAction(event2 -> {
					int rID = (int) event2.getGUIHolder().getProperty(Main.pl, "station_id");
					RadioStation r = StationManager.getRadioStation(rID);
					int index = (int) event2.getGUIHolder().getProperty(Main.pl, "index");
					Player p = event2.getPlayer();
					if(!r.getPlaylist().isEmpty()) {
						r.getPlaylist().add(index+1, item.getID());
					}else {
						r.getPlaylist().add(item.getID());
					}
					StationManager.updateStationGUI(rID);
					if(Config.auto_close_song_gui) p.openInventory(GUIs.getStationGUI(p, rID, index+1));
				});
			}

			@Override
			public List<Song> getItems(GUIBuildEvent event) {
				String sortBy = (String) event.getGUIHolder().getProperty(Main.pl, "sort_by");
				List<Song> aSongs = new ArrayList<>(SongManager.songs);
				if(sortBy.equalsIgnoreCase("alphabet")) {
					aSongs.sort(new Comparator<Song>() {
						@Override
						public int compare(Song o1, Song o2) {
							return o1.getName().compareToIgnoreCase(o2.getName());
						}
					});
				}else if(sortBy.equalsIgnoreCase("id")) {
					aSongs.sort(new Comparator<Song>() {
						@Override
						public int compare(Song o1, Song o2) {
							return o1.getID()-o2.getID();
						}
					});
				}else if(sortBy.equalsIgnoreCase("author")) {
					aSongs.sort(new Comparator<Song>() {
						@Override
						public int compare(Song o1, Song o2) {
							return (o1.getOriginalAuthor()!=null?o1.getOriginalAuthor():Config.default_author).compareToIgnoreCase((o2.getOriginalAuthor()!=null?o2.getOriginalAuthor():Config.default_author));
						}
					});
				}
				return aSongs;
			}
		});
		
		GUIElement gPane = new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.BLACK_STAINED_GLASS_PANE, 1, "§0"));
		for(int i = 5*9; i < 6*9; i++) {
			builder.addElement(i, gPane);
		}
		
		builder.addElement(45, new StaticGUIElement(ItemUtils.createItem(ItemUtils.arrowLeft(VersionedDyeColor.RED), Config.getMessage("gui.stop-listening"))).setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				int rID = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				Inventory inv = getStationGUI(event.getPlayer(), rID, 0);
				event.getPlayer().openInventory(inv);
			}
		}));
		
		builder.addPreviousPageItem(52, ItemUtils.createItem(ItemUtils.arrowLeft(), Config.getMessage("gui.previous-page")));
		builder.addNextPageItem(53, ItemUtils.createItem(ItemUtils.arrowRight(), Config.getMessage("gui.next-page")));
		
		builder.addElement(51, new GUIElement() {
			
			public ItemStack getItem(GUIBuildEvent event) {
				String sortBy = (String) event.getGUIHolder().getProperty(Main.pl, "sort_by");

				VersionedMaterial m = VersionedMaterial.BLACK_BANNER;
				String n = "§cError";
				if(sortBy.equalsIgnoreCase("alphabet")) {
					m = VersionedMaterial.BROWN_BANNER;
					n = Config.getMessage("gui.songs.sorting-by").replace("%mode%", Config.getMessage("gui.songs.mode-alphabet"));
				}else if(sortBy.equalsIgnoreCase("id")) {
					m = VersionedMaterial.BLUE_BANNER;
					n = Config.getMessage("gui.songs.sorting-by").replace("%mode%", Config.getMessage("gui.songs.mode-id"));
				}else if(sortBy.equalsIgnoreCase("author")) {
					m = VersionedMaterial.GREEN_BANNER;
					n = Config.getMessage("gui.songs.sorting-by").replace("%mode%", Config.getMessage("gui.songs.mode-author"));
				}
				return ItemUtils.createItem(m, 1, n);
			}
			
		}.setAction(new GUIElementAction() {
			
			@Override
			public void onAction(GUIElementActionEvent event) {
				String sortBy = (String) event.getGUIHolder().getProperty(Main.pl, "sort_by");
				String sB = null;
				if(sortBy.equals("alphabet")) {
					sB = "id";
				}else if(sortBy.equals("id")) {
					sB = "author";
				}else if(sortBy.equals("author")) {
					sB = "alphabet";
				}
				event.getGUIHolder().setProperty(Main.pl, "sort_by", sB);
				SONGS_GUI.refreshInstance(event.getPlayer());
			}
		}));
		
		builder.addPageSlotsInRange(0, 44);
		
		return builder.build();
	}
	
	@SuppressWarnings("deprecation")
	private static GUI buildStationIconGUI() {
		GUIBuilder b = new GUIBuilder(Config.inventory_name, 1);
		ItemStack gPane = ItemUtils.createItem(VersionedMaterial.BLACK_STAINED_GLASS_PANE, 1, "§0");
		for(int i = 0; i < 9; i++) {
			b.addElement(i, new StaticGUIElement(gPane));
		}
		b.addElement(4, new StaticGUIElement(ItemUtils.createItem(VersionedMaterial.LIGHT_GRAY_BANNER, 1, Config.getMessage("station.gui.drop-here"))).setAction(event -> {
			if(event.getItemClickedWith() != null && !event.getItemClickedWith().getType().equals(Material.AIR)) {
				int rID = (int) event.getGUIHolder().getProperty(Main.pl, "station_id");
				RadioStation r = StationManager.getRadioStation(rID);
				r.setIcon(event.getItemClickedWith());
				if(!Config.remove_icon_items) {
					Tools.addItemsSafely(event.getPlayer(), event.getItemClickedWith());
				}
				event.getEvent().setCursor(new ItemStack(Material.AIR));
				event.getPlayer().openInventory(GUIs.getStationGUI(event.getPlayer(), rID, 0));
			}
		}));
		
		b.setDragDropListener(event -> {
			event.setCancelled(false);
		});
		return b.build();
	}
	
}
