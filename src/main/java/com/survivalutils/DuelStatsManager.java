package com.survivalutils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DuelStatsManager {

    private final SurvivalUtils plugin;
    private final File file;
    private FileConfiguration data;

    public DuelStatsManager(SurvivalUtils plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "duelstats.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create duelstats.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save duelstats.yml", e);
        }
    }

    public void recordWin(UUID uuid) {
        data.set("players." + uuid + ".wins", getWins(uuid) + 1);
        save();
    }

    public void recordLoss(UUID uuid) {
        data.set("players." + uuid + ".losses", getLosses(uuid) + 1);
        save();
    }

    public int getWins(UUID uuid) {
        return data.getInt("players." + uuid + ".wins", 0);
    }

    public int getLosses(UUID uuid) {
        return data.getInt("players." + uuid + ".losses", 0);
    }

    public record DuelRecord(UUID uuid, String name, int wins, int losses) {
    }

    public List<DuelRecord> getTop(int limit) {
        List<DuelRecord> records = new ArrayList<>();
        if (!data.isConfigurationSection("players")) {
            return records;
        }
        for (String uuidString : data.getConfigurationSection("players").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                continue;
            }
            int wins = getWins(uuid);
            if (wins == 0) {
                continue;
            }
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            records.add(new DuelRecord(uuid, name != null ? name : uuidString.substring(0, 8), wins, getLosses(uuid)));
        }
        records.sort((a, b) -> Integer.compare(b.wins(), a.wins()));
        return records.size() > limit ? records.subList(0, limit) : records;
    }
}
