package com.survivalutils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public class HomeManager {

    public static final String DEFAULT_HOME = "home";
    private static final int MAX_HOMES_PER_PLAYER = 10;

    private final SurvivalUtils plugin;
    private final File file;
    private FileConfiguration data;

    public HomeManager(SurvivalUtils plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create homes.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (data == null) {
            return;
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save homes.yml", e);
        }
    }

    private String path(UUID uuid, String name) {
        return "homes." + uuid + "." + name.toLowerCase(Locale.ROOT);
    }

    public boolean setHome(UUID uuid, String name, Location location) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (!data.contains("homes." + uuid) && getHomeNames(uuid).size() >= MAX_HOMES_PER_PLAYER) {
            return false;
        }
        if (!getHomeNames(uuid).contains(normalized) && getHomeNames(uuid).size() >= MAX_HOMES_PER_PLAYER) {
            return false;
        }
        data.set(path(uuid, normalized), serialize(location));
        save();
        return true;
    }

    public Location getHome(UUID uuid, String name) {
        String raw = data.getString(path(uuid, name));
        if (raw == null) {
            return null;
        }
        return deserialize(raw);
    }

    public boolean deleteHome(UUID uuid, String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (!data.contains(path(uuid, normalized))) {
            return false;
        }
        data.set(path(uuid, normalized), null);
        save();
        return true;
    }

    public List<String> getHomeNames(UUID uuid) {
        List<String> names = new ArrayList<>();
        if (data.isConfigurationSection("homes." + uuid)) {
            names.addAll(data.getConfigurationSection("homes." + uuid).getKeys(false));
        }
        return names;
    }

    private String serialize(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ()
                + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    private Location deserialize(String raw) {
        String[] parts = raw.split(";");
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
