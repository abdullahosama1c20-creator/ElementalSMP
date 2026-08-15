package com.example.utilitypvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CommandHandler implements CommandExecutor, TabCompleter {
    private final UtilityPvP plugin;

    CommandHandler(UtilityPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command is player-only.", NamedTextColor.RED));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "sethome" -> setHome(player, args);
            case "home" -> home(player, args);
            case "delhome" -> deleteHome(player, args);
            case "tpa" -> tpa(player, args);
            case "tpaccept" -> accept(player);
            case "tpdeny" -> deny(player);
            case "spawn" -> spawn(player);
            case "settings" -> plugin.getSettingsManager().openMenu(player);
            default -> player.sendMessage(Component.text("Unknown command.", NamedTextColor.RED));
        }
        return true;
    }

    private void setHome(Player player, String[] args) {
        if (blocked(player)) return;
        String name = args.length == 0 ? "home" : args[0].toLowerCase();
        if (!plugin.getHomeManager().isValidName(name)) {
            player.sendMessage(Component.text("Home names may contain only letters, numbers, _ and -, up to 16 characters.", NamedTextColor.RED));
            return;
        }
        plugin.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation());
        player.sendMessage(Component.text("Home '" + name + "' set.", NamedTextColor.GREEN));
    }

    private void home(Player player, String[] args) {
        if (blocked(player)) return;
        String name = args.length == 0 ? "home" : args[0].toLowerCase();
        Location location = plugin.getHomeManager().getHome(player.getUniqueId(), name);
        if (location == null || location.getWorld() == null) {
            player.sendMessage(Component.text("That home does not exist.", NamedTextColor.RED));
            return;
        }
        plugin.getWarmupManager().begin(player, location, "home '" + name + "'");
    }

    private void deleteHome(Player player, String[] args) {
        if (blocked(player)) return;
        String name = args.length == 0 ? "home" : args[0].toLowerCase();
        if (plugin.getHomeManager().deleteHome(player.getUniqueId(), name)) {
            player.sendMessage(Component.text("Home '" + name + "' deleted.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("That home does not exist.", NamedTextColor.RED));
        }
    }

    private void tpa(Player player, String[] args) {
        if (blocked(player)) return;
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /tpa <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("That player is not online or does not exist.", NamedTextColor.RED));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("You cannot TPA to yourself.", NamedTextColor.RED));
            return;
        }
        plugin.getTpaManager().request(player, target);
    }

    private void accept(Player player) {
        if (blocked(player)) return;
        plugin.getTpaManager().accept(player);
    }

    private void deny(Player player) {
        plugin.getTpaManager().deny(player);
    }

    private void spawn(Player player) {
        if (blocked(player)) return;
        World world = player.getWorld();
        plugin.getWarmupManager().begin(player, world.getSpawnLocation(), "spawn");
    }

    private boolean blocked(Player player) {
        if (plugin.getCombatManager().isTagged(player)) {
            player.sendMessage(Component.text("You cannot use that command while combat tagged.", NamedTextColor.RED));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        String name = command.getName().toLowerCase();
        if (name.equals("tpa") && args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player) && target.getName().toLowerCase().startsWith(prefix)) {
                    result.add(target.getName());
                }
            }
            return result;
        }
        if ((name.equals("home") || name.equals("delhome")) && args.length == 1) {
            String prefix = args[0].toLowerCase();
            return plugin.getHomeManager().getHomeNames(player.getUniqueId()).stream()
                    .filter(home -> home.startsWith(prefix))
                    .sorted()
                    .toList();
        }
        return Collections.emptyList();
    }
}
