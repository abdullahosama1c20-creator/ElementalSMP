package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {

    private static final long EXPIRY_SECONDS = 30L;

    private record PendingRequest(UUID requester, BukkitTask expiryTask) {
    }

    private final SurvivalUtils plugin;
    /** Keyed by the target's UUID - the player who must /tpaccept or /tpdeny. */
    private final Map<UUID, PendingRequest> pendingByTarget = new HashMap<>();

    public TpaManager(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    public boolean hasPendingRequestFor(UUID targetUuid) {
        return pendingByTarget.containsKey(targetUuid);
    }

    public void sendRequest(Player requester, Player target) {
        if (plugin.getSettingsManager().isAutoAcceptTpa(target.getUniqueId())) {
            requester.sendMessage(Component.text(target.getName() + " has auto-accept enabled - teleporting...", NamedTextColor.GREEN));
            teleportRequesterToTarget(requester, target);
            return;
        }

        clear(target.getUniqueId());
        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingByTarget.remove(target.getUniqueId());
            if (requester.isOnline()) {
                requester.sendMessage(Component.text("Your teleport request to " + target.getName() + " expired.", NamedTextColor.RED));
            }
            if (target.isOnline()) {
                target.sendMessage(Component.text(requester.getName() + "'s teleport request expired.", NamedTextColor.GRAY));
            }
        }, EXPIRY_SECONDS * 20L);

        pendingByTarget.put(target.getUniqueId(), new PendingRequest(requester.getUniqueId(), expiryTask));

        requester.sendMessage(Component.text("Teleport request sent to " + target.getName() + ". Expires in 30 seconds.", NamedTextColor.YELLOW));
        target.sendMessage(Component.text(requester.getName() + " wants to teleport to you. ", NamedTextColor.YELLOW)
                .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("/tpdeny", NamedTextColor.RED)));
        if (plugin.getSettingsManager().isSoundAlertsEnabled(target.getUniqueId())) {
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.2F);
        }
    }

    public void accept(Player target) {
        PendingRequest request = pendingByTarget.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return;
        }
        request.expiryTask().cancel();
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            return;
        }
        target.sendMessage(Component.text("Accepted. Teleporting " + requester.getName() + " to you...", NamedTextColor.GREEN));
        teleportRequesterToTarget(requester, target);
    }

    public void deny(Player target) {
        PendingRequest request = pendingByTarget.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return;
        }
        request.expiryTask().cancel();
        target.sendMessage(Component.text("Teleport request denied.", NamedTextColor.YELLOW));
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text(target.getName() + " denied your teleport request.", NamedTextColor.RED));
        }
    }

    private void teleportRequesterToTarget(Player requester, Player target) {
        if (plugin.getTeleportWarmupManager().isPending(requester.getUniqueId())) {
            requester.sendMessage(Component.text("You already have a teleport pending.", NamedTextColor.RED));
            return;
        }
        plugin.getTeleportWarmupManager().startWarmup(requester, target.getName(), () -> {
            requester.teleport(target.getLocation());
            requester.playSound(requester.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            requester.sendMessage(Component.text("Teleported to " + target.getName() + "!", NamedTextColor.GREEN));
        });
    }

    public void clear(UUID targetUuid) {
        PendingRequest request = pendingByTarget.remove(targetUuid);
        if (request != null) {
            request.expiryTask().cancel();
        }
    }
}
