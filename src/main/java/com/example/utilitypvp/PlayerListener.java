package com.example.utilitypvp;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

final class PlayerListener implements Listener {
    private final UtilityPvP plugin;

    PlayerListener(UtilityPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        plugin.getCombatManager().tag(attacker, victim);
        plugin.getWarmupManager().cancelOnDamage(victim);
        plugin.getWarmupManager().cancelOnDamage(attacker);
        if (plugin.getSettingsManager().isSoundAlertsEnabled(victim.getUniqueId())) {
            victim.playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.7f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getWarmupManager().cancelOnDamage(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().distanceSquared(event.getTo()) <= 0.01D) {
            return;
        }
        if (plugin.getWarmupManager().getOrigin(event.getPlayer().getUniqueId()) != null) {
            plugin.getWarmupManager().cancelOnMove(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getCombatManager().isTagged(player)) {
            return;
        }
        String command = event.getMessage().trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        String base = command.split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int namespace = base.indexOf(':');
        if (namespace >= 0) {
            base = base.substring(namespace + 1);
        }
        if (base.equals("tpa") || base.equals("tpaccept") || base.equals("home")
                || base.equals("sethome") || base.equals("spawn")) {
            event.setCancelled(true);
            player.sendMessage(net.kyori.adventure.text.Component.text("That command is blocked while combat tagged.", net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getWarmupManager().cancel(player.getUniqueId(), "");
        plugin.getTpaManager().handleQuit(player.getUniqueId());
        plugin.getCombatManager().handleQuit(player);
        plugin.getScoreboardService().remove(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getScoreboardService().update(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        event.viewers().removeIf(audience -> audience instanceof Player recipient
                && !recipient.getUniqueId().equals(sender.getUniqueId())
                && !plugin.getSettingsManager().isPublicChatVisible(recipient.getUniqueId()));
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
