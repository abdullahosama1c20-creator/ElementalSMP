package com.example.utilitypvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class SettingsManager {
    private static final String TITLE = "Player Settings";
    private final UtilityPvP plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();

    SettingsManager(UtilityPvP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "settings.yml");
    }

    void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data directory.");
        }
        data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                cache.put(uuid, new PlayerSettings(
                        section.getBoolean(key + ".autoAcceptTpa", false),
                        section.getBoolean(key + ".publicChat", true),
                        section.getBoolean(key + ".scoreboard", true),
                        section.getBoolean(key + ".duelRequests", true),
                        section.getBoolean(key + ".soundAlerts", true)
                ));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid UUID in settings.yml: " + key);
            }
        }
    }

    void save() {
        for (Map.Entry<UUID, PlayerSettings> entry : cache.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerSettings s = entry.getValue();
            data.set(path + ".autoAcceptTpa", s.autoAcceptTpa());
            data.set(path + ".publicChat", s.publicChat());
            data.set(path + ".scoreboard", s.scoreboard());
            data.set(path + ".duelRequests", s.duelRequests());
            data.set(path + ".soundAlerts", s.soundAlerts());
        }
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save settings.yml: " + exception.getMessage());
        }
    }

    PlayerSettings get(UUID uuid) {
        return cache.computeIfAbsent(uuid, ignored -> new PlayerSettings(false, true, true, true, true));
    }

    boolean isAutoAcceptTpa(UUID uuid) { return get(uuid).autoAcceptTpa(); }
    boolean isPublicChatVisible(UUID uuid) { return get(uuid).publicChat(); }
    boolean isScoreboardEnabled(UUID uuid) { return get(uuid).scoreboard(); }
    boolean isDuelRequestsEnabled(UUID uuid) { return get(uuid).duelRequests(); }
    boolean isSoundAlertsEnabled(UUID uuid) { return get(uuid).soundAlerts(); }

    void toggle(UUID uuid, SettingKey key) {
        PlayerSettings old = get(uuid);
        PlayerSettings next = switch (key) {
            case AUTO_ACCEPT_TPA -> old.withAutoAcceptTpa(!old.autoAcceptTpa());
            case PUBLIC_CHAT -> old.withPublicChat(!old.publicChat());
            case SCOREBOARD -> old.withScoreboard(!old.scoreboard());
            case DUEL_REQUESTS -> old.withDuelRequests(!old.duelRequests());
            case SOUND_ALERTS -> old.withSoundAlerts(!old.soundAlerts());
        };
        cache.put(uuid, next);
        save();
    }

    void openMenu(Player player) {
        SettingsHolder holder = new SettingsHolder();
        Inventory inventory = Bukkit.createInventory(holder, 36, Component.text(TITLE, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
        holder.inventory = inventory;

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot = 0; slot < 36; slot++) {
            inventory.setItem(slot, filler);
        }

        PlayerSettings s = get(player.getUniqueId());
        inventory.setItem(10, toggleItem("Auto-Accept TPA", s.autoAcceptTpa(), Material.ENDER_PEARL));
        inventory.setItem(12, toggleItem("Public Chat Visibility", s.publicChat(), Material.PAPER));
        inventory.setItem(14, toggleItem("Scoreboard", s.scoreboard(), Material.BOOK));
        inventory.setItem(16, toggleItem("Duel Requests", s.duelRequests(), Material.IRON_SWORD));
        inventory.setItem(22, toggleItem("Sound Alerts", s.soundAlerts(), Material.NOTE_BLOCK));
        inventory.setItem(31, item(Material.BARRIER, Component.text("Close", NamedTextColor.RED)));

        player.openInventory(inventory);
    }

    boolean isSettingsInventory(Inventory inventory) {
        return inventory.getHolder() instanceof SettingsHolder;
    }

    void handleClick(Player player, int slot) {
        SettingKey key = switch (slot) {
            case 10 -> SettingKey.AUTO_ACCEPT_TPA;
            case 12 -> SettingKey.PUBLIC_CHAT;
            case 14 -> SettingKey.SCOREBOARD;
            case 16 -> SettingKey.DUEL_REQUESTS;
            case 22 -> SettingKey.SOUND_ALERTS;
            default -> null;
        };

        if (slot == 31) {
            player.closeInventory();
            return;
        }
        if (key == null) {
            return;
        }

        toggle(player.getUniqueId(), key);
        if (key == SettingKey.SOUND_ALERTS && isSoundAlertsEnabled(player.getUniqueId())) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        } else if (isSoundAlertsEnabled(player.getUniqueId())) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        }
        openMenu(player);
        if (key == SettingKey.SCOREBOARD) {
            plugin.getScoreboardService().update(player);
        }
    }

    private ItemStack toggleItem(String name, boolean enabled, Material icon) {
        Material material = enabled ? Material.LIME_DYE : Material.RED_DYE;
        ItemStack stack = item(material, Component.text(name, NamedTextColor.WHITE));
        ItemMeta meta = stack.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: ", NamedTextColor.GRAY).append(Component.text(enabled ? "ENABLED" : "DISABLED", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.text("Click to toggle", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack item(Material material, Component name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.customName(name);
        stack.setItemMeta(meta);
        return stack;
    }
}

enum SettingKey {
    AUTO_ACCEPT_TPA,
    PUBLIC_CHAT,
    SCOREBOARD,
    DUEL_REQUESTS,
    SOUND_ALERTS
}

record PlayerSettings(boolean autoAcceptTpa, boolean publicChat, boolean scoreboard, boolean duelRequests, boolean soundAlerts) {
    PlayerSettings withAutoAcceptTpa(boolean value) { return new PlayerSettings(value, publicChat, scoreboard, duelRequests, soundAlerts); }
    PlayerSettings withPublicChat(boolean value) { return new PlayerSettings(autoAcceptTpa, value, scoreboard, duelRequests, soundAlerts); }
    PlayerSettings withScoreboard(boolean value) { return new PlayerSettings(autoAcceptTpa, publicChat, value, duelRequests, soundAlerts); }
    PlayerSettings withDuelRequests(boolean value) { return new PlayerSettings(autoAcceptTpa, publicChat, scoreboard, value, soundAlerts); }
    PlayerSettings withSoundAlerts(boolean value) { return new PlayerSettings(autoAcceptTpa, publicChat, scoreboard, duelRequests, value); }
}

final class SettingsHolder implements InventoryHolder {
    Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
