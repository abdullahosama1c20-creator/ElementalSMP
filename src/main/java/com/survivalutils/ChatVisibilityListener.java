package com.survivalutils;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatVisibilityListener implements Listener {

    private final SurvivalUtils plugin;

    public ChatVisibilityListener(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.viewers().removeIf(viewer -> {
            if (!(viewer instanceof Player player)) {
                return false;
            }
            if (player.equals(event.getPlayer())) {
                return false;
            }
            return !plugin.getSettingsManager().isPublicChatEnabled(player.getUniqueId());
        });
    }
}
