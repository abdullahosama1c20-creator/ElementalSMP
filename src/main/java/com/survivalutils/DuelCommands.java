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
import java.util.UUID;

public class DuelCommands implements CommandExecutor, TabCompleter {

    private final SurvivalUtils plugin;

    public DuelCommands(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "duel" -> handleDuel(player, args);
            case "duelaccept" -> plugin.getDuelManager().accept(player);
            case "dueldeny" -> plugin.getDuelManager().deny(player);
            case "duelstats" -> handleStats(player, args);
            case "dueltop" -> handleTop(player);
            case "duelcancel" -> {
                if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                    UUID opponentUuid = plugin.getDuelManager().getOpponent(player.getUniqueId());
                    plugin.getDuelManager().endDuel(player.getUniqueId(), player.getName() + " forfeited.");
                    plugin.getDuelStatsManager().recordLoss(player.getUniqueId());
                    if (opponentUuid != null) {
                        plugin.getDuelStatsManager().recordWin(opponentUuid);
                    }
                    player.sendMessage(Component.text("You forfeited the duel.", NamedTextColor.YELLOW));
                } else {
                    plugin.getDuelManager().clearPending(player.getUniqueId());
                    player.sendMessage(Component.text("No active duel to cancel.", NamedTextColor.RED));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleDuel(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /duel <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("You can't duel yourself.", NamedTextColor.RED));
            return;
        }
        plugin.getDuelManager().sendRequest(player, target);
    }

    private void handleStats(Player player, String[] args) {
        UUID target;
        String label;
        if (args.length > 0) {
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            target = offline.getUniqueId();
            label = offline.getName() != null ? offline.getName() : args[0];
        } else {
            target = player.getUniqueId();
            label = player.getName();
        }
        int wins = plugin.getDuelStatsManager().getWins(target);
        int losses = plugin.getDuelStatsManager().getLosses(target);
        int total = wins + losses;
        double winRate = total == 0 ? 0.0 : (wins * 100.0) / total;

        player.sendMessage(Component.text("--- " + label + "'s Duel Record ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Wins: ", NamedTextColor.GRAY).append(Component.text(wins, NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Losses: ", NamedTextColor.GRAY).append(Component.text(losses, NamedTextColor.RED)));
        player.sendMessage(Component.text("Win rate: ", NamedTextColor.GRAY).append(Component.text(String.format("%.0f%%", winRate), NamedTextColor.WHITE)));
    }

    private void handleTop(Player player) {
        List<DuelStatsManager.DuelRecord> top = plugin.getDuelStatsManager().getTop(10);
        player.sendMessage(Component.text("--- Duel Leaderboard ---", NamedTextColor.GOLD));
        if (top.isEmpty()) {
            player.sendMessage(Component.text("No duels recorded yet.", NamedTextColor.GRAY));
            return;
        }
        int rank = 1;
        for (DuelStatsManager.DuelRecord record : top) {
            player.sendMessage(Component.text("#" + rank + " ", NamedTextColor.YELLOW)
                    .append(Component.text(record.name() + " ", NamedTextColor.WHITE))
                    .append(Component.text(record.wins() + "W", NamedTextColor.GREEN))
                    .append(Component.text(" / ", NamedTextColor.GRAY))
                    .append(Component.text(record.losses() + "L", NamedTextColor.RED)));
            rank++;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("duel") && args.length == 1) {
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
