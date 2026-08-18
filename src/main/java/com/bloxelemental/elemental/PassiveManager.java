package com.bloxelemental.elemental;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PassiveManager {

    private final ElementalSMP plugin;

    public PassiveManager(ElementalSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L * 30L);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Element element = plugin.getMasteryManager().getElement(player.getUniqueId());
            if (element != null) {
                PassiveInfo.applyBuffs(player, element);
                applySetBonus(player, element);
            }
        }
    }

    private void applySetBonus(Player player, Element element) {
        if (ArmorSets.hasFullSet(plugin, player, element)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.ABSORPTION, 20 * 40, 0, true, false));
        }
    }
}
