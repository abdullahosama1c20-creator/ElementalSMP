package com.bloxelemental.elemental;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casts elemental abilities from held catalyst items, tracks per-ability
 * cooldowns with an action bar countdown, handles Storm Core / Void Tear
 * awakening consumption, and grants Mastery XP for combat kills.
 */
public class AbilityListener implements Listener {

    private static NamespacedKey key(ElementalSMP plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    private final ElementalSMP plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    /** UUID -> timestamp (ms) up to which a pending awaken is confirmed by a second click. */
    private final Map<UUID, Long> pendingAwakenConfirmations = new HashMap<>();
    private static final long AWAKEN_CONFIRM_WINDOW_MS = 15_000L;

    public AbilityListener(ElementalSMP plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------------
    // Item factories
    // ---------------------------------------------------------------------

    public static ItemStack catalystItem(ElementalSMP plugin, Element element) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("elementalsmp", element.name().toLowerCase() + "_catalyst"));
        meta.displayName(Component.text(element.displayName() + " Catalyst", element.color(), TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Right-click ", NamedTextColor.GRAY)
                        .append(Component.text("(Lv.1) Basic: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .append(Component.text(AbilityInfo.describe(element, Tier.BASIC), NamedTextColor.GRAY)),
                Component.text("Shift+Right-click ", NamedTextColor.GRAY)
                        .append(Component.text("(Lv.25) Mobility: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .append(Component.text(AbilityInfo.describe(element, Tier.MOBILITY), NamedTextColor.GRAY)),
                Component.text("Off-hand click ", NamedTextColor.GRAY)
                        .append(Component.text("(Lv.50) Heavy: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .append(Component.text(AbilityInfo.describe(element, Tier.HEAVY), NamedTextColor.GRAY)),
                Component.text("Shift+Off-hand ", NamedTextColor.GRAY)
                        .append(Component.text("(Lv.100) Ultimate: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .append(Component.text(AbilityInfo.describe(element, Tier.ULTIMATE), NamedTextColor.GRAY)),
                Component.text("Run /element abilities for the full list.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(key(plugin, "elemental_catalyst"), PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(key(plugin, "catalyst_element"), PersistentDataType.STRING, element.name());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack stormCoreItem() {
        ItemStack item = new ItemStack(Material.BREEZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Storm Core", NamedTextColor.YELLOW, TextDecoration.BOLD));
        meta.lore(List.of(Component.text("Shift + Right-click at Mastery Lv.100", NamedTextColor.GRAY),
                Component.text("to awaken the element of Lightning.", NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack voidTearItem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Void Tear", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        meta.lore(List.of(Component.text("Shift + Right-click at Mastery Lv.100", NamedTextColor.GRAY),
                Component.text("to awaken the element of Void.", NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------------
    // Interaction handling
    // ---------------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        Player player = event.getPlayer();
        ItemMeta meta = item.getItemMeta();

        if (Boolean.TRUE.equals(meta.getPersistentDataContainer().get(key(plugin, "elemental_catalyst"), PersistentDataType.BOOLEAN))) {
            event.setCancelled(true);
            handleCatalystUse(player, item, event.getHand());
            return;
        }

        if (item.getType() == Material.BREEZE_ROD && "Storm Core".equals(plainName(meta))) {
            event.setCancelled(true);
            handleAwakening(player, item, Element.LIGHTNING);
            return;
        }
        if (item.getType() == Material.ECHO_SHARD && "Void Tear".equals(plainName(meta))) {
            event.setCancelled(true);
            handleAwakening(player, item, Element.VOID);
        }
    }

    private String plainName(ItemMeta meta) {
        Component name = meta.displayName();
        return name == null ? null : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name);
    }

    private void handleAwakening(Player player, ItemStack item, Element target) {
        MasteryManager manager = plugin.getMasteryManager();
        UUID uuid = player.getUniqueId();
        if (!manager.isAwakeningEligible(uuid)) {
            player.sendMessage(Component.text("You need Mastery Level 100 on a starter element before you can awaken.", NamedTextColor.RED));
            return;
        }

        long now = System.currentTimeMillis();
        Long confirmBy = pendingAwakenConfirmations.get(uuid);
        if (confirmBy == null || confirmBy < now) {
            pendingAwakenConfirmations.put(uuid, now + AWAKEN_CONFIRM_WINDOW_MS);
            Element current = manager.getElement(uuid);
            player.sendMessage(Component.text("WARNING: ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("Awakening will PERMANENTLY replace your ", NamedTextColor.YELLOW))
                    .append(Component.text(current != null ? current.displayName() : "current", current != null ? current.color() : NamedTextColor.WHITE))
                    .append(Component.text(" element and reset your Mastery to Level 1. This cannot be undone by you.", NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Right-click again within 15 seconds to confirm.", NamedTextColor.GRAY));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.7F);
            return;
        }
        pendingAwakenConfirmations.remove(uuid);

        Element previousElement = manager.getElement(uuid);
        item.setAmount(item.getAmount() - 1);
        manager.awaken(uuid, target);

        // Old element's catalyst no longer does anything - clear it out and hand over the new one
        // so the player is never left without a working ability item.
        removeCatalystsOfElement(plugin, player, previousElement);
        ArmorSets.removeArmorOfElement(plugin, player, previousElement);
        player.getInventory().addItem(catalystItem(plugin, target));
        player.getInventory().addItem(ArmorSets.armorPieces(plugin, target));
        PassiveInfo.applyBuffs(player, target);

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 120, 1, 1.5, 1, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, target == Element.VOID ? 0.5F : 1.5F);
        player.sendMessage(Component.text("You have awakened the element of ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(target.displayName(), target.color(), TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.LIGHT_PURPLE)));
        player.sendMessage(Component.text("Your new " + target.displayName() + " Catalyst has been added to your inventory.", NamedTextColor.GRAY));
    }

    /**
     * Removes every catalyst item bound to the given element from a player's
     * inventory (used after an element change, since the old catalyst no longer works).
     */
    public static void removeCatalystsOfElement(ElementalSMP plugin, Player player, Element element) {
        if (element == null) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !stack.hasItemMeta()) {
                continue;
            }
            String tag = stack.getItemMeta().getPersistentDataContainer()
                    .get(key(plugin, "catalyst_element"), PersistentDataType.STRING);
            if (element.name().equals(tag)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private void handleCatalystUse(Player player, ItemStack item, EquipmentSlot hand) {
        MasteryManager manager = plugin.getMasteryManager();
        UUID uuid = player.getUniqueId();
        Element element = manager.getElement(uuid);
        if (element == null) {
            player.sendMessage(Component.text("You must choose an element with /element gui first.", NamedTextColor.RED));
            return;
        }

        String catalystElement = item.getItemMeta().getPersistentDataContainer()
                .get(key(plugin, "catalyst_element"), PersistentDataType.STRING);
        if (catalystElement == null || !catalystElement.equals(element.name())) {
            player.sendMessage(Component.text("This catalyst is not bound to your element.", NamedTextColor.RED));
            return;
        }

        boolean offhand = hand == EquipmentSlot.OFF_HAND;
        boolean sneaking = player.isSneaking();
        Tier tier;
        if (!offhand && !sneaking) {
            tier = Tier.BASIC;
        } else if (!offhand) {
            tier = Tier.MOBILITY;
        } else if (!sneaking) {
            tier = Tier.HEAVY;
        } else {
            tier = Tier.ULTIMATE;
        }

        if (!manager.canUseTier(uuid, tier.requiredLevel)) {
            player.sendMessage(Component.text(tier.label + " requires Mastery Level " + tier.requiredLevel + ".", NamedTextColor.RED));
            return;
        }

        String cooldownKey = element.name() + "_" + tier.name();
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        Long readyAt = playerCooldowns.get(cooldownKey);
        if (readyAt != null && readyAt > now) {
            double remaining = (readyAt - now) / 1000.0;
            player.sendActionBar(Component.text(String.format(tier.label + " on cooldown: %.1fs", remaining), NamedTextColor.RED));
            return;
        }

        castAbility(player, element, tier);
        long cooldownEnd = now + (tier.cooldownSeconds * 1000L);
        playerCooldowns.put(cooldownKey, cooldownEnd);
        startCooldownCountdown(player, tier.label, cooldownEnd);
    }

    private void startCooldownCountdown(Player player, String label, long readyAtMillis) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                long remainingMs = readyAtMillis - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    player.sendActionBar(Component.text(label + " ready!", NamedTextColor.GREEN));
                    cancel();
                    return;
                }
                player.sendActionBar(Component.text(String.format(label + ": %.1fs", remainingMs / 1000.0), NamedTextColor.AQUA));
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // ---------------------------------------------------------------------
    // Ability effects
    // ---------------------------------------------------------------------

    private void castAbility(Player player, Element element, Tier tier) {
        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection();

        switch (element) {
            case FIRE -> castFire(player, origin, direction, tier);
            case WATER -> castWater(player, origin, direction, tier);
            case AIR -> castAir(player, origin, direction, tier);
            case EARTH -> castEarth(player, origin, direction, tier);
            case LIGHTNING -> castLightning(player, origin, direction, tier);
            case VOID -> castVoid(player, origin, direction, tier);
        }
    }

    /** Draws a rotating double-helix particle stream between two points. */
    private void helix(Location start, Vector direction, double length, Particle particle) {
        helix(start, direction, length, particle, null);
    }

    private void helix(Location start, Vector direction, double length, Particle particle, Object data) {
        Vector normal = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (normal.lengthSquared() == 0) {
            normal = new Vector(1, 0, 0);
        }
        Vector normal2 = direction.clone().crossProduct(normal).normalize();
        for (double d = 0; d < length; d += 0.3) {
            double angle = d * 4;
            Location point = start.clone().add(direction.clone().multiply(d));
            point.add(normal.clone().multiply(Math.cos(angle) * 0.6));
            point.add(normal2.clone().multiply(Math.sin(angle) * 0.6));
            if (data != null) {
                point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0, data);
            } else {
                point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
            }
        }
    }

    private void sphereBurst(Location center, Particle particle, double radius) {
        sphereBurst(center, particle, radius, null);
    }

    private void sphereBurst(Location center, Particle particle, double radius, Object data) {
        for (double phi = 0; phi < Math.PI; phi += Math.PI / 12) {
            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 12) {
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                Location point = center.clone().add(x, y, z);
                if (data != null) {
                    point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0, data);
                } else {
                    point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private List<LivingEntity> nearbyTargets(Player player, double radius) {
        return player.getLocation().getNearbyLivingEntities(radius, e -> !e.equals(player)).stream().toList();
    }

    private void awardXpForCast(Player player, Tier tier) {
        double amount = switch (tier) {
            case BASIC -> 2.0;
            case MOBILITY -> 3.0;
            case HEAVY -> 5.0;
            case ULTIMATE -> 8.0;
        };
        plugin.getMasteryManager().addXP(player, amount);
    }

    private void castFire(Player player, Location origin, Vector direction, Tier tier) {
        switch (tier) {
            case BASIC -> {
                helix(origin, direction, 6, Particle.FLAME);
                player.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
                for (LivingEntity target : nearbyTargets(player, 4)) {
                    if (isInFront(player, target)) {
                        dealDamage(target, 4.0, player);
                        target.setFireTicks(60);
                    }
                }
            }
            case MOBILITY -> {
                player.setVelocity(direction.clone().multiply(1.8).setY(0.6));
                sphereBurst(origin, Particle.FLAME, 1.2);
                player.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1.0F, 1.2F);
            }
            case HEAVY -> {
                sphereBurst(player.getLocation().add(0, 1, 0), Particle.LAVA, 2.5);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
                for (LivingEntity target : nearbyTargets(player, 5)) {
                    dealDamage(target, 10.0, player);
                    target.setFireTicks(100);
                    target.setVelocity(target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().setY(0.4));
                }
            }
            case ULTIMATE -> {
                for (int i = 0; i < 3; i++) {
                    sphereBurst(origin.clone().add(direction.clone().multiply(i * 3)), Particle.FLAME, 2.0);
                }
                player.playSound(origin, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0F, 0.7F);
                for (LivingEntity target : nearbyTargets(player, 8)) {
                    dealDamage(target, 20.0, player);
                    target.setFireTicks(200);
                }
            }
        }
        awardXpForCast(player, tier);
    }

    private void castWater(Player player, Location origin, Vector direction, Tier tier) {
        switch (tier) {
            case BASIC -> {
                helix(origin, direction, 6, Particle.SPLASH);
                player.playSound(origin, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0F, 1.0F);
                for (LivingEntity target : nearbyTargets(player, 4)) {
                    if (isInFront(player, target)) {
                        dealDamage(target, 3.0, player);
                        target.setVelocity(direction.clone().multiply(1.2).setY(0.2));
                    }
                }
            }
            case MOBILITY -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
                sphereBurst(origin, Particle.BUBBLE_POP, 1.2);
                player.playSound(origin, Sound.ENTITY_DOLPHIN_JUMP, 1.0F, 1.0F);
            }
            case HEAVY -> {
                sphereBurst(player.getLocation().add(0, 1, 0), Particle.CLOUD, 2.5);
                player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 1.0F, 0.6F);
                for (LivingEntity target : nearbyTargets(player, 5)) {
                    dealDamage(target, 9.0, player);
                    target.setVelocity(new Vector(0, 1.1, 0));
                }
            }
            case ULTIMATE -> {
                for (int i = 0; i < 3; i++) {
                    sphereBurst(origin.clone().add(direction.clone().multiply(i * 3)), Particle.SPLASH, 2.0);
                }
                player.playSound(origin, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 1.0F);
                for (LivingEntity target : nearbyTargets(player, 8)) {
                    dealDamage(target, 18.0, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                }
            }
        }
        awardXpForCast(player, tier);
    }

    private void castAir(Player player, Location origin, Vector direction, Tier tier) {
        switch (tier) {
            case BASIC -> {
                helix(origin, direction, 6, Particle.CLOUD);
                player.playSound(origin, Sound.ENTITY_PHANTOM_FLAP, 1.0F, 1.4F);
                for (LivingEntity target : nearbyTargets(player, 4)) {
                    if (isInFront(player, target)) {
                        dealDamage(target, 3.0, player);
                        target.setVelocity(direction.clone().multiply(1.5));
                    }
                }
            }
            case MOBILITY -> {
                player.setVelocity(direction.clone().multiply(2.2).setY(0.8));
                sphereBurst(origin, Particle.CLOUD, 1.2);
                player.playSound(origin, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.5F);
            }
            case HEAVY -> {
                sphereBurst(player.getLocation().add(0, 1, 0), Particle.EXPLOSION, 2.5);
                player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0F, 1.0F);
                for (LivingEntity target : nearbyTargets(player, 5)) {
                    dealDamage(target, 8.0, player);
                    Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.0);
                    target.setVelocity(push.setY(0.6));
                }
            }
            case ULTIMATE -> {
                for (int i = 0; i < 3; i++) {
                    sphereBurst(origin.clone().add(direction.clone().multiply(i * 3)), Particle.CLOUD, 2.0);
                }
                player.playSound(origin, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5F, 0.8F);
                for (LivingEntity target : nearbyTargets(player, 8)) {
                    dealDamage(target, 16.0, player);
                    target.setVelocity(new Vector(0, 1.5, 0));
                }
            }
        }
        awardXpForCast(player, tier);
    }

    private void castEarth(Player player, Location origin, Vector direction, Tier tier) {
        Object stoneData = Material.STONE.createBlockData();
        switch (tier) {
            case BASIC -> {
                helix(origin, direction, 6, Particle.BLOCK_CRUMBLE, stoneData);
                player.playSound(origin, Sound.BLOCK_STONE_BREAK, 1.0F, 0.8F);
                for (LivingEntity target : nearbyTargets(player, 4)) {
                    if (isInFront(player, target)) {
                        dealDamage(target, 4.0, player);
                        target.setVelocity(new Vector(0, 0.5, 0));
                    }
                }
            }
            case MOBILITY -> {
                sphereBurst(origin, Particle.BLOCK_CRUMBLE, 1.2, stoneData);
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
                player.playSound(origin, Sound.BLOCK_STONE_STEP, 1.0F, 0.6F);
            }
            case HEAVY -> {
                sphereBurst(player.getLocation().add(0, 1, 0), Particle.EXPLOSION, 2.5);
                player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1.0F, 0.9F);
                for (LivingEntity target : nearbyTargets(player, 5)) {
                    dealDamage(target, 11.0, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3));
                }
            }
            case ULTIMATE -> {
                for (int i = 0; i < 3; i++) {
                    sphereBurst(origin.clone().add(direction.clone().multiply(i * 3)), Particle.BLOCK_CRUMBLE, 2.0, stoneData);
                }
                player.playSound(origin, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0F, 0.7F);
                for (LivingEntity target : nearbyTargets(player, 8)) {
                    dealDamage(target, 20.0, player);
                    target.setVelocity(new Vector(0, 0.2, 0));
                }
            }
        }
        awardXpForCast(player, tier);
    }

    private void castLightning(Player player, Location origin, Vector direction, Tier tier) {
        switch (tier) {
            case BASIC -> {
                helix(origin, direction, 6, Particle.ELECTRIC_SPARK);
                player.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6F, 1.4F);
                for (LivingEntity target : nearbyTargets(player, 4)) {
                    if (isInFront(player, target)) {
                        dealDamage(target, 5.0, player);
                    }
                }
            }
            case MOBILITY -> {
                Location dest = origin.clone().add(direction.clone().multiply(6));
                player.teleport(dest);
                sphereBurst(dest, Particle.ELECTRIC_SPARK, 1.2);
                player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.5F);
            }
            case HEAVY -> {
                sphereBurst(player.getLocation().add(0, 1, 0), Particle.ELECTRIC_SPARK, 2.5);
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);
                for (LivingEntity target : nearbyTargets(player, 5)) {
                    dealDamage(target, 12.0, player);
                    target.getWorld().strikeLightningEffect(target.getLocation());
                }
            }
            case ULTIMATE -> {
                for (LivingEntity target : nearbyTargets(player, 8)) {
                    dealDamage(target, 22.0, player);
                    target.getWorld().strikeLightningEffect(target.getLocation());
                }
                player.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 0.7F);
            }
        }
        awardXpForCast(player, tier);
    }

    private void castVoid(Player player, Location origin, Vector direction, Tier tier) {
        switch (tier) {
            case BASIC -> {
                helix(origin, direction, 6, Particle.PORTAL);
                player.playSound(origin, Sound.ENTITY_ENDERMAN_STARE, 0.6F, 1.0F);
                for (LivingEntity target : nearbyTargets(player, 4)) {
                    if (isInFront(player, target)) {
                        dealDamage(target, 5.0, player);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                    }
                }
            }
            case MOBILITY -> {
                Location dest = origin.clone().add(direction.clone().multiply(8));
                player.teleport(dest);
                sphereBurst(dest, Particle.PORTAL, 1.2);
                player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
            }
            case HEAVY -> {
                sphereBurst(player.getLocation().add(0, 1, 0), Particle.REVERSE_PORTAL, 2.5);
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6F, 1.0F);
                for (LivingEntity target : nearbyTargets(player, 5)) {
                    dealDamage(target, 13.0, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                }
            }
            case ULTIMATE -> {
                for (int i = 0; i < 3; i++) {
                    sphereBurst(origin.clone().add(direction.clone().multiply(i * 3)), Particle.PORTAL, 2.0);
                }
                player.playSound(origin, Sound.ENTITY_WARDEN_ROAR, 1.0F, 0.7F);
                for (LivingEntity target : nearbyTargets(player, 8)) {
                    dealDamage(target, 24.0, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                }
            }
        }
        awardXpForCast(player, tier);
    }

    private boolean isInFront(Player player, Entity target) {
        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        return player.getLocation().getDirection().dot(toTarget) > 0.3;
    }

    /**
     * Applies the elemental counter cycle on top of a base ability damage
     * amount, then deals it. Chamber mobs count as having their chamber's
     * element for this purpose; other mobs and elementless players are neutral.
     */
    private void dealDamage(LivingEntity target, double baseAmount, Player attacker) {
        Element attackerElement = plugin.getMasteryManager().getElement(attacker.getUniqueId());
        Element defenderElement = resolveElement(target);
        double multiplier = ElementalCounters.damageMultiplier(attackerElement, defenderElement);
        target.damage(baseAmount * multiplier, attacker);
    }

    private Element resolveElement(LivingEntity entity) {
        if (entity instanceof Player p) {
            return plugin.getMasteryManager().getElement(p.getUniqueId());
        }
        String tag = entity.getPersistentDataContainer().get(key(plugin, "chamber_element"), PersistentDataType.STRING);
        return tag == null ? null : Element.fromArgument(tag);
    }

    // ---------------------------------------------------------------------
    // Combat XP
    // ---------------------------------------------------------------------

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        MasteryManager manager = plugin.getMasteryManager();
        if (!manager.hasElement(killer.getUniqueId())) {
            return;
        }
        double baseXp = event.getEntity() instanceof Player ? 50.0 : 10.0;
        manager.addXP(killer, baseXp);
        manager.incrementKills(killer.getUniqueId());
    }
}
