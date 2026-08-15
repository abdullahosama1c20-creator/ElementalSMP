package com.example.utilitypvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class TeleportWarmupManager {
    private final UtilityPvP plugin;
    private final CombatManager combat;
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

    TeleportWarmupManager(UtilityPvP plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    void begin(Player player, Location destination, String label) {
        cancel(player.getUniqueId(), "");
        Location origin = player.getLocation().clone();
        UUID uuid = player.getUniqueId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PendingTeleport current = pending.remove(uuid);
            if (current == null || !player.isOnline()) {
                return;
            }
            if (combat.isTagged(player)) {
                player.sendMessage(Component.text("You cannot teleport while combat tagged.", NamedTextColor.RED));
                return;
            }
            player.teleportAsync(destination.clone()).thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.text("Teleported to " + label + ".", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Teleport failed.", NamedTextColor.RED));
                }
            });
        }, 60L);

        pending.put(uuid, new PendingTeleport(origin, task));
        player.sendMessage(Component.text("Teleporting to " + label + " in 3 seconds. Do not move or take damage.", NamedTextColor.YELLOW));
    }

    void cancelOnMove(Player player) {
        PendingTeleport p = pending.get(player.getUniqueId());
        if (p == null) {
            return;
        }
        cancel(player.getUniqueId(), "Teleport cancelled because you moved.");
    }

    void cancelOnDamage(Player player) {
        if (pending.containsKey(player.getUniqueId())) {
            cancel(player.getUniqueId(), "Teleport cancelled because you took damage.");
        }
    }

    void cancel(UUID uuid, String message) {
        PendingTeleport p = pending.remove(uuid);
        if (p != null) {
            p.task().cancel();
            if (!message.isEmpty()) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text(message, NamedTextColor.RED));
                }
            }
        }
    }

    void cancelAll() {
        for (UUID uuid : pending.keySet()) {
            cancel(uuid, "");
        }
    }

    Location getOrigin(UUID uuid) {
        PendingTeleport p = pending.get(uuid);
        return p == null ? null : p.origin();
    }

    private record PendingTeleport(Location origin, BukkitTask task) {}
}
