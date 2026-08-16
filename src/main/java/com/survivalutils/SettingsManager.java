package com.survivalutils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class SettingsManager {

    public enum Toggle {
        AUTO_ACCEPT_TPA("autoAcceptTpa", false),
        PUBLIC_CHAT("publicChat", true),
        SCOREBOARD("scoreboard", true),
        DUEL_REQUESTS("duelRequests", true),
        SOUND_ALERTS("soundAlerts", true);

        final String key;
        final boolean defaultValue;

        Toggle(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    private final SurvivalUtils plugin;
    private final File file;
    private FileConfiguration data;

    public SettingsManager(SurvivalUtils plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "settings.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create settings.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save settings.yml", e);
        }
    }

    private String path(UUID uuid, Toggle toggle) {
        return "players." + uuid + "." + toggle.key;
    }

    public boolean get(UUID uuid, Toggle toggle) {
        return data.getBoolean(path(uuid, toggle), toggle.defaultValue);
    }

    public void set(UUID uuid, Toggle toggle, boolean value) {
        data.set(path(uuid, toggle), value);
        save();
    }

    public void toggle(UUID uuid, Toggle toggle) {
        set(uuid, toggle, !get(uuid, toggle));
    }

    public boolean isAutoAcceptTpa(UUID uuid) {
        return get(uuid, Toggle.AUTO_ACCEPT_TPA);
    }

    public boolean isPublicChatEnabled(UUID uuid) {
        return get(uuid, Toggle.PUBLIC_CHAT);
    }

    public boolean isScoreboardEnabled(UUID uuid) {
        return get(uuid, Toggle.SCOREBOARD);
    }

    public boolean isDuelRequestsEnabled(UUID uuid) {
        return get(uuid, Toggle.DUEL_REQUESTS);
    }

    public boolean isSoundAlertsEnabled(UUID uuid) {
        return get(uuid, Toggle.SOUND_ALERTS);
    }
}
