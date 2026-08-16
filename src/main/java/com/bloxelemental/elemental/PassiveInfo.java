package com.bloxelemental.elemental;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Defines and applies the one passive perk each element grants its bearer,
 * on top of the four active abilities. Damage-immunity passives are enforced
 * live in PassiveListener; buff-style passives are refreshed periodically by
 * PassiveManager so they never wear off while you hold that element.
 */
public final class PassiveInfo {

    private PassiveInfo() {
    }

    public static String describe(Element element) {
        return switch (element) {
            case FIRE -> "Immune to fire and lava damage.";
            case WATER -> "Infinite water breathing.";
            case AIR -> "Immune to fall damage and moves slightly faster.";
            case EARTH -> "Increased knockback resistance.";
            case LIGHTNING -> "Moves slightly faster and is immune to lightning strikes.";
            case VOID -> "Permanent night vision and slow falling.";
        };
    }

    /** Re-applies this element's refreshable buffs (potion effects). Safe to call repeatedly. */
    public static void applyBuffs(Player player, Element element) {
        int refreshTicks = 20 * 40; // reapplied every 30s by PassiveManager, so 40s covers the gap
        switch (element) {
            case WATER -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, refreshTicks, 0, true, false));
            case AIR -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, refreshTicks, 0, true, false));
            case LIGHTNING -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, refreshTicks, 0, true, false));
            case VOID -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, refreshTicks, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, refreshTicks, 0, true, false));
            }
            default -> {
                // FIRE and EARTH passives are handled via damage events / attributes below, no potion needed.
            }
        }

        if (element == Element.EARTH) {
            AttributeInstance knockbackResistance = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockbackResistance != null) {
                knockbackResistance.setBaseValue(0.3D);
            }
        } else {
            // Reset knockback resistance for anyone who left Earth (e.g. awakened away from it).
            AttributeInstance knockbackResistance = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockbackResistance != null && knockbackResistance.getBaseValue() == 0.3D) {
                knockbackResistance.setBaseValue(0.0D);
            }
        }
    }
}
