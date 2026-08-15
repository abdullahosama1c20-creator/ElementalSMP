package com.example.utilitypvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CombatManager {
    private static final long COMBAT_DURATION_MILLIS = 15_000L;
    private final UtilityPvP plugin;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();
    private BukkitTask task;

    CombatManager(UtilityPvP plugin) {
        this.plugin = plugin;
    }

    void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 2L);
    }

    void tag(Player player) {
        combatUntil.put(player.getUniqueId(), System.currentTimeMillis() + COMBAT_DURATION_MILLIS);
    }

    void tag(Player first, Player second) {
        tag(first);
        tag(second);
    }

    boolean isTagged(Player player) {
        return isTagged(player.getUniqueId());
    }

    boolean isTagged(UUID uuid) {
        Long until = combatUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            combatUntil.remove(uuid);
            return false;
        }
        return true;
    }

    long getRemainingSeconds(UUID uuid) {
        Long until = combatUntil.get(uuid);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) {
            combatUntil.remove(uuid);
            return 0L;
        }
        return (remaining + 999L) / 1000L;
    }

    void clear(UUID uuid) {
        combatUntil.remove(uuid);
    }

    void handleQuit(Player player) {
        if (!isTagged(player)) {
            return;
        }
        combatUntil.remove(player.getUniqueId());
        if (!player.isDead()) {
            player.setHealth(0.0D);
        }
    }

    void shutdown() {
        if (task != null) {
            task.cancel();
        }
        combatUntil.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : combatUntil.entrySet()) {
            if (entry.getValue() <= now) {
                combatUntil.remove(entry.getKey(), entry.getValue());
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.sendActionBar(Component.text("Combat tag expired.", NamedTextColor.GREEN));
                }
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                long seconds = (entry.getValue() - now + 999L) / 1000L;
                player.sendActionBar(Component.text("⚔ Combat: " + seconds + "s", NamedTextColor.RED));
            }
        }
    }
}
