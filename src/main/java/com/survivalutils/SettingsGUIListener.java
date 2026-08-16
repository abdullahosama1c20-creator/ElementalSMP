package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsGUIListener implements Listener {

    private static class SettingsGuiHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record ToggleSlot(int slot, SettingsManager.Toggle toggle, Material icon, String title, String description) {
    }

    private static final List<ToggleSlot> SLOTS = List.of(
            new ToggleSlot(10, SettingsManager.Toggle.AUTO_ACCEPT_TPA, Material.ENDER_PEARL, "Auto-Accept TPA", "Automatically accept incoming teleport requests."),
            new ToggleSlot(12, SettingsManager.Toggle.PUBLIC_CHAT, Material.PAPER, "Public Chat Visibility", "See messages sent in public chat."),
            new ToggleSlot(14, SettingsManager.Toggle.SCOREBOARD, Material.OAK_SIGN, "Scoreboard", "Show the sidebar scoreboard."),
            new ToggleSlot(16, SettingsManager.Toggle.DUEL_REQUESTS, Material.IRON_SWORD, "Duel Requests", "Allow other players to challenge you to a duel."),
            new ToggleSlot(22, SettingsManager.Toggle.SOUND_ALERTS, Material.NOTE_BLOCK, "Sound Alerts", "Play sounds for TPA requests and other alerts.")
    );

    private final SurvivalUtils plugin;

    public SettingsGUIListener(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    public static void open(SurvivalUtils plugin, Player player) {
        Inventory inventory = Bukkit.createInventory(new SettingsGuiHolder(), 36,
                Component.text("Settings", NamedTextColor.GOLD, TextDecoration.BOLD));

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        for (ToggleSlot slot : SLOTS) {
            inventory.setItem(slot.slot(), buildItem(plugin, player, slot));
        }

        player.openInventory(inventory);
    }

    private static ItemStack buildItem(SurvivalUtils plugin, Player player, ToggleSlot slot) {
        boolean enabled = plugin.getSettingsManager().get(player.getUniqueId(), slot.toggle());
        ItemStack item = new ItemStack(slot.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(slot.title(), enabled ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text(slot.description(), NamedTextColor.GRAY),
                Component.text(""),
                Component.text("Status: ", NamedTextColor.GRAY)
                        .append(Component.text(enabled ? "ENABLED" : "DISABLED", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)),
                Component.text("Click to toggle", NamedTextColor.DARK_GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SettingsGuiHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        for (ToggleSlot slot : SLOTS) {
            if (slot.slot() == event.getRawSlot()) {
                plugin.getSettingsManager().toggle(player.getUniqueId(), slot.toggle());
                event.getInventory().setItem(slot.slot(), buildItem(plugin, player, slot));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);

                if (slot.toggle() == SettingsManager.Toggle.SCOREBOARD) {
                    boolean enabled = plugin.getSettingsManager().isScoreboardEnabled(player.getUniqueId());
                    if (enabled) {
                        plugin.getScoreboardManager().show(player);
                    } else {
                        plugin.getScoreboardManager().hide(player);
                    }
                }
                return;
            }
        }
    }
}
