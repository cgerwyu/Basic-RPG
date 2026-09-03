package net.cgerwyu.basicrpgclasses.weapon;

import net.cgerwyu.basicrpgclasses.data.RpgClass;

import java.util.List;

public record WeaponProfile(
        String id,
        RpgClass requiredClass,
        WeaponFamily family,
        Handedness handedness,
        float attacksPerSecond,
        float armorPenetration,
        float poiseDamage,
        List<ComboStep> combo
) {
    public WeaponProfile {
        combo = List.copyOf(combo);
    }
}
