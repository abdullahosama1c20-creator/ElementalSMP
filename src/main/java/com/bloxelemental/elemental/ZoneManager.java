package com.bloxelemental.elemental;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

/**
 * Every 60 minutes, opens a random 15-block radius "Mastery Zone" in the main
 * world. Players fighting inside the zone earn double Mastery XP. A persistent
 * boss bar is shown to everyone online with the zone's live coordinates.
 */
public class ZoneManager {

    private static final int ZONE_RADIUS = 15;
    private static final long PERIOD_TICKS = 20L * 60L * 60L; // 60 minutes
    private static final int WORLD_BOUND = 5000;

    private final ElementalSMP plugin;
    private final Random random = new Random();
    private final BossBar bossBar = BossBar.bossBar(
            Component.text("No active Mastery Zone yet...", NamedTextColor.GRAY),
            0.0F,
            BossBar.Color.PURPLE,
            BossBar.Overlay.PROGRESS
    );

    private Location zoneCenter;
    private BukkitTask task;

    public ZoneManager(ElementalSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::rollNewZone, 0L, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    private void rollNewZone() {
        World world = Bukkit.getWorlds().get(0);
        int x = random.nextInt(WORLD_BOUND * 2) - WORLD_BOUND;
        int z = random.nextInt(WORLD_BOUND * 2) - WORLD_BOUND;
        int y = world.getHighestBlockYAt(x, z) + 1;
        zoneCenter = new Location(world, x, y, z);

        Component title = Component.text("2x MASTERY ZONE ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text("@ (" + x + ", " + y + ", " + z + ") r" + ZONE_RADIUS, NamedTextColor.WHITE));
        bossBar.name(title);
        bossBar.progress(1.0F);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6F, 1.4F);
            player.sendMessage(title);
        }
    }

    /**
     * Ensures a newly joined player also sees the currently active boss bar.
     */
    public void trackPlayer(Player player) {
        if (zoneCenter != null) {
            player.showBossBar(bossBar);
        }
    }

    public void untrackPlayer(Player player) {
        player.hideBossBar(bossBar);
    }

    public boolean isInZone(Location location) {
        if (zoneCenter == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().equals(zoneCenter.getWorld())) {
            return false;
        }
        double dx = location.getX() - zoneCenter.getX();
        double dz = location.getZ() - zoneCenter.getZ();
        return (dx * dx + dz * dz) <= (ZONE_RADIUS * ZONE_RADIUS);
    }

    public Location getZoneCenter() {
        return zoneCenter;
    }
}
