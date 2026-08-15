package com.example.utilitypvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UtilityPvP extends JavaPlugin {
    private HomeManager homeManager;
    private SettingsManager settingsManager;
    private CombatManager combatManager;
    private TpaManager tpaManager;
    private TeleportWarmupManager warmupManager;
    private ScoreboardService scoreboardService;
    private BukkitTask tpaCleanupTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.settingsManager = new SettingsManager(this);
        this.settingsManager.load();
        this.homeManager = new HomeManager(this);
        this.homeManager.load();
        this.combatManager = new CombatManager(this);
        this.warmupManager = new TeleportWarmupManager(this, combatManager);
        this.tpaManager = new TpaManager(this, combatManager, settingsManager);
        this.scoreboardService = new ScoreboardService(this, settingsManager, combatManager);

        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("sethome").setExecutor(commandHandler);
        getCommand("sethome").setTabCompleter(commandHandler);
        getCommand("home").setExecutor(commandHandler);
        getCommand("home").setTabCompleter(commandHandler);
        getCommand("delhome").setExecutor(commandHandler);
        getCommand("delhome").setTabCompleter(commandHandler);
        getCommand("tpa").setExecutor(commandHandler);
        getCommand("tpa").setTabCompleter(commandHandler);
        getCommand("tpaccept").setExecutor(commandHandler);
        getCommand("tpdeny").setExecutor(commandHandler);
        getCommand("spawn").setExecutor(commandHandler);
        getCommand("settings").setExecutor(commandHandler);
        getCommand("settings").setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new SettingsMenuListener(this), this);

        this.combatManager.start();
        this.scoreboardService.start();

        this.tpaCleanupTask = Bukkit.getScheduler().runTaskTimer(this, tpaManager::cleanupExpired, 20L, 20L);
        getLogger().info("UtilityPvP enabled.");
    }

    @Override
    public void onDisable() {
        if (tpaCleanupTask != null) {
            tpaCleanupTask.cancel();
        }
        if (warmupManager != null) {
            warmupManager.cancelAll();
        }
        if (tpaManager != null) {
            tpaManager.clearAll();
        }
        if (combatManager != null) {
            combatManager.shutdown();
        }
        if (settingsManager != null) {
            settingsManager.save();
        }
        if (homeManager != null) {
            homeManager.save();
        }
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public TeleportWarmupManager getWarmupManager() {
        return warmupManager;
    }

    public ScoreboardService getScoreboardService() {
        return scoreboardService;
    }
}

final class ScoreboardService {
    private final UtilityPvP plugin;
    private final SettingsManager settings;
    private final CombatManager combat;
    private final ScoreboardManager manager;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    ScoreboardService(UtilityPvP plugin, SettingsManager settings, CombatManager combat) {
        this.plugin = plugin;
        this.settings = settings;
        this.combat = combat;
        this.manager = Bukkit.getScoreboardManager();
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                update(player);
            }
        }, 20L, 20L);
    }

    void update(Player player) {
        if (!settings.isScoreboardEnabled(player.getUniqueId())) {
            player.setScoreboard(manager.getMainScoreboard());
            boards.remove(player.getUniqueId());
            return;
        }

        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), key -> createBoard());
        Objective objective = board.getObjective("utilitypvp");
        if (objective == null) {
            board = createBoard();
            boards.put(player.getUniqueId(), board);
            objective = board.getObjective("utilitypvp");
        }

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        objective.getScore("§0UtilityPvP").setScore(5);
        objective.getScore("Online: " + Bukkit.getOnlinePlayers().size()).setScore(4);
        objective.getScore("§1UtilityPvP").setScore(3);
        long combatSeconds = combat.getRemainingSeconds(player.getUniqueId());
        objective.getScore(combatSeconds > 0 ? "Combat: " + combatSeconds + "s" : "Combat: safe").setScore(2);
        objective.getScore("§2UtilityPvP").setScore(1);

        player.setScoreboard(board);
    }

    void remove(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(manager.getMainScoreboard());
    }

    private Scoreboard createBoard() {
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective(
                "utilitypvp",
                org.bukkit.scoreboard.Criteria.DUMMY,
                Component.text("UtilityPvP", NamedTextColor.AQUA),
                RenderType.INTEGER
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return board;
    }
}
