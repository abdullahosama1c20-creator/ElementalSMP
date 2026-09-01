package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A lightweight duel system: no dedicated arena/world management, just a
 * mutually agreed 1v1 fight. Whoever dies first loses; normal death/drop
 * rules still apply (this doesn't make deaths safe - see the note in
 * DuelListener). Honors the Duel Requests setting toggle.
 */
public class DuelManager {

    private record PendingRequest(UUID requester, BukkitTask expiryTask) {
    }

    private final long expirySeconds;
    private final SurvivalUtils plugin;
    private final Map<UUID, PendingRequest> pendingByTarget = new HashMap<>();
    /** Both directions stored so either participant can be looked up by UUID. */
    private final Map<UUID, UUID> activeDuels = new HashMap<>();

    public DuelManager(SurvivalUtils plugin) {
        this.plugin = plugin;
        this.expirySeconds = plugin.getConfig().getLong("duel.expiry-seconds", 30L);
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public void sendRequest(Player requester, Player target) {
        if (!plugin.getSettingsManager().isDuelRequestsEnabled(target.getUniqueId())) {
            requester.sendMessage(Component.text(target.getName() + " is not accepting duel requests.", NamedTextColor.RED));
            return;
        }
        if (isInDuel(requester.getUniqueId()) || isInDuel(target.getUniqueId())) {
            requester.sendMessage(Component.text("One of you is already in a duel.", NamedTextColor.RED));
            return;
        }

        clearPending(target.getUniqueId());
        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingByTarget.remove(target.getUniqueId());
            if (requester.isOnline()) {
                requester.sendMessage(Component.text("Your duel request to " + target.getName() + " expired.", NamedTextColor.RED));
            }
        }, expirySeconds * 20L);

        pendingByTarget.put(target.getUniqueId(), new PendingRequest(requester.getUniqueId(), expiryTask));

        requester.sendMessage(Component.text("Duel request sent to " + target.getName() + ". Expires in " + expirySeconds + " seconds.", NamedTextColor.YELLOW));
        target.sendMessage(Component.text(requester.getName() + " has challenged you to a duel! ", NamedTextColor.YELLOW)
                .append(Component.text("/duelaccept", NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("/dueldeny", NamedTextColor.RED)));
        if (plugin.getSettingsManager().isSoundAlertsEnabled(target.getUniqueId())) {
            target.playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6F, 1.4F);
        }
    }

    public void accept(Player target) {
        PendingRequest request = pendingByTarget.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(Component.text("You have no pending duel requests.", NamedTextColor.RED));
            return;
        }
        request.expiryTask().cancel();
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            return;
        }
        if (requester.getGameMode() == GameMode.SPECTATOR || target.getGameMode() == GameMode.SPECTATOR) {
            target.sendMessage(Component.text("Can't duel while in spectator mode.", NamedTextColor.RED));
            return;
        }

        activeDuels.put(requester.getUniqueId(), target.getUniqueId());
        activeDuels.put(target.getUniqueId(), requester.getUniqueId());

        for (Player p : new Player[]{requester, target}) {
            p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            p.setFoodLevel(20);
            p.showTitle(Title.title(
                    Component.text("DUEL!", NamedTextColor.RED),
                    Component.text("Fight!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(300))
            ));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
        }
    }

    public void deny(Player target) {
        PendingRequest request = pendingByTarget.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(Component.text("You have no pending duel requests.", NamedTextColor.RED));
            return;
        }
        request.expiryTask().cancel();
        target.sendMessage(Component.text("Duel request denied.", NamedTextColor.YELLOW));
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text(target.getName() + " denied your duel request.", NamedTextColor.RED));
        }
    }

    /** Ends a duel without a death - e.g. one side disconnects or forfeits. */
    public void endDuel(UUID uuid, String reason) {
        UUID opponentUuid = activeDuels.remove(uuid);
        if (opponentUuid == null) {
            return;
        }
        activeDuels.remove(opponentUuid);
        Player opponent = Bukkit.getPlayer(opponentUuid);
        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(Component.text("The duel has ended: " + reason, NamedTextColor.YELLOW));
        }
    }

    public void clearPending(UUID targetUuid) {
        PendingRequest request = pendingByTarget.remove(targetUuid);
        if (request != null) {
            request.expiryTask().cancel();
        }
    }
}
