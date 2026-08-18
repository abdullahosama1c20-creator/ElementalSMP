package com.survivalutils;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AfkListener implements Listener {

    private final SurvivalUtils plugin;

    public AfkListener(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getAfkManager().markActive(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Ignore pure look-changes (turning the camera) so players aren't kept
        // "active" just by having their client send tiny rotation packets.
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        plugin.getAfkManager().markActive(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        plugin.getAfkManager().markActive(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        plugin.getAfkManager().markActive(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getAfkManager().clear(event.getPlayer().getUniqueId());
    }
}
