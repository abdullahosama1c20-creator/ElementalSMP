package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpaCommands implements CommandExecutor, TabCompleter {

    private final SurvivalUtils plugin;

    public TpaCommands(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "tpa" -> handleTpa(player, args);
            case "tpaccept" -> plugin.getTpaManager().accept(player);
            case "tpdeny" -> plugin.getTpaManager().deny(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleTpa(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /tpa <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("You can't teleport to yourself.", NamedTextColor.RED));
            return;
        }
        plugin.getTpaManager().sendRequest(player, target);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("tpa") && args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
