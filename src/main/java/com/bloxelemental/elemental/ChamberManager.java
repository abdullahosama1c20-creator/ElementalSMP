package com.bloxelemental.elemental;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Owns the single active elemental chamber: a pre-designed, code-authored
 * structure (fixed layout per element) that spawns real vanilla spawners
 * with re-themed mobs. Clearing enough mobs unlocks a themed loot chest.
 * Rolls a new chamber (random element, random location) every 60 minutes,
 * or on demand via forceReroll().
 */
public class ChamberManager {

    private static final int INTERIOR = 9;                 // interior floor is INTERIOR x INTERIOR
    private static final int SPAN = INTERIOR + 2;           // + 1 block wall on each side
    private static final int INTERIOR_HEIGHT = 5;
    private static final int HEIGHT_SPAN = INTERIOR_HEIGHT + 2; // + floor + ceiling
    private static final long PERIOD_TICKS = 20L * 60L * 60L; // 60 minutes
    private static final int WORLD_BOUND = 5000;
    private static final int KILLS_REQUIRED = 12;

    private final ElementalSMP plugin;
    private final Random random = new Random();

    private final BossBar bossBar = BossBar.bossBar(
            Component.text("No active Elemental Chamber yet...", NamedTextColor.GRAY),
            0.0F,
            BossBar.Color.PURPLE,
            BossBar.Overlay.PROGRESS
    );

    private BukkitTask task;
    private Location origin;
    private ChamberTheme activeTheme;
    private final List<Location> spawnerLocations = new ArrayList<>();
    private Location chestLocation;
    private int killsRegistered;
    private boolean cleared;

    public ChamberManager(ElementalSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::rollNewChamber, 0L, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    public void forceReroll() {
        rollNewChamber();
    }

    // ---------------------------------------------------------------------
    // Building
    // ---------------------------------------------------------------------

    private void rollNewChamber() {
        Element element = Element.values()[random.nextInt(Element.values().length)];
        ChamberTheme theme = ChamberTheme.forElement(element);
        World world = Bukkit.getWorlds().get(0);

        int x = random.nextInt(WORLD_BOUND * 2) - WORLD_BOUND;
        int z = random.nextInt(WORLD_BOUND * 2) - WORLD_BOUND;
        int y = switch (theme.yPlacement()) {
            case UNDERGROUND_MID -> 35;
            case UNDERGROUND_DEEP -> 10;
            case SURFACE -> Math.max(world.getHighestBlockYAt(x, z) + 1, 64);
            case SKY -> 200;
        };

        this.origin = new Location(world, x, y, z);
        this.activeTheme = theme;
        this.spawnerLocations.clear();
        this.chestLocation = null;
        this.killsRegistered = 0;
        this.cleared = false;

        build(theme);

        Component title = Component.text("ELEMENTAL CHAMBER ", theme.element().color(), TextDecoration.BOLD)
                .append(Component.text(theme.element().displayName() + " @ (" + x + ", " + y + ", " + z + ")", NamedTextColor.WHITE));
        bossBar.name(title);
        bossBar.progress(1.0F);
        bossBar.color(bossBarColorFor(theme.element()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5F, 1.2F);
            player.sendMessage(title);
        }
    }

    private BossBar.Color bossBarColorFor(Element element) {
        return switch (element) {
            case FIRE -> BossBar.Color.RED;
            case WATER -> BossBar.Color.BLUE;
            case AIR -> BossBar.Color.WHITE;
            case EARTH -> BossBar.Color.GREEN;
            case LIGHTNING -> BossBar.Color.YELLOW;
            case VOID -> BossBar.Color.PURPLE;
        };
    }

    private void build(ChamberTheme theme) {
        World world = origin.getWorld();
        int baseX = origin.getBlockX() - (SPAN / 2);
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ() - (SPAN / 2);

        for (int x = 0; x < SPAN; x++) {
            for (int z = 0; z < SPAN; z++) {
                for (int y = 0; y < HEIGHT_SPAN; y++) {
                    boolean isFloor = y == 0;
                    boolean isCeiling = y == HEIGHT_SPAN - 1;
                    boolean isWallX = x == 0 || x == SPAN - 1;
                    boolean isWallZ = z == 0 || z == SPAN - 1;
                    boolean isBoundary = isFloor || isCeiling || isWallX || isWallZ;

                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    boolean isDoorway = isWallZ && z == 0 && x >= 4 && x <= 6 && y >= 1 && y <= 3;

                    if (isDoorway) {
                        block.setType(Material.AIR);
                        continue;
                    }

                    if (!isBoundary) {
                        block.setType(theme.floodInterior() ? Material.WATER : Material.AIR);
                        continue;
                    }

                    if (isFloor) {
                        block.setType(theme.floorMaterial());
                    } else if (isCeiling) {
                        block.setType(theme.openCeiling() ? Material.AIR : theme.ceilingMaterial());
                    } else {
                        block.setType(theme.wallMaterial());
                    }
                }
            }
        }

        // Corner pillars
        int[][] pillarCorners = {{1, 1}, {1, SPAN - 2}, {SPAN - 2, 1}, {SPAN - 2, SPAN - 2}};
        for (int[] corner : pillarCorners) {
            for (int y = 1; y < HEIGHT_SPAN - 1; y++) {
                world.getBlockAt(baseX + corner[0], baseY + y, baseZ + corner[1]).setType(theme.accentMaterial());
            }
        }

        // Wall lighting
        world.getBlockAt(baseX, baseY + 3, baseZ + SPAN / 2).setType(theme.lightMaterial());
        world.getBlockAt(baseX + SPAN - 1, baseY + 3, baseZ + SPAN / 2).setType(theme.lightMaterial());

        // Spawners
        placeSpawner(world, baseX + 3, baseY + 1, baseZ + 5, theme);
        placeSpawner(world, baseX + 7, baseY + 1, baseZ + 5, theme);

        // Chest alcove near the back wall (loot appears once the chamber is cleared)
        chestLocation = new Location(world, baseX + SPAN / 2, baseY + 1, baseZ + SPAN - 3);
    }

    private void placeSpawner(World world, int x, int y, int z, ChamberTheme theme) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.SPAWNER);
        if (block.getState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(theme.mobTypes()[random.nextInt(theme.mobTypes().length)]);
            spawner.setSpawnCount(1);
            spawner.setMaxNearbyEntities(6);
            spawner.setRequiredPlayerRange(16);
            spawner.setDelay(200);
            spawner.setMinSpawnDelay(200);
            spawner.setMaxSpawnDelay(600);
            spawner.update(true);
        }
        spawnerLocations.add(block.getLocation());
    }

    // ---------------------------------------------------------------------
    // State queries
    // ---------------------------------------------------------------------

    public boolean isInChamber(Location loc) {
        if (origin == null || loc.getWorld() == null || !loc.getWorld().equals(origin.getWorld())) {
            return false;
        }
        int halfSpan = SPAN / 2 + 1;
        double dx = Math.abs(loc.getX() - origin.getX());
        double dz = Math.abs(loc.getZ() - origin.getZ());
        double dy = loc.getY() - origin.getY();
        return dx <= halfSpan && dz <= halfSpan && dy >= -1 && dy <= HEIGHT_SPAN + 1;
    }

    public boolean isSpawnerLocation(Location loc) {
        for (Location spawnerLoc : spawnerLocations) {
            if (spawnerLoc.getBlockX() == loc.getBlockX() && spawnerLoc.getBlockY() == loc.getBlockY()
                    && spawnerLoc.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * XP multiplier applied by MasteryManager.addXP for combat happening inside
     * the active chamber: elements matching the chamber's element get the
     * biggest bonus, mismatched elements still get a smaller one, and
     * everywhere else is a plain 1x.
     */
    public double getBonusMultiplier(Location loc, Element playerElement) {
        if (!isInChamber(loc) || activeTheme == null) {
            return 1.0D;
        }
        return playerElement == activeTheme.element() ? 3.0D : 1.5D;
    }

    public Element getActiveElement() {
        return activeTheme == null ? null : activeTheme.element();
    }

    public Location getChamberCenter() {
        return origin;
    }

    public int getRadius() {
        return SPAN / 2 + 1;
    }

    public int getKillsRegistered() {
        return killsRegistered;
    }

    public int getKillsRequired() {
        return KILLS_REQUIRED;
    }

    public boolean isCleared() {
        return cleared;
    }

    /**
     * Called by ChamberListener whenever a chamber-spawned mob dies. Returns
     * true if this kill was the one that cleared the chamber (so the caller
     * can announce it).
     */
    public boolean registerKill(Player killer) {
        if (cleared || activeTheme == null) {
            return false;
        }
        killsRegistered++;
        if (killsRegistered < KILLS_REQUIRED) {
            killer.sendActionBar(Component.text("Chamber progress: " + killsRegistered + "/" + KILLS_REQUIRED, NamedTextColor.LIGHT_PURPLE));
            return false;
        }
        cleared = true;
        clearChamberRewards();
        return true;
    }

    private void clearChamberRewards() {
        // Deactivate spawners so the chamber stops producing more mobs.
        for (Location loc : spawnerLocations) {
            Block block = loc.getBlock();
            if (block.getType() == Material.SPAWNER) {
                block.setType(Material.AIR);
            }
        }

        if (chestLocation != null) {
            chestLocation.getBlock().setType(Material.CHEST);
            if (chestLocation.getBlock().getState() instanceof Chest chest) {
                fillLoot(chest, activeTheme);
            }
        }

        Component message = Component.text("The ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(activeTheme.element().displayName(), activeTheme.element().color(), TextDecoration.BOLD))
                .append(Component.text(" Chamber has been cleared! A loot chest has appeared inside.", NamedTextColor.LIGHT_PURPLE));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        bossBar.progress(0.0F);
        bossBar.name(Component.text("Chamber cleared - a new one rolls soon.", NamedTextColor.GRAY));
    }

    private void fillLoot(Chest chest, ChamberTheme theme) {
        int itemCount = 3 + random.nextInt(3); // 3-5 stacks
        for (int i = 0; i < itemCount; i++) {
            Material material = theme.lootPool()[random.nextInt(theme.lootPool().length)];
            int amount = 1 + random.nextInt(3);
            chest.getBlockInventory().addItem(new ItemStack(material, amount));
        }
        if (theme.rareLoot() != null && random.nextDouble() < 0.15D) {
            chest.getBlockInventory().addItem(new ItemStack(theme.rareLoot(), 1));
        }
    }

    // ---------------------------------------------------------------------
    // Boss bar tracking
    // ---------------------------------------------------------------------

    public void trackPlayer(Player player) {
        if (origin != null) {
            player.showBossBar(bossBar);
        }
    }

    public void untrackPlayer(Player player) {
        player.hideBossBar(bossBar);
    }
}
