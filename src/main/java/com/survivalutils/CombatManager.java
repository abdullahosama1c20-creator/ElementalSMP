package com.survivalutils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final long combatDurationMs;
    private final Map<UUID, Long> taggedUntil = new HashMap<>();

    public CombatManager(SurvivalUtils plugin) {
        this.combatDurationMs = plugin.getConfig().getLong("combat.tag-duration-seconds", 15L) * 1000L;
    }

    public void tag(UUID uuid) {
        taggedUntil.put(uuid, System.currentTimeMillis() + combatDurationMs);
    }

    public boolean isTagged(UUID uuid) {
        Long until = taggedUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            taggedUntil.remove(uuid);
            return false;
        }
        return true;
    }

    public long remainingMillis(UUID uuid) {
        Long until = taggedUntil.get(uuid);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public void clear(UUID uuid) {
        taggedUntil.remove(uuid);
    }

    public Map<UUID, Long> getAllTagged() {
        return taggedUntil;
    }
}
