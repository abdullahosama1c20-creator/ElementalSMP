package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class DuelListener implements Listener {

    private final SurvivalUtils plugin;

    public DuelListener(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!plugin.getDuelManager().isInDuel(uuid)) {
            return;
        }

        double resultingHealth = player.getHealth() - event.getFinalDamage();
        if (resultingHealth > 0) {
            return;
        }

        // Lethal hit during a duel: cancel the actual death and declare the opponent the winner instead.
        event.setCancelled(true);
        player.setHealth(1.0);

        UUID opponentUuid = plugin.getDuelManager().getOpponent(uuid);
        plugin.getDuelManager().endDuel(uuid, "you lost!");

        Player opponent = opponentUuid != null ? Bukkit.getPlayer(opponentUuid) : null;
        player.sendMessage(Component.text("You lost the duel!", NamedTextColor.RED, TextDecoration.BOLD));
        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(Component.text("You won the duel against " + player.getName() + "!", NamedTextColor.GREEN, TextDecoration.BOLD));
            opponent.playSound(opponent.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getDuelManager().isInDuel(uuid)) {
            plugin.getDuelManager().endDuel(uuid, event.getPlayer().getName() + " disconnected - you win by forfeit.");
        }
        plugin.getDuelManager().clearPending(uuid);
    }
}
