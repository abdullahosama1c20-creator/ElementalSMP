package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class HomeCommands implements CommandExecutor, TabCompleter {

    private final SurvivalUtils plugin;

    public HomeCommands(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        String homeName = args.length > 0 ? args[0] : HomeManager.DEFAULT_HOME;

        switch (command.getName().toLowerCase()) {
            case "sethome" -> handleSetHome(player, homeName);
            case "home" -> handleHome(player, homeName);
            case "delhome" -> handleDelHome(player, homeName);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleSetHome(Player player, String name) {
        boolean ok = plugin.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation());
        if (!ok) {
            player.sendMessage(Component.text("You've reached the maximum number of homes.", NamedTextColor.RED));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0F, 1.0F);
        player.sendMessage(Component.text("Home '" + name.toLowerCase() + "' set!", NamedTextColor.GREEN));
    }

    private void handleHome(Player player, String name) {
        if (plugin.getCombatManager().isTagged(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot teleport home while in combat!", NamedTextColor.RED));
            return;
        }
        Location home = plugin.getHomeManager().getHome(player.getUniqueId(), name);
        if (home == null) {
            player.sendMessage(Component.text("You don't have a home named '" + name.toLowerCase() + "'.", NamedTextColor.RED));
            return;
        }
        if (plugin.getTeleportWarmupManager().isPending(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have a teleport pending.", NamedTextColor.RED));
            return;
        }
        plugin.getTeleportWarmupManager().startWarmup(player, "home '" + name.toLowerCase() + "'", () -> {
            player.teleport(home);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            player.sendMessage(Component.text("Teleported home!", NamedTextColor.GREEN));
        });
    }

    private void handleDelHome(Player player, String name) {
        boolean ok = plugin.getHomeManager().deleteHome(player.getUniqueId(), name);
        if (!ok) {
            player.sendMessage(Component.text("You don't have a home named '" + name.toLowerCase() + "'.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Home '" + name.toLowerCase() + "' deleted.", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return plugin.getHomeManager().getHomeNames(player.getUniqueId());
        }
        return List.of();
    }
}
