package net.cgerwyu.basicrpgclasses.equipment;

import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.weapon.Handedness;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Set;

public record EquipmentDescriptor(
        Kind kind,
        String typeTranslationKey,
        Set<RpgClass> allowedClasses,
        Handedness handedness,
        boolean offHandOnly,
        ArmorWeight armorWeight,
        boolean holy,
        EquipmentSlot armorSlot,
        AccessoryType accessoryType
) {
    public EquipmentDescriptor {
        allowedClasses = Set.copyOf(allowedClasses);
    }

    public enum Kind {
        WEAPON,
        ARMOR,
        ACCESSORY
    }
}
