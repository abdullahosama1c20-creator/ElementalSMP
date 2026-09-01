package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AfkManager {

    private final long afkThresholdMs;
    private final SurvivalUtils plugin;
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Set<UUID> afkPlayers = new HashSet<>();

    public AfkManager(SurvivalUtils plugin) {
        this.plugin = plugin;
        this.afkThresholdMs = plugin.getConfig().getLong("afk.threshold-minutes", 3L) * 60 * 1000L;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 30L, 20L * 30L);
    }

    public void markActive(UUID uuid) {
        lastActivity.put(uuid, System.currentTimeMillis());
        if (afkPlayers.remove(uuid)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Bukkit.broadcast(Component.text(player.getName() + " is no longer AFK.", NamedTextColor.GRAY));
            }
        }
    }

    public boolean isAfk(UUID uuid) {
        return afkPlayers.contains(uuid);
    }

    public void clear(UUID uuid) {
        lastActivity.remove(uuid);
        afkPlayers.remove(uuid);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Long last = lastActivity.get(uuid);
            if (last == null) {
                lastActivity.put(uuid, now);
                continue;
            }
            boolean shouldBeAfk = (now - last) >= afkThresholdMs;
            if (shouldBeAfk && afkPlayers.add(uuid)) {
                Bukkit.broadcast(Component.text(player.getName() + " is now AFK.", NamedTextColor.GRAY));
            }
        }
    }
}
