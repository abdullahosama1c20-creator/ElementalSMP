package com.example.utilitypvp;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

final class SettingsMenuListener implements Listener {
    private final UtilityPvP plugin;

    SettingsMenuListener(UtilityPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        if (!plugin.getSettingsManager().isSettingsInventory(event.getView().getTopInventory())) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        plugin.getSettingsManager().handleClick(player, event.getSlot());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (plugin.getSettingsManager().isSettingsInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
