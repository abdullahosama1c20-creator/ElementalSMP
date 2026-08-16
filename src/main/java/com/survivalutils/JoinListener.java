package com.survivalutils;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final SurvivalUtils plugin;

    public JoinListener(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getSettingsManager().isScoreboardEnabled(event.getPlayer().getUniqueId())) {
            plugin.getScoreboardManager().show(event.getPlayer());
        }
    }
}
