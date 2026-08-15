package com.example.utilitypvp;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class HomeManager {
    private final UtilityPvP plugin;
    private final File file;
    private FileConfiguration data;

    HomeManager(UtilityPvP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
    }

    void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data directory.");
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save homes.yml: " + exception.getMessage());
        }
    }

    boolean isValidName(String name) {
        return name != null && name.length() <= 16 && name.matches("[A-Za-z0-9_-]+");
    }

    void setHome(UUID uuid, String name, Location location) {
        data.set(path(uuid, name), location);
        save();
    }

    Location getHome(UUID uuid, String name) {
        return data.getLocation(path(uuid, name));
    }

    boolean deleteHome(UUID uuid, String name) {
        String path = path(uuid, name);
        if (!data.contains(path)) {
            return false;
        }
        data.set(path, null);
        save();
        return true;
    }

    List<String> getHomeNames(UUID uuid) {
        ConfigurationSection section = data.getConfigurationSection("homes." + uuid);
        if (section == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    private String path(UUID uuid, String name) {
        return "homes." + uuid + "." + name.toLowerCase();
    }
}
