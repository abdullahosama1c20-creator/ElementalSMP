package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class CombatListener implements Listener {

    /** Command labels (without the leading slash) blocked while a player is combat tagged. */
    private static final Set<String> BLOCKED_COMMANDS = Set.of(
            "tpa", "tpaccept", "tpdeny", "home", "sethome", "spawn"
    );

    private final SurvivalUtils plugin;

    public CombatListener(SurvivalUtils plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickActionBars, 0L, 20L);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = null;
        Player attacker = null;

        if (event.getEntity() instanceof Player p) {
            victim = p;
        }

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) {
                attacker = p;
            }
        }

        if (victim == null || attacker == null || victim.equals(attacker)) {
            return;
        }

        plugin.getCombatManager().tag(victim.getUniqueId());
        plugin.getCombatManager().tag(attacker.getUniqueId());
    }

    private void tickActionBars() {
        for (UUID uuid : Set.copyOf(plugin.getCombatManager().getAllTagged().keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            long remainingMs = plugin.getCombatManager().remainingMillis(uuid);
            if (remainingMs <= 0) {
                continue;
            }
            player.sendActionBar(Component.text(String.format("\u2694 Combat Tagged: %.1fs", remainingMs / 1000.0), NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getCombatManager().isTagged(player.getUniqueId())) {
            return;
        }
        String[] parts = event.getMessage().substring(1).split(" ");
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (BLOCKED_COMMANDS.contains(label)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot use that command while in combat!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getCombatManager().isTagged(player.getUniqueId())) {
            return;
        }
        // Combat logging penalty: kill them so their inventory drops naturally, exactly
        // as if they'd died fighting instead of disconnecting to escape.
        player.setHealth(0.0);
        plugin.getCombatManager().clear(player.getUniqueId());
        Bukkit.broadcast(Component.text(player.getName() + " combat logged and was slain for it.", NamedTextColor.DARK_RED));
    }
}
