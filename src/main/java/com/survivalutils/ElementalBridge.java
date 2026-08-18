package com.survivalutils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Talks to ElementalSMP purely via reflection, so SurvivalUtils has zero
 * compile-time dependency on it - no shared library project, no committed
 * jar, no build-order coupling. If ElementalSMP isn't installed (or a method
 * signature ever changes), every call here just returns empty/no-ops instead
 * of throwing, so SurvivalUtils works completely fine standalone.
 */
public class ElementalBridge {

    public record ElementInfo(String elementName, int level) {
    }

    private Plugin elementalPlugin;
    private boolean available;

    public void refresh() {
        elementalPlugin = Bukkit.getPluginManager().getPlugin("ElementalSMP");
        available = elementalPlugin != null && elementalPlugin.isEnabled();
    }

    public boolean isAvailable() {
        return available;
    }

    public Optional<ElementInfo> getElementInfo(UUID uuid) {
        if (!available) {
            return Optional.empty();
        }
        try {
            Method getMasteryManager = elementalPlugin.getClass().getMethod("getMasteryManager");
            Object masteryManager = getMasteryManager.invoke(elementalPlugin);

            Method getElement = masteryManager.getClass().getMethod("getElement", UUID.class);
            Object elementObj = getElement.invoke(masteryManager, uuid);
            if (elementObj == null) {
                return Optional.empty();
            }

            Method displayName = elementObj.getClass().getMethod("displayName");
            String name = (String) displayName.invoke(elementObj);

            Method getLevel = masteryManager.getClass().getMethod("getLevel", UUID.class);
            int level = (int) getLevel.invoke(masteryManager, uuid);

            return Optional.of(new ElementInfo(name, level));
        } catch (ReflectiveOperationException | ClassCastException e) {
            return Optional.empty();
        }
    }

    /** Returns true if both players have chosen an element and it's the same one. */
    public boolean sameElement(UUID a, UUID b) {
        Optional<ElementInfo> infoA = getElementInfo(a);
        Optional<ElementInfo> infoB = getElementInfo(b);
        return infoA.isPresent() && infoB.isPresent() && infoA.get().elementName().equals(infoB.get().elementName());
    }

    /** Grants bonus Mastery XP to a player via ElementalSMP's own addXP method, if available. */
    public void grantBonusXp(Player player, double amount) {
        if (!available) {
            return;
        }
        try {
            Method getMasteryManager = elementalPlugin.getClass().getMethod("getMasteryManager");
            Object masteryManager = getMasteryManager.invoke(elementalPlugin);
            Method addXp = masteryManager.getClass().getMethod("addXP", Player.class, double.class);
            addXp.invoke(masteryManager, player, amount);
        } catch (ReflectiveOperationException ignored) {
            // ElementalSMP not present or its API changed - silently skip the bonus.
        }
    }
}
