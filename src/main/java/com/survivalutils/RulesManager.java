package com.survivalutils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class RulesManager {

    private final SurvivalUtils plugin;
    private final File file;
    private FileConfiguration data;

    public RulesManager(SurvivalUtils plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rules.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        boolean isNew = !file.exists();
        if (isNew) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create rules.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        if (isNew || !data.isList("lines")) {
            data.set("lines", defaultRules());
            save();
        }
    }

    private List<String> defaultRules() {
        List<String> defaults = new ArrayList<>();
        defaults.add("Be respectful to other players and staff.");
        defaults.add("No griefing, stealing, or cheating/hacked clients.");
        defaults.add("No spamming or advertising other servers.");
        defaults.add("Follow staff instructions at all times.");
        defaults.add("Use //rules to edit this list (OP only).");
        return defaults;
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save rules.yml", e);
        }
    }

    public List<String> getRules() {
        return data.getStringList("lines");
    }

    public void setRules(List<String> lines) {
        data.set("lines", lines);
        save();
    }

    public void addRule(String line) {
        List<String> lines = new ArrayList<>(getRules());
        lines.add(line);
        setRules(lines);
    }

    public boolean removeRule(int index) {
        List<String> lines = new ArrayList<>(getRules());
        if (index < 0 || index >= lines.size()) {
            return false;
        }
        lines.remove(index);
        setRules(lines);
        return true;
    }

    public void clearRules() {
        setRules(new ArrayList<>());
    }
}
