package com.survivalutils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ScoreboardManager {

    private final SurvivalUtils plugin;

    public ScoreboardManager(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, 40L);
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getSettingsManager().isScoreboardEnabled(player.getUniqueId())) {
                show(player);
            }
        }
    }

    public void show(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("survivalutils", "dummy",
                ChatColor.GOLD + "" + ChatColor.BOLD + "SURVIVAL SMP");
        objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

        int line = 8;
        setLine(objective, line--, ChatColor.GRAY + "Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
        setLine(objective, line--, ChatColor.GRAY + "Ping: " + ChatColor.WHITE + player.getPing() + "ms");
        setLine(objective, line--, ChatColor.GRAY + "Time: " + ChatColor.WHITE + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        setLine(objective, line--, " ");
        setLine(objective, line--, ChatColor.GRAY + "Combat: " + (plugin.getCombatManager().isTagged(player.getUniqueId())
                ? ChatColor.RED + "TAGGED" : ChatColor.GREEN + "Safe"));

        plugin.getElementalBridge().getElementInfo(player.getUniqueId()).ifPresent(info ->
                setLine(objective, 3, ChatColor.GRAY + "Element: " + ChatColor.WHITE + info.elementName() + " Lv." + info.level()));

        if (plugin.getAfkManager().isAfk(player.getUniqueId())) {
            setLine(objective, 2, ChatColor.YELLOW + "AFK");
        }

        player.setScoreboard(scoreboard);
    }

    private void setLine(Objective objective, int line, String text) {
        // Scoreboard lines must be unique strings; pad with invisible color codes if needed.
        Score score = objective.getScore(text.length() > 0 ? text : " ");
        score.setScore(line);
    }

    public void hide(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
