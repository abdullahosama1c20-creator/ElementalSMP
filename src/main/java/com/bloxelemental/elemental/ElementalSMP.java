package com.bloxelemental.elemental;

import org.bukkit.plugin.java.JavaPlugin;

public final class ElementalSMP extends JavaPlugin {

    private static ElementalSMP instance;

    private MasteryManager masteryManager;
    private ZoneManager zoneManager;

    @Override
    public void onEnable() {
        instance = this;

        masteryManager = new MasteryManager(this);
        masteryManager.loadData();

        zoneManager = new ZoneManager(this);
        zoneManager.start();

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);

        getCommand("element").setExecutor(new ElementCommand(this));
        AdminCommandHandler adminHandler = new AdminCommandHandler(this);
        getCommand("elemental").setExecutor(adminHandler);
        getCommand("elemental").setTabCompleter(adminHandler);

        getLogger().info("ElementalSMP has been enabled. " + masteryManager.getClass().getSimpleName() + " ready.");
    }

    @Override
    public void onDisable() {
        if (masteryManager != null) {
            masteryManager.saveData();
        }
        if (zoneManager != null) {
            zoneManager.stop();
        }
        getLogger().info("ElementalSMP has been disabled.");
    }

    public static ElementalSMP getInstance() {
        return instance;
    }

    public MasteryManager getMasteryManager() {
        return masteryManager;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }
}
