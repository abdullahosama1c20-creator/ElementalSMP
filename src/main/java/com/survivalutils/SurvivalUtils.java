package com.survivalutils;

import org.bukkit.plugin.java.JavaPlugin;

public final class SurvivalUtils extends JavaPlugin {

    private HomeManager homeManager;
    private TpaManager tpaManager;
    private CombatManager combatManager;
    private SettingsManager settingsManager;
    private RulesManager rulesManager;
    private TeleportWarmupManager teleportWarmupManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        homeManager = new HomeManager(this);
        homeManager.load();

        settingsManager = new SettingsManager(this);
        settingsManager.load();

        rulesManager = new RulesManager(this);
        rulesManager.load();

        combatManager = new CombatManager();
        tpaManager = new TpaManager(this);
        teleportWarmupManager = new TeleportWarmupManager(this);
        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.start();

        getServer().getPluginManager().registerEvents(teleportWarmupManager, this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new SettingsGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatVisibilityListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        HomeCommands homeCommands = new HomeCommands(this);
        getCommand("sethome").setExecutor(homeCommands);
        getCommand("home").setExecutor(homeCommands);
        getCommand("home").setTabCompleter(homeCommands);
        getCommand("delhome").setExecutor(homeCommands);
        getCommand("delhome").setTabCompleter(homeCommands);

        TpaCommands tpaCommands = new TpaCommands(this);
        getCommand("tpa").setExecutor(tpaCommands);
        getCommand("tpa").setTabCompleter(tpaCommands);
        getCommand("tpaccept").setExecutor(tpaCommands);
        getCommand("tpdeny").setExecutor(tpaCommands);

        getCommand("settings").setExecutor(new SettingsCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("rules").setExecutor(new RulesCommand(this));
        getCommand("/rules").setExecutor(new RulesEditCommand(this));

        getLogger().info("SurvivalUtils has been enabled.");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            homeManager.save();
        }
        if (settingsManager != null) {
            settingsManager.save();
        }
        if (rulesManager != null) {
            rulesManager.save();
        }
        getLogger().info("SurvivalUtils has been disabled.");
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public RulesManager getRulesManager() {
        return rulesManager;
    }

    public TeleportWarmupManager getTeleportWarmupManager() {
        return teleportWarmupManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
