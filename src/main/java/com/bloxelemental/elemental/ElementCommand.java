package com.bloxelemental.elemental;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ElementCommand implements CommandExecutor, TabCompleter {

    private final ElementalSMP plugin;

    public ElementCommand(ElementalSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /element gui | /element abilities | /element stats", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("abilities")) {
            GUIListener.openAbilitiesGUI(plugin, player);
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            handleStats(player);
            return true;
        }

        if (!args[0].equalsIgnoreCase("gui")) {
            player.sendMessage(Component.text("Usage: /element gui | /element abilities | /element stats", NamedTextColor.YELLOW));
            return true;
        }

        if (plugin.getMasteryManager().hasElement(player.getUniqueId())) {
            player.sendMessage(Component.text("You have already chosen the element of ", NamedTextColor.RED)
                    .append(Component.text(plugin.getMasteryManager().getElement(player.getUniqueId()).displayName(),
                            NamedTextColor.GOLD)));
            return true;
        }

        GUIListener.openElementSelectionGUI(player);
        return true;
    }

    private void handleStats(Player player) {
        MasteryManager manager = plugin.getMasteryManager();
        Element element = manager.getElement(player.getUniqueId());
        if (element == null) {
            player.sendMessage(Component.text("Choose an element with /element gui first.", NamedTextColor.RED));
            return;
        }
        int level = manager.getLevel(player.getUniqueId());
        double xp = manager.getXP(player.getUniqueId());
        double xpNeeded = manager.xpForNextLevel(level);

        player.sendMessage(Component.text("--- Your Stats ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Element: ", NamedTextColor.GRAY).append(Component.text(element.displayName(), element.color())));
        player.sendMessage(Component.text("Mastery Level: ", NamedTextColor.GRAY).append(Component.text(level + "/" + MasteryManager.MAX_LEVEL, NamedTextColor.WHITE)));
        if (level < MasteryManager.MAX_LEVEL) {
            player.sendMessage(Component.text("XP to next level: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.0f/%.0f", xp, xpNeeded), NamedTextColor.WHITE)));
        }
        player.sendMessage(Component.text("Kills: ", NamedTextColor.GRAY).append(Component.text(manager.getKills(player.getUniqueId()), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Chambers Cleared: ", NamedTextColor.GRAY).append(Component.text(manager.getChambersCleared(player.getUniqueId()), NamedTextColor.WHITE)));
        if (manager.isAwakeningEligible(player.getUniqueId())) {
            player.sendMessage(Component.text("Awakening Eligible! ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("Find a Storm Core or Void Tear.", NamedTextColor.GRAY)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("gui", "abilities", "stats");
        }
        return List.of();
    }
}
