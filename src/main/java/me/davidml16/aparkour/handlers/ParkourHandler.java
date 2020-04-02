package me.davidml16.aparkour.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.davidml16.aparkour.data.Parkour;
import me.davidml16.aparkour.data.Reward;
import me.davidml16.aparkour.data.WalkableBlock;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.davidml16.aparkour.Main;
import me.davidml16.aparkour.managers.ColorManager;
import org.bukkit.permissions.Permission;

public class ParkourHandler {

	private HashMap<String, Parkour> parkours;
	private HashMap<String, File> parkourFiles;
	private HashMap<String, YamlConfiguration> parkourConfigs;

	private boolean kickFromParkourOnFail;
	private GameMode parkourGamemode;

	public ParkourHandler() {
		this.parkours = new HashMap<String, Parkour>();
		this.parkourFiles = new HashMap<String, File>();
		this.parkourConfigs = new HashMap<String, YamlConfiguration>();
		this.kickFromParkourOnFail = Main.getInstance().getConfig().getBoolean("KickFromParkourOnFail.Enabled");
		this.parkourGamemode = GameMode.valueOf(Main.getInstance().getConfig().getString("ParkourGamemode"));
	}

	public HashMap<String, Parkour> getParkours() {
		return parkours;
	}

	public HashMap<String, File> getParkourFiles() {
		return parkourFiles;
	}

	public HashMap<String, YamlConfiguration> getParkourConfigs() {
		return parkourConfigs;
	}

	public boolean isKickFromParkourOnFail() {
		return kickFromParkourOnFail;
	}

	public void setKickFromParkourOnFail(boolean kickFromParkourOnFail) {
		this.kickFromParkourOnFail = kickFromParkourOnFail;
	}

	public GameMode getParkourGamemode() {
		return parkourGamemode;
	}

	public void setParkourGamemode(GameMode parkourGamemode) {
		this.parkourGamemode = parkourGamemode;
	}

	public boolean createParkour(String id) {
		File file = new File(Main.getInstance().getDataFolder(), "parkours/" + id + ".yml");
		if(!file.exists()) {
			try {
				file.createNewFile();
				parkourFiles.put(id, file);
				parkourConfigs.put(id, YamlConfiguration.loadConfiguration(file));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean removeParkour(String id) {
		if(parkourFiles.containsKey(id) && parkourConfigs.containsKey(id)) {
			File file = parkourFiles.get(id);
			file.delete();
			parkourFiles.remove(id);
			parkourConfigs.remove(id);
			return true;
		}
		return false;
	}

	public void saveConfig(String id) {
		try {
			File file = parkourFiles.get(id);
			if(file.exists()) {
				parkourConfigs.get(id).save(file);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FileConfiguration getConfig(String id) {
		return parkourConfigs.get(id);
	}

	public void loadParkours() {
		File directory = new File(Main.getInstance().getDataFolder(), "parkours");
		if(!directory.exists()) {
			directory.mkdir();
		}

		Main.log.sendMessage(ColorManager.translate(""));
		Main.log.sendMessage(ColorManager.translate("  &eLoading parkours:"));
		File[] allFiles = new File(Main.getInstance().getDataFolder(), "parkours").listFiles();
		for (File file : allFiles) {
			String id = file.getName().toLowerCase().replace(".yml", "");

			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			String name = config.getString("parkour.name");

			parkourFiles.put(id, file);
			parkourConfigs.put(id, config);

			if(!Character.isDigit(id.charAt(0))) {
				if (validParkourData(config)) {
					Location spawn = (Location) config.get("parkour.spawn");
					Location start = (Location) config.get("parkour.start");
					Location end = (Location) config.get("parkour.end");
					Location statsHologram = null;
					Location topHologram = null;

					if (Main.getInstance().isHologramsEnabled()) {
						if ((Location) config.get("parkour.holograms.stats") != null) {
							statsHologram = (Location) config.get("parkour.holograms.stats");
						}

						if ((Location) config.get("parkour.holograms.top") != null) {
							topHologram = (Location) config.get("parkour.holograms.top");
						}
					}

					if (parkours.size() < 21) {
						Parkour parkour = new Parkour(id, name, spawn, start, end, statsHologram, topHologram);
						parkours.put(id, parkour);

						if (config.contains("parkour.walkableBlocks")) {
							List<WalkableBlock> walkable = getWalkableBlocks(id);
							parkour.setWalkableBlocks(walkable);
							saveWalkableBlocksString(id, walkable);
							saveConfig(id);
						}

						if (!config.contains("parkour.rewards")) {
							config.set("parkour.rewards", new ArrayList<>());
							saveConfig(id);
						}

						if (!config.contains("parkour.permissionRequired")) {
							config.set("parkour.permissionRequired.enabled", false);
							config.set("parkour.permissionRequired.permission", "aparkour.permission." + id);
							config.set("parkour.permissionRequired.message", "&cYou dont have permission to start this parkour!");
							saveConfig(id);
						}

						if (config.contains("parkour.permissionRequired")) {
							parkour.setPermissionRequired(config.getBoolean("parkour.permissionRequired.enabled"));
							parkour.setPermission(config.getString("parkour.permissionRequired.permission"));
							parkour.setPermissionMessage(config.getString("parkour.permissionRequired.message"));

							if (Main.getInstance().getServer().getPluginManager().getPermission(parkour.getPermission()) == null) {
								Main.getInstance().getServer().getPluginManager().addPermission(new Permission(parkour.getPermission()));
							}
						}

						if (!config.contains("parkour.plateHolograms")) {
							config.set("parkour.plateHolograms.start.enabled", false);
							config.set("parkour.plateHolograms.start.distanceBelowPlate", 2.0D);
							config.set("parkour.plateHolograms.end.enabled", false);
							config.set("parkour.plateHolograms.end.distanceBelowPlate", 2.0D);
							saveConfig(id);
						}

						if (config.contains("parkour.plateHolograms")) {
							parkour.getStart().setHologramEnabled(config.getBoolean("parkour.plateHolograms.start.enabled"));
							parkour.getStart().setHologramDistance(config.getDouble("parkour.plateHolograms.start.distanceBelowPlate"));
							parkour.getEnd().setHologramEnabled(config.getBoolean("parkour.plateHolograms.end.enabled"));
							parkour.getEnd().setHologramDistance(config.getDouble("parkour.plateHolograms.end.distanceBelowPlate"));
						}

						Main.log.sendMessage(ColorManager.translate("    &a'" + name + "' loaded!"));
					} else {
						Main.log.sendMessage(ColorManager
								.translate("    &c'" + name + "' not loaded because maximum parkours limit reached!"));
					}
				} else {
					Main.log.sendMessage(ColorManager.translate("    &c'" + name + "' not loaded because parkour data is not correct!"));
				}
			} else {
				Main.log.sendMessage(ColorManager.translate("    &c'" + name + "' not loaded because parkour id starts with a number!"));
			}
		}
		
		if(parkours.size() == 0)
			Main.log.sendMessage(ColorManager.translate("    &cNo parkour has been loaded!"));
		
		Main.log.sendMessage(ColorManager.translate(""));
	}

	public void loadHolograms() {
		for (Parkour parkour : parkours.values()) {
			if(parkour.getStart().isHologramEnabled()) {
				Hologram hologram = HologramsAPI.createHologram(Main.getInstance(), parkour.getStart().getLocation().clone().add(0.5D, parkour.getStart().getHologramDistance(), 0.5D));
				hologram.appendTextLine(Main.getInstance().getLanguageHandler().getMessage("HOLOGRAMS_PLATES_START_LINE1"));
				hologram.appendTextLine(Main.getInstance().getLanguageHandler().getMessage("HOLOGRAMS_PLATES_START_LINE2"));
				parkour.getStart().setHologram(hologram);
			}
			if(parkour.getEnd().isHologramEnabled()) {
				Hologram hologram = HologramsAPI.createHologram(Main.getInstance(), parkour.getEnd().getLocation().clone().add(0.5D, parkour.getEnd().getHologramDistance(), 0.5D));
				hologram.appendTextLine(Main.getInstance().getLanguageHandler().getMessage("HOLOGRAMS_PLATES_END_LINE1"));
				hologram.appendTextLine(Main.getInstance().getLanguageHandler().getMessage("HOLOGRAMS_PLATES_END_LINE2"));
				parkour.getEnd().setHologram(hologram);
			}
		}
	}

	public boolean parkourExists(String id) {
		return parkourFiles.containsKey(id);
	}

	public boolean validParkourData(YamlConfiguration config) {
			return config.contains("parkour.spawn")
					&& config.contains("parkour.start")
					&& config.contains("parkour.end");
	}

	public Parkour getParkourById(String id) {
		for (Parkour parkour : parkours.values()) {
			if (parkour.getId().equalsIgnoreCase(id))
				return parkour;
		}
		return null;
	}

	public Parkour getParkourByLocation(Location loc) {
		for (Parkour parkour : parkours.values()) {
			if (loc.equals(parkour.getStart().getLocation()) || loc.equals(parkour.getEnd().getLocation()))
				return parkours.get(parkour.getId());
		}
		return null;
	}

	public List<WalkableBlock> getWalkableBlocks(String id) {
		List<WalkableBlock> walkable = new ArrayList<WalkableBlock>();
		if(parkourConfigs.get(id).contains("parkour.walkableBlocks")) {
			for (String block : parkourConfigs.get(id).getStringList("parkour.walkableBlocks")) {
				String[] parts = block.split(":");
				Material material = null;
				material = Material.getMaterial(Integer.parseInt(parts[0]));
				byte data = parts.length == 2 ? Byte.parseByte(parts[1]) : 0;
				WalkableBlock walkableBlockk = new WalkableBlock(Integer.parseInt(parts[0]), data);
				if (material != null && !walkable.contains(walkableBlockk)) {
					if (walkable.size() < 21) {
						walkable.add(walkableBlockk);
					}
				}
			}
		}
		return walkable;
	}

	public List<String> getWalkableBlocksString(List<WalkableBlock> walkable) {
		List<String> list = new ArrayList<String>();
		for(WalkableBlock block : walkable) {
			list.add(Material.getMaterial(block.getId()).getId() + ":" + block.getData());
		}
		return list;
	}

	public void saveWalkableBlocksString(String id, List<WalkableBlock> walkable) {
		List<String> list = new ArrayList<String>();
		for(WalkableBlock block : walkable) {
			list.add(Material.getMaterial(block.getId()).getId() + ":" + block.getData());
		}

		parkourConfigs.get(id).set("parkour.walkableBlocks", list);
		saveConfig(id);
	}

	public List<Reward> getRewards(String id) {
		List<Reward> rewards = new ArrayList<Reward>();
		if (parkourConfigs.get(id).contains("parkour.rewards")) {
			if (parkourConfigs.get(id).getConfigurationSection("parkour.rewards") != null) {
				for (String rewardid : parkourConfigs.get(id).getConfigurationSection("parkour.rewards").getKeys(false)) {
					if (validRewardData(id, rewardid)) {
						String permission = parkourConfigs.get(id).getString("parkour.rewards." + rewardid + ".permission");
						String command = parkourConfigs.get(id).getString("parkour.rewards." + rewardid + ".command");
						boolean firstTime = parkourConfigs.get(id).getBoolean("parkour.rewards." + rewardid + ".firstTime");
						rewards.add(new Reward(id, permission, command, firstTime));

						if (Main.getInstance().getServer().getPluginManager().getPermission(permission) == null) {
							Main.getInstance().getServer().getPluginManager().addPermission(new Permission(permission));
						}
					}
				}
			}
		}
		return rewards;
	}

	private boolean validRewardData(String parkourID, String rewardID) {
		return parkourConfigs.get(parkourID).contains("parkour.rewards." + rewardID + ".permission")
				&& parkourConfigs.get(parkourID).contains("parkour.rewards." + rewardID + ".command")
				&& parkourConfigs.get(parkourID).contains("parkour.rewards." + rewardID + ".firstTime");
	}

}
