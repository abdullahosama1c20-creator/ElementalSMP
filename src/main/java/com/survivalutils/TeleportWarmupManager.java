package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportWarmupManager implements Listener {

    private static final double MOVE_CANCEL_THRESHOLD = 0.3D;

    private final int warmupSeconds;
    private final SurvivalUtils plugin;
    private final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();
    private final Map<UUID, Location> startLocations = new HashMap<>();

    public TeleportWarmupManager(SurvivalUtils plugin) {
        this.plugin = plugin;
        this.warmupSeconds = plugin.getConfig().getInt("teleport.warmup-seconds", 3);
    }

    public boolean isPending(UUID uuid) {
        return pendingTasks.containsKey(uuid);
    }

    /**
     * Begins a 3-second warmup for the player. If they move more than a small
     * threshold or take damage before it finishes, the teleport is cancelled
     * and onComplete never runs.
     */
    public void startWarmup(Player player, String destinationLabel, Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        cancel(uuid, null);

        startLocations.put(uuid, player.getLocation());
        player.sendMessage(Component.text("Teleporting to " + destinationLabel + " in " + warmupSeconds + " seconds. Don't move!", NamedTextColor.YELLOW));

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingTasks.remove(uuid);
            startLocations.remove(uuid);
            if (player.isOnline()) {
                onComplete.run();
            }
        }, warmupSeconds * 20L);

        pendingTasks.put(uuid, task);
    }

    /**
     * Cancels a pending warmup, if any. Pass a non-null reason to notify the player why.
     */
    public void cancel(UUID uuid, String reason) {
        BukkitTask task = pendingTasks.remove(uuid);
        startLocations.remove(uuid);
        if (task != null) {
            task.cancel();
            if (reason != null) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(Component.text("Teleport cancelled: " + reason, NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Location start = startLocations.get(uuid);
        if (start == null) {
            return;
        }
        if (event.getTo() == null || start.distanceSquared(event.getTo()) < (MOVE_CANCEL_THRESHOLD * MOVE_CANCEL_THRESHOLD)) {
            return;
        }
        cancel(uuid, "you moved.");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isPending(player.getUniqueId())) {
            cancel(player.getUniqueId(), "you took damage.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), null);
    }
}
