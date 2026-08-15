package com.example.utilitypvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class TpaManager {
    private static final long REQUEST_DURATION_MILLIS = 30_000L;
    private final UtilityPvP plugin;
    private final CombatManager combat;
    private final SettingsManager settings;
    private final Map<UUID, TpaRequest> incoming = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> outgoingByPlayer = new ConcurrentHashMap<>();

    TpaManager(UtilityPvP plugin, CombatManager combat, SettingsManager settings) {
        this.plugin = plugin;
        this.combat = combat;
        this.settings = settings;
    }

    void request(Player requester, Player target) {
        UUID requesterId = requester.getUniqueId();
        UUID targetId = target.getUniqueId();
        TpaRequest previous = incoming.put(targetId, new TpaRequest(requesterId, targetId, System.currentTimeMillis() + REQUEST_DURATION_MILLIS));
        if (previous != null) {
            outgoingByPlayer.remove(previous.requester());
        }
        outgoingByPlayer.put(requesterId, targetId);

        if (settings.isAutoAcceptTpa(targetId)) {
            if (combat.isTagged(requester) || combat.isTagged(target)) {
                remove(targetId);
                requester.sendMessage(Component.text("TPA could not be auto-accepted because a player is combat tagged.", NamedTextColor.RED));
                target.sendMessage(Component.text("Auto-accept was blocked while combat tagged.", NamedTextColor.RED));
                return;
            }
            accept(target);
            return;
        }

        target.sendMessage(Component.text(requester.getName() + " sent you a TPA request. Use /tpaccept or /tpdeny.", NamedTextColor.AQUA));
        requester.sendMessage(Component.text("TPA request sent to " + target.getName() + ". Expires in 30 seconds.", NamedTextColor.GREEN));
        if (settings.isSoundAlertsEnabled(targetId)) {
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        }
    }

    void accept(Player target) {
        TpaRequest request = incoming.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(Component.text("You have no pending TPA request.", NamedTextColor.RED));
            return;
        }
        outgoingByPlayer.remove(request.requester());

        if (request.expiresAt() <= System.currentTimeMillis()) {
            target.sendMessage(Component.text("That TPA request has expired.", NamedTextColor.RED));
            return;
        }

        Player requester = plugin.getServer().getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("The requester is no longer online.", NamedTextColor.RED));
            return;
        }
        if (combat.isTagged(target) || combat.isTagged(requester)) {
            target.sendMessage(Component.text("TPA cannot be accepted while either player is combat tagged.", NamedTextColor.RED));
            requester.sendMessage(Component.text("Your TPA was denied because you or the target is combat tagged.", NamedTextColor.RED));
            return;
        }

        requester.teleportAsync(target.getLocation().clone()).thenAccept(success -> {
            if (success) {
                requester.sendMessage(Component.text("TPA accepted. Teleported to " + target.getName() + ".", NamedTextColor.GREEN));
                target.sendMessage(Component.text("TPA accepted for " + requester.getName() + ".", NamedTextColor.GREEN));
            } else {
                requester.sendMessage(Component.text("TPA teleport failed.", NamedTextColor.RED));
            }
        });
    }

    void deny(Player target) {
        TpaRequest request = incoming.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(Component.text("You have no pending TPA request.", NamedTextColor.RED));
            return;
        }
        outgoingByPlayer.remove(request.requester());
        Player requester = plugin.getServer().getPlayer(request.requester());
        target.sendMessage(Component.text("TPA request denied.", NamedTextColor.YELLOW));
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text(target.getName() + " denied your TPA request.", NamedTextColor.RED));
        }
    }

    void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (TpaRequest request : incoming.values().toArray(new TpaRequest[0])) {
            if (request.expiresAt() <= now) {
                if (incoming.remove(request.target(), request)) {
                    outgoingByPlayer.remove(request.requester());
                    Player requester = plugin.getServer().getPlayer(request.requester());
                    Player target = plugin.getServer().getPlayer(request.target());
                    if (requester != null && requester.isOnline()) {
                        requester.sendMessage(Component.text("Your TPA request expired.", NamedTextColor.YELLOW));
                    }
                    if (target != null && target.isOnline()) {
                        target.sendMessage(Component.text("A TPA request expired.", NamedTextColor.YELLOW));
                    }
                }
            }
        }
    }

    void handleQuit(UUID uuid) {
        UUID target = outgoingByPlayer.remove(uuid);
        if (target != null) {
            incoming.remove(target);
        }
        TpaRequest request = incoming.remove(uuid);
        if (request != null) {
            outgoingByPlayer.remove(request.requester());
            Player requester = plugin.getServer().getPlayer(request.requester());
            if (requester != null && requester.isOnline()) {
                requester.sendMessage(Component.text("Your TPA request was cancelled because the target disconnected.", NamedTextColor.YELLOW));
            }
        }
    }

    void clearAll() {
        incoming.clear();
        outgoingByPlayer.clear();
    }

    private void remove(UUID target) {
        TpaRequest request = incoming.remove(target);
        if (request != null) {
            outgoingByPlayer.remove(request.requester());
        }
    }

    private record TpaRequest(UUID requester, UUID target, long expiresAt) {}
}
