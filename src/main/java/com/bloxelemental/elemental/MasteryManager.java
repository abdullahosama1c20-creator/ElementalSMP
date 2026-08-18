package com.bloxelemental.elemental;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles persistent storage and business logic for elements, mastery levels and XP.
 * Backed by a flat-file data.yml living in the plugin's data folder.
 */
public class MasteryManager {

    public static final int MAX_LEVEL = 100;
    public static final int MOBILITY_THRESHOLD = 25;
    public static final int HEAVY_THRESHOLD = 50;
    public static final int ULTIMATE_THRESHOLD = 100;

    private final ElementalSMP plugin;
    private final File dataFile;
    private FileConfiguration data;
    private double xpBasePerLevel = 50.0D;
    private double xpScalingPerLevel = 15.0D;

    public MasteryManager(ElementalSMP plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.xpBasePerLevel = plugin.getConfig().getDouble("mastery.xp-base-per-level", 50.0D);
        this.xpScalingPerLevel = plugin.getConfig().getDouble("mastery.xp-scaling-per-level", 15.0D);
    }

    public void loadData() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        if (data == null) {
            return;
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    private String path(UUID uuid, String key) {
        return "players." + uuid + "." + key;
    }

    public boolean hasElement(UUID uuid) {
        return data.isString(path(uuid, "element"));
    }

    public Element getElement(UUID uuid) {
        String raw = data.getString(path(uuid, "element"));
        if (raw == null) {
            return null;
        }
        return Element.fromArgument(raw);
    }

    public void setElement(UUID uuid, Element element) {
        data.set(path(uuid, "element"), element.name());
        if (!data.isInt(path(uuid, "level"))) {
            data.set(path(uuid, "level"), 1);
        }
        if (!data.isDouble(path(uuid, "xp")) && !data.isInt(path(uuid, "xp"))) {
            data.set(path(uuid, "xp"), 0.0D);
        }
        saveData();
    }

    public int getLevel(UUID uuid) {
        return data.getInt(path(uuid, "level"), 1);
    }

    public void setLevel(UUID uuid, int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        data.set(path(uuid, "level"), clamped);
        // Whenever the level is manually forced, remove any partial xp so
        // display stays consistent with the new level's threshold.
        data.set(path(uuid, "xp"), 0.0D);
        saveData();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            announceAbilities(player, clamped);
        }
    }

    public double getXP(UUID uuid) {
        return data.getDouble(path(uuid, "xp"), 0.0D);
    }

    /**
     * XP required to advance from the given level to the next one.
     */
    public double xpForNextLevel(int level) {
        return xpBasePerLevel + (level * xpScalingPerLevel);
    }

    /**
     * Adds mastery XP to the given player's currently selected element, applying
     * the elemental chamber bonus multiplier if the player is fighting inside
     * the active chamber, then resolves any level-ups (and the ability unlocks
     * that come with them).
     */
    public void addXP(Player player, double baseAmount) {
        UUID uuid = player.getUniqueId();
        if (!hasElement(uuid)) {
            return;
        }
        int level = getLevel(uuid);
        if (level >= MAX_LEVEL) {
            return;
        }

        Element element = getElement(uuid);
        double multiplier = plugin.getChamberManager() != null
                ? plugin.getChamberManager().getBonusMultiplier(player.getLocation(), element)
                : 1.0D;
        double amount = baseAmount * multiplier;
        boolean bonusApplied = multiplier > 1.0D;

        double xp = getXP(uuid) + amount;

        boolean leveledUp = false;
        while (level < MAX_LEVEL && xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level);
            level++;
            leveledUp = true;
        }
        if (level >= MAX_LEVEL) {
            level = MAX_LEVEL;
            xp = 0.0D;
        }

        data.set(path(uuid, "level"), level);
        data.set(path(uuid, "xp"), xp);
        saveData();

        if (bonusApplied) {
            String label = multiplier >= 3.0D ? "3x Matching Chamber Bonus" : "1.5x Chamber Bonus";
            player.sendActionBar(Component.text("+" + String.format("%.1f", amount) + " Mastery XP (" + label + ")", NamedTextColor.LIGHT_PURPLE));
        }

        if (leveledUp) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            player.sendMessage(Component.text("Your Mastery has grown! Level " + level, NamedTextColor.GOLD, TextDecoration.BOLD));
            announceAbilities(player, level);
        }
    }

    /**
     * If the given level exactly matches an unlock threshold, informs the player
     * of the new ability with a title and chat message.
     */
    private void announceAbilities(Player player, int level) {
        String unlocked = null;
        if (level == 1) {
            unlocked = "Basic Skill";
        } else if (level == MOBILITY_THRESHOLD) {
            unlocked = "Mobility Skill";
        } else if (level == HEAVY_THRESHOLD) {
            unlocked = "Heavy Combat Skill";
        } else if (level == ULTIMATE_THRESHOLD) {
            unlocked = "Ultimate Skill - Awakening Eligible!";
        } else {
            return;
        }
        player.showTitle(Title.title(
                Component.text("Ability Unlocked!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(unlocked, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));
    }

    /**
     * Returns the human readable list of abilities unlocked at the player's current level.
     */
    public List<String> getUnlockedAbilities(UUID uuid) {
        List<String> abilities = new ArrayList<>();
        int level = getLevel(uuid);
        if (level >= 1) {
            abilities.add("Basic Skill");
        }
        if (level >= MOBILITY_THRESHOLD) {
            abilities.add("Mobility Skill");
        }
        if (level >= HEAVY_THRESHOLD) {
            abilities.add("Heavy Combat Skill");
        }
        if (level >= ULTIMATE_THRESHOLD) {
            abilities.add("Ultimate Skill");
        }
        return abilities;
    }

    public boolean canUseTier(UUID uuid, int requiredLevel) {
        return getLevel(uuid) >= requiredLevel;
    }

    public boolean isAwakeningEligible(UUID uuid) {
        Element element = getElement(uuid);
        return element != null && element.isStarter() && getLevel(uuid) >= ULTIMATE_THRESHOLD;
    }

    /**
     * Forcefully awakens a player into Lightning or Void, resetting their
     * mastery progress on the new element back to level 1.
     */
    public void awaken(UUID uuid, Element advancedElement) {
        data.set(path(uuid, "element"), advancedElement.name());
        data.set(path(uuid, "level"), 1);
        data.set(path(uuid, "xp"), 0.0D);
        saveData();
    }

    // ---------------------------------------------------------------------
    // Stats tracking (kills, chambers cleared) and leaderboards
    // ---------------------------------------------------------------------

    public void incrementKills(UUID uuid) {
        data.set(path(uuid, "kills"), getKills(uuid) + 1);
        saveData();
    }

    public int getKills(UUID uuid) {
        return data.getInt(path(uuid, "kills"), 0);
    }

    public void incrementChambersCleared(UUID uuid) {
        data.set(path(uuid, "chambersCleared"), getChambersCleared(uuid) + 1);
        saveData();
    }

    public int getChambersCleared(UUID uuid) {
        return data.getInt(path(uuid, "chambersCleared"), 0);
    }

    /** One row of a /elemental top leaderboard. */
    public record LeaderboardEntry(UUID uuid, String name, Element element, int level, double xp) {
    }

    /**
     * Returns the top players by Mastery level (ties broken by XP), optionally
     * filtered to one element. Player names are resolved via Bukkit's offline
     * player cache, so anyone who has ever joined the server will show up.
     */
    public List<LeaderboardEntry> getTopPlayers(Element filter, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!data.isConfigurationSection("players")) {
            return entries;
        }
        for (String uuidString : data.getConfigurationSection("players").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Element element = getElement(uuid);
            if (element == null || (filter != null && element != filter)) {
                continue;
            }
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            entries.add(new LeaderboardEntry(uuid, name != null ? name : uuidString.substring(0, 8), element, getLevel(uuid), getXP(uuid)));
        }
        entries.sort((a, b) -> {
            if (b.level() != a.level()) {
                return Integer.compare(b.level(), a.level());
            }
            return Double.compare(b.xp(), a.xp());
        });
        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }
}
