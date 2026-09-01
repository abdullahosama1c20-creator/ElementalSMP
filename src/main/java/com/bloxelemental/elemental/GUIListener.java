package com.bloxelemental.elemental;

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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders and handles clicks for the /element gui starter selection menu.
 */
public class GUIListener implements Listener {

    /** Marks inventories opened by this plugin so we never mistake a player's own chest GUI for ours. */
    public static class ElementGuiHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** Marks the read-only /element abilities GUI so clicks can be safely cancelled. */
    public static class AbilitiesGuiHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final Map<Integer, Element> SLOT_MAP = new HashMap<>();
    static {
        SLOT_MAP.put(2, Element.FIRE);
        SLOT_MAP.put(3, Element.WATER);
        SLOT_MAP.put(5, Element.AIR);
        SLOT_MAP.put(6, Element.EARTH);
    }

    private final ElementalSMP plugin;

    public GUIListener(ElementalSMP plugin) {
        this.plugin = plugin;
    }

    public static void openElementSelectionGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(new ElementGuiHolder(), 9,
                Component.text("Choose Your Element", NamedTextColor.GOLD, TextDecoration.BOLD));

        ItemStack filler = namedItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }

        for (Map.Entry<Integer, Element> entry : SLOT_MAP.entrySet()) {
            Element element = entry.getValue();
            ItemStack item = namedItem(element.icon(),
                    Component.text(element.displayName(), element.color(), TextDecoration.BOLD));
            ItemMeta meta = item.getItemMeta();
            meta.lore(java.util.List.of(
                    Component.text("Click to bind this element", NamedTextColor.GRAY),
                    Component.text("as your Starter Element.", NamedTextColor.GRAY)
            ));
            item.setItemMeta(meta);
            inventory.setItem(entry.getKey(), item);
        }

        player.openInventory(inventory);
    }

    private static ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Shows a compact 9-slot readout of a player's element: the four ability
     * tiers plus their passive, each colored by whether it's unlocked at the
     * player's current Mastery level. Replaces the old chat-spam listing.
     */
    public static void openAbilitiesGUI(ElementalSMP plugin, Player player) {
        MasteryManager manager = plugin.getMasteryManager();
        Element element = manager.getElement(player.getUniqueId());
        if (element == null) {
            player.sendMessage(Component.text("Choose an element with /element gui first.", NamedTextColor.RED));
            return;
        }
        int level = manager.getLevel(player.getUniqueId());

        Inventory inventory = Bukkit.createInventory(new AbilitiesGuiHolder(), 9,
                Component.text(element.displayName() + " Abilities - Lv." + level, element.color(), TextDecoration.BOLD));

        ItemStack filler = namedItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(1, abilityIcon(element, level, Material.IRON_SWORD, Tier.BASIC));
        inventory.setItem(3, abilityIcon(element, level, Material.FEATHER, Tier.MOBILITY));
        inventory.setItem(4, passiveIcon(element));
        inventory.setItem(5, abilityIcon(element, level, Material.BLAZE_POWDER, Tier.HEAVY));
        inventory.setItem(7, abilityIcon(element, level, Material.NETHER_STAR, Tier.ULTIMATE));

        player.openInventory(inventory);
    }

    private static ItemStack abilityIcon(Element element, int level, Material material, Tier tier) {
        boolean unlocked = level >= tier.requiredLevel;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text((unlocked ? "" : "[LOCKED] ") + tier.label, unlocked ? element.color() : NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text(AbilityInfo.describe(element, tier), unlocked ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY),
                Component.text(""),
                Component.text("Requires Lv." + tier.requiredLevel + " | " + tier.cooldownSeconds + "s cooldown",
                        unlocked ? NamedTextColor.GREEN : NamedTextColor.RED)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack passiveIcon(Element element) {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Passive", element.color(), TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text(PassiveInfo.describe(element), NamedTextColor.GRAY),
                Component.text(""),
                Component.text("Always active", NamedTextColor.GREEN)
        ));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof AbilitiesGuiHolder) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ElementGuiHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Element chosen = SLOT_MAP.get(event.getRawSlot());
        if (chosen == null) {
            return;
        }

        if (plugin.getMasteryManager().hasElement(player.getUniqueId())) {
            player.sendMessage(Component.text("You have already chosen an element. Ask an admin to reset it if needed.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        plugin.getMasteryManager().setElement(player.getUniqueId(), chosen);
        player.getInventory().addItem(AbilityListener.catalystItem(plugin, chosen));
        player.getInventory().addItem(ArmorSets.armorPieces(plugin, chosen));
        PassiveInfo.applyBuffs(player, chosen);
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        player.sendMessage(Component.text("You have bound yourself to the element of ", NamedTextColor.GREEN)
                .append(Component.text(chosen.displayName(), chosen.color(), TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Your Elemental Catalyst has been added to your inventory.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("A matching armor set was added too - wear the full set for a bonus.", NamedTextColor.GRAY));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getChamberManager() != null) {
            plugin.getChamberManager().trackPlayer(event.getPlayer());
        }
        if (!plugin.getMasteryManager().hasElement(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(Component.text("Welcome! Run ", NamedTextColor.YELLOW)
                    .append(Component.text("/element gui", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" to choose your starter element.", NamedTextColor.YELLOW)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getChamberManager() != null) {
            plugin.getChamberManager().untrackPlayer(event.getPlayer());
        }
    }
}
