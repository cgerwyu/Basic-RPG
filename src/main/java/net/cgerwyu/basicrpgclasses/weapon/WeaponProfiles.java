package net.cgerwyu.basicrpgclasses.weapon;

import net.cgerwyu.basicrpgclasses.data.RpgClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WeaponProfiles {
    public static final WeaponProfile APPRENTICE_STAFF = profile(
            "apprentice_staff", RpgClass.MAGE, WeaponFamily.STAFF, Handedness.TWO_HANDED,
            1.0F, 0.0F, 0.5F,
            step(0.65F, HitShape.SMASH_AREA, 2.5F, 35.0F)
    );
    public static final WeaponProfile IRON_RAPIER = profile(
            "iron_rapier", RpgClass.WARRIOR, WeaponFamily.RAPIER, Handedness.ONE_HANDED,
            1.8F, 0.15F, 0.45F,
            step(0.65F, HitShape.THRUST_RAY, 3.5F, 8.0F),
            step(0.70F, HitShape.THRUST_RAY, 3.5F, 8.0F),
            step(0.80F, HitShape.THRUST_RAY, 3.7F, 8.0F),
            step(0.90F, HitShape.THRUST_RAY, 3.7F, 8.0F),
            step(1.45F, HitShape.THRUST_RAY, 4.0F, 10.0F)
    );
    public static final WeaponProfile IRON_GREATSWORD = profile(
            "iron_greatsword", RpgClass.WARRIOR, WeaponFamily.GREATSWORD, Handedness.TWO_HANDED,
            0.8F, 0.20F, 1.2F,
            step(1.15F, HitShape.SLASH_CONE, 3.8F, 100.0F),
            step(1.40F, HitShape.SLASH_CONE, 4.0F, 120.0F),
            step(1.85F, HitShape.SLASH_CONE, 4.2F, 150.0F)
    );
    public static final WeaponProfile IRON_WARHAMMER = profile(
            "iron_warhammer", RpgClass.WARRIOR, WeaponFamily.WARHAMMER, Handedness.TWO_HANDED,
            0.65F, 0.30F, 2.0F,
            step(1.40F, HitShape.SMASH_AREA, 3.0F, 55.0F),
            step(2.20F, HitShape.SMASH_AREA, 3.2F, 70.0F)
    );
    public static final WeaponProfile SIMPLE_SHORTBOW = profile(
            "simple_shortbow", RpgClass.HUNTER, WeaponFamily.SHORTBOW, Handedness.TWO_HANDED,
            1.45F, 0.0F, 0.25F,
            step(0.80F, HitShape.PROJECTILE, 32.0F, 0.0F)
    );
    public static final WeaponProfile SIMPLE_RECURVE_BOW = profile(
            "simple_recurve_bow", RpgClass.HUNTER, WeaponFamily.RECURVE_BOW, Handedness.TWO_HANDED,
            1.15F, 0.05F, 0.35F,
            step(1.00F, HitShape.PROJECTILE, 48.0F, 0.0F)
    );
    public static final WeaponProfile SIMPLE_LONGBOW = profile(
            "simple_longbow", RpgClass.HUNTER, WeaponFamily.LONGBOW, Handedness.TWO_HANDED,
            0.85F, 0.10F, 0.5F,
            step(1.30F, HitShape.PROJECTILE, 64.0F, 0.0F)
    );
    public static final WeaponProfile HUNTING_KNIFE = profile(
            "hunting_knife", RpgClass.HUNTER, WeaponFamily.KNIFE, Handedness.ONE_HANDED,
            2.0F, 0.05F, 0.35F,
            step(0.70F, HitShape.SLASH_CONE, 2.4F, 35.0F),
            step(0.85F, HitShape.SLASH_CONE, 2.5F, 40.0F),
            step(1.25F, HitShape.THRUST_RAY, 2.8F, 12.0F)
    );
    public static final WeaponProfile FORGEHEART_GREATSWORD = profile(
            "forgeheart_greatsword", RpgClass.WARRIOR, WeaponFamily.GREATSWORD, Handedness.TWO_HANDED,
            0.75F, 0.28F, 1.6F,
            step(1.30F, HitShape.SLASH_CONE, 4.1F, 115.0F),
            step(1.65F, HitShape.SLASH_CONE, 4.3F, 135.0F),
            step(2.10F, HitShape.SMASH_AREA, 3.2F, 160.0F)
    );
    public static final WeaponProfile CRIMSON_DRAGON_GREATSWORD = profile(
            "crimson_dragon_greatsword", RpgClass.WARRIOR, WeaponFamily.GREATSWORD, Handedness.TWO_HANDED,
            0.82F, 0.35F, 1.8F,
            step(1.45F, HitShape.SLASH_CONE, 4.3F, 120.0F),
            step(1.75F, HitShape.SLASH_CONE, 4.5F, 145.0F),
            step(2.30F, HitShape.SMASH_AREA, 3.5F, 170.0F)
    );
    public static final WeaponProfile OATHKEEPER_SWORD = profile(
            "oathkeeper_sword", RpgClass.PALADIN, WeaponFamily.ONE_HANDED_SWORD, Handedness.ONE_HANDED,
            1.25F, 0.16F, 0.75F,
            step(0.95F, HitShape.SLASH_CONE, 3.1F, 75.0F),
            step(1.10F, HitShape.SLASH_CONE, 3.2F, 85.0F),
            step(1.45F, HitShape.THRUST_RAY, 3.5F, 15.0F)
    );
    public static final WeaponProfile DAWNFIRE_SWORD = profile(
            "dawnfire_sword", RpgClass.PALADIN, WeaponFamily.ONE_HANDED_SWORD, Handedness.ONE_HANDED,
            1.20F, 0.22F, 0.90F,
            step(1.05F, HitShape.SLASH_CONE, 3.2F, 80.0F),
            step(1.25F, HitShape.SLASH_CONE, 3.3F, 90.0F),
            step(1.60F, HitShape.THRUST_RAY, 3.7F, 15.0F)
    );
    public static final WeaponProfile RUNIC_BULWARK = profile(
            "runic_bulwark", RpgClass.PALADIN, WeaponFamily.SHIELD, Handedness.ONE_HANDED,
            0.65F, 0.0F, 1.2F,
            step(0.75F, HitShape.SMASH_AREA, 2.2F, 60.0F)
    );
    public static final WeaponProfile STORMGUARD_SHIELD = profile(
            "stormguard_shield", RpgClass.PALADIN, WeaponFamily.SHIELD, Handedness.ONE_HANDED,
            0.70F, 0.0F, 1.3F,
            step(0.85F, HitShape.SMASH_AREA, 2.3F, 65.0F)
    );
    public static final WeaponProfile STORMWING_STAFF = profile(
            "stormwing_staff", RpgClass.MAGE, WeaponFamily.STAFF, Handedness.TWO_HANDED,
            1.05F, 0.08F, 0.65F,
            step(0.95F, HitShape.PROJECTILE, 52.0F, 0.0F)
    );
    public static final WeaponProfile FROZEN_SERPENT_STAFF = profile(
            "frozen_serpent_staff", RpgClass.MAGE, WeaponFamily.STAFF, Handedness.TWO_HANDED,
            0.90F, 0.12F, 0.80F,
            step(1.15F, HitShape.PROJECTILE, 58.0F, 0.0F)
    );
    public static final WeaponProfile BROODMOTHER_SCEPTER = profile(
            "broodmother_scepter", RpgClass.PRIEST, WeaponFamily.SCEPTER, Handedness.ONE_HANDED,
            1.20F, 0.0F, 0.45F,
            step(0.80F, HitShape.PROJECTILE, 42.0F, 0.0F)
    );
    public static final WeaponProfile CRIMSON_DAWN_SCEPTER = profile(
            "crimson_dawn_scepter", RpgClass.PRIEST, WeaponFamily.SCEPTER, Handedness.ONE_HANDED,
            1.05F, 0.08F, 0.60F,
            step(1.05F, HitShape.PROJECTILE, 50.0F, 0.0F)
    );
    public static final WeaponProfile MOSSFANG_SHORTBOW = profile(
            "mossfang_shortbow", RpgClass.HUNTER, WeaponFamily.SHORTBOW, Handedness.TWO_HANDED,
            1.55F, 0.04F, 0.35F,
            step(0.95F, HitShape.PROJECTILE, 38.0F, 0.0F)
    );
    public static final WeaponProfile DUSKSTALKER_LONGBOW = profile(
            "duskstalker_longbow", RpgClass.HUNTER, WeaponFamily.LONGBOW, Handedness.TWO_HANDED,
            0.80F, 0.18F, 0.65F,
            step(1.45F, HitShape.PROJECTILE, 72.0F, 0.0F)
    );

    private static final Map<String, WeaponProfile> BY_ID = new LinkedHashMap<>();

    static {
        for (WeaponProfile profile : List.of(
                APPRENTICE_STAFF, IRON_RAPIER, IRON_GREATSWORD, IRON_WARHAMMER,
                SIMPLE_SHORTBOW, SIMPLE_RECURVE_BOW, SIMPLE_LONGBOW, HUNTING_KNIFE,
                FORGEHEART_GREATSWORD, CRIMSON_DRAGON_GREATSWORD,
                OATHKEEPER_SWORD, DAWNFIRE_SWORD, RUNIC_BULWARK, STORMGUARD_SHIELD,
                STORMWING_STAFF, FROZEN_SERPENT_STAFF, BROODMOTHER_SCEPTER, CRIMSON_DAWN_SCEPTER,
                MOSSFANG_SHORTBOW, DUSKSTALKER_LONGBOW
        )) {
            BY_ID.put(profile.id(), profile);
        }
    }

    public static WeaponProfile get(String id) {
        WeaponProfile profile = BY_ID.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown weapon profile: " + id);
        }
        return profile;
    }

    public static Map<String, WeaponProfile> all() {
        return Map.copyOf(BY_ID);
    }

    private static WeaponProfile profile(
            String id, RpgClass requiredClass, WeaponFamily family, Handedness handedness,
            float attacksPerSecond, float armorPenetration, float poiseDamage, ComboStep... combo
    ) {
        return new WeaponProfile(id, requiredClass, family, handedness, attacksPerSecond,
                armorPenetration, poiseDamage, List.of(combo));
    }

    private static ComboStep step(float damage, HitShape shape, float reach, float angle) {
        return new ComboStep(damage, shape, reach, angle);
    }

    private WeaponProfiles() {
    }
}
