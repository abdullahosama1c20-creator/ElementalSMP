package com.survivalutils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    public static final long COMBAT_DURATION_MS = 15_000L;

    private final Map<UUID, Long> taggedUntil = new HashMap<>();

    public void tag(UUID uuid) {
        taggedUntil.put(uuid, System.currentTimeMillis() + COMBAT_DURATION_MS);
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
