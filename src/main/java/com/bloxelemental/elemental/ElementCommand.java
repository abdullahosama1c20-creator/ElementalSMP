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
            player.sendMessage(Component.text("Usage: /element gui | /element abilities", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("abilities")) {
            GUIListener.openAbilitiesGUI(plugin, player);
            return true;
        }

        if (!args[0].equalsIgnoreCase("gui")) {
            player.sendMessage(Component.text("Usage: /element gui | /element abilities", NamedTextColor.YELLOW));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("gui", "abilities");
        }
        return List.of();
    }
}
