package com.survivalutils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Vanilla servers can't detect an arbitrary keypress like "G" - that's a
 * client-side binding Minecraft doesn't report to the server unless it's
 * tied to a real action. The closest fit is Swap Offhand Item (default key
 * F), which IS a real server event. Any player can rebind F to G (or
 * anything else) in their own Controls settings and this will still fire.
 */
public class SwapHandsSettingsListener implements Listener {

    private final SurvivalUtils plugin;

    public SwapHandsSettingsListener(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        SettingsGUIListener.open(plugin, player);
    }
}
