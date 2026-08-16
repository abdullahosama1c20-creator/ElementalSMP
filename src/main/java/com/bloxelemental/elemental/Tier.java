package com.bloxelemental.elemental;

/**
 * The four ability tiers every element unlocks at, matching the Mastery
 * thresholds in MasteryManager.
 */
public enum Tier {
    BASIC(1, 3, "Basic Skill"),
    MOBILITY(MasteryManager.MOBILITY_THRESHOLD, 8, "Mobility Skill"),
    HEAVY(MasteryManager.HEAVY_THRESHOLD, 15, "Heavy Combat Skill"),
    ULTIMATE(MasteryManager.ULTIMATE_THRESHOLD, 30, "Ultimate Skill");

    final int requiredLevel;
    final int cooldownSeconds;
    final String label;

    Tier(int requiredLevel, int cooldownSeconds, String label) {
        this.requiredLevel = requiredLevel;
        this.cooldownSeconds = cooldownSeconds;
        this.label = label;
    }
}
