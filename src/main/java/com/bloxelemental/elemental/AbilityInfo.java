package com.bloxelemental.elemental;

import java.util.EnumMap;
import java.util.Map;

/**
 * Static lookup of flavour/gameplay descriptions for each element's four
 * ability tiers (index 0 = Basic, 1 = Mobility, 2 = Heavy, 3 = Ultimate).
 */
public final class AbilityInfo {

    private static final Map<Element, String[]> DESCRIPTIONS = new EnumMap<>(Element.class);

    static {
        DESCRIPTIONS.put(Element.FIRE, new String[]{
                "Hurls a helix of flame forward, damaging and igniting enemies in front of you.",
                "Launches you forward and upward in a burst of fire.",
                "Erupts a fiery explosion around you, damaging, igniting and launching nearby enemies.",
                "Unleashes a chain of fiery bursts ahead of you for massive damage and prolonged burning."
        });
        DESCRIPTIONS.put(Element.WATER, new String[]{
                "Sends a wave of water forward, damaging and pushing back enemies.",
                "Grants a burst of speed and aquatic agility (Dolphin's Grace).",
                "Summons a downpour around you that damages and launches nearby enemies upward.",
                "Crashes a chain of waves ahead of you, dealing heavy damage and slowing enemies."
        });
        DESCRIPTIONS.put(Element.AIR, new String[]{
                "A gust of wind that damages and knocks back enemies in front of you.",
                "Propels you forward and upward through the air (Air Dash).",
                "Creates a violent updraft around you that damages and launches nearby enemies.",
                "A chain of violent gusts that devastate and launch anything nearby."
        });
        DESCRIPTIONS.put(Element.EARTH, new String[]{
                "Shatters the ground in front of you, damaging and popping enemies upward.",
                "Hardens your skin, granting temporary damage resistance.",
                "Slams the earth around you, damaging and slowing nearby enemies.",
                "A chain of ground-shattering blasts that crush anything nearby."
        });
        DESCRIPTIONS.put(Element.LIGHTNING, new String[]{
                "A bolt of lightning arcing forward, damaging enemies in front of you.",
                "Blinks you forward instantly in a flash of lightning.",
                "Calls down lightning strikes on nearby enemies.",
                "A devastating barrage of lightning strikes on every nearby enemy."
        });
        DESCRIPTIONS.put(Element.VOID, new String[]{
                "Tears open space in front of you, damaging and darkening the vision of enemies.",
                "Blinks you through the void to a nearby location.",
                "Warps space around you, damaging and blinding nearby enemies.",
                "Rips open the void itself, dealing devastating damage and withering everything nearby."
        });
    }

    private AbilityInfo() {
    }

    public static String describe(Element element, Tier tier) {
        String[] arr = DESCRIPTIONS.get(element);
        if (arr == null) {
            return "No description available.";
        }
        return arr[tier.ordinal()];
    }
}
