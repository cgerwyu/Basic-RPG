package net.cgerwyu.basicrpgclasses.skill;

import java.util.HashMap;
import java.util.Map;

/**
 * Small, reusable visual primitives. Combat skills compose these instead of
 * spawning hundreds of vanilla particles.
 */
public enum SkillVfxType {
    BURST(0),
    AREA_RING(1),
    SHIELD(2),
    TRAVEL_STREAK(3),
    SLASH_ORBIT(4),
    LIGHTNING_ARC(5),
    METEOR_FLIGHT(6),
    METEOR_IMPACT(7),
    GROUND_CONE(8),
    HEALING_FIELD(9),
    WIND_TRAIL(10),
    FROST_FIELD(11),
    SKY_CROSS(12),
    BOW_CHARGE(13),
    PROJECTILE_TRAIL(14),
    TAUNT_ARROWS(15),
    FORTIFY_SHIELDS(16),
    KINETIC_BURST(17),
    WARRIOR_AURA(18),
    BLOOD_DRAIN(19),
    HUNTER_TARGET_FIELD(20),
    HUNTER_AFTERIMAGE(21),
    HUNTER_CLOAK(22),
    PIERCING_VOLLEY(23),
    FIREBALL_AURA(24),
    HOLY_WINGS(25),
    SONIC_DASH(26),
    DIVINE_SLASH_WAVE(27),
    TARGET_RING(28),
    WARRIOR_LEAP_IMPACT(29),
    BULWARK_SHIELDS(30),
    ARCANE_SHIELD(31),
    PRIEST_BEAM(32);

    private static final Map<Integer, SkillVfxType> BY_ID = new HashMap<>();

    static {
        for (SkillVfxType value : values()) {
            BY_ID.put(value.id, value);
        }
    }

    private final int id;

    SkillVfxType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static SkillVfxType byId(int id) {
        return BY_ID.getOrDefault(id, BURST);
    }
}
