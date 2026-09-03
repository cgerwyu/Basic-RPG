package net.cgerwyu.basicrpgclasses.equipment;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.item.ProfiledWeapon;
import net.cgerwyu.basicrpgclasses.registry.ModItemTags;
import net.cgerwyu.basicrpgclasses.weapon.Handedness;
import net.cgerwyu.basicrpgclasses.weapon.WeaponFamily;
import net.cgerwyu.basicrpgclasses.weapon.WeaponProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.equipment.Equippable;

import java.util.EnumSet;
import java.util.Set;

public final class EquipmentRules {
    private static final Set<RpgClass> ALL_CLASSES = Set.copyOf(EnumSet.of(
            RpgClass.WARRIOR, RpgClass.MAGE, RpgClass.HUNTER, RpgClass.PRIEST, RpgClass.PALADIN
    ));
    private static final Set<RpgClass> LIGHT_ARMOR_CLASSES = Set.copyOf(EnumSet.of(
            RpgClass.MAGE, RpgClass.HUNTER, RpgClass.PRIEST
    ));

    public static EquipmentDescriptor describe(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (stack.getItem() instanceof ProfiledWeapon profiledWeapon) {
            return profiledWeapon(profiledWeapon.profile());
        }

        AccessoryType accessoryType = accessoryType(stack);
        if (accessoryType != null) {
            return new EquipmentDescriptor(
                    EquipmentDescriptor.Kind.ACCESSORY,
                    accessoryType.translationKey(),
                    ALL_CLASSES,
                    null,
                    false,
                    null,
                    false,
                    null,
                    accessoryType
            );
        }

        EquipmentDescriptor armor = armor(stack);
        if (armor != null) {
            return armor;
        }

        if (stack.getItem() instanceof ShieldItem || stack.is(Items.SHIELD)) {
            return vanillaWeapon("tooltip.basicrpgclasses.type.shield", Handedness.ONE_HANDED, true, RpgClass.WARRIOR);
        }
        if (stack.getItem() instanceof BowItem) {
            return vanillaWeapon("tooltip.basicrpgclasses.type.bow", Handedness.TWO_HANDED, false, RpgClass.HUNTER);
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return vanillaWeapon("tooltip.basicrpgclasses.type.crossbow", Handedness.TWO_HANDED, false, RpgClass.HUNTER);
        }
        if (stack.is(ItemTags.SWORDS)) {
            return vanillaWeapon(
                    "tooltip.basicrpgclasses.type.sword",
                    Handedness.ONE_HANDED,
                    false,
                    Set.of(RpgClass.WARRIOR, RpgClass.PALADIN)
            );
        }
        if (stack.is(ItemTags.AXES)) {
            return vanillaWeapon("tooltip.basicrpgclasses.type.axe", Handedness.ONE_HANDED, false, RpgClass.WARRIOR);
        }
        if (stack.is(Items.MACE)) {
            return vanillaWeapon("tooltip.basicrpgclasses.type.hammer", Handedness.TWO_HANDED, false, RpgClass.WARRIOR);
        }
        if (stack.is(ItemTags.SPEARS) || stack.is(Items.TRIDENT)) {
            return vanillaWeapon("tooltip.basicrpgclasses.type.spear", Handedness.TWO_HANDED, false, RpgClass.WARRIOR);
        }

        return null;
    }

    public static boolean canUse(Player player, ItemStack stack) {
        EquipmentDescriptor descriptor = describe(stack);
        return descriptor == null || descriptor.allowedClasses().contains(player.getData(ModAttachments.PLAYER_CLASS).rpgClass());
    }

    public static boolean canUse(RpgClass rpgClass, EquipmentDescriptor descriptor) {
        return descriptor == null || descriptor.allowedClasses().contains(rpgClass);
    }

    public static boolean canPlace(Player player, RpgEquipmentSlotType slotType, ItemStack stack) {
        EquipmentDescriptor descriptor = describe(stack);
        if (descriptor == null || !canUse(player, stack) || !fitsSlot(descriptor, slotType)) {
            return false;
        }

        PlayerEquipmentData equipment = player.getData(ModAttachments.PLAYER_EQUIPMENT);
        if (slotType == RpgEquipmentSlotType.MAIN_WEAPON
                && descriptor.handedness() == Handedness.TWO_HANDED
                && !equipment.get(RpgEquipmentSlotType.OFF_WEAPON).isEmpty()) {
            return false;
        }
        if (slotType == RpgEquipmentSlotType.OFF_WEAPON
                && isTwoHanded(equipment.get(RpgEquipmentSlotType.MAIN_WEAPON))) {
            return false;
        }
        return true;
    }

    public static boolean fitsSlot(EquipmentDescriptor descriptor, RpgEquipmentSlotType slotType) {
        return switch (slotType) {
            case MAIN_WEAPON -> descriptor.kind() == EquipmentDescriptor.Kind.WEAPON && !descriptor.offHandOnly();
            case OFF_WEAPON -> descriptor.kind() == EquipmentDescriptor.Kind.WEAPON
                    && (descriptor.offHandOnly() || descriptor.handedness() == Handedness.ONE_HANDED);
            case NECKLACE -> descriptor.accessoryType() == AccessoryType.NECKLACE;
            case RING_LEFT, RING_RIGHT -> descriptor.accessoryType() == AccessoryType.RING;
            case BELT -> descriptor.accessoryType() == AccessoryType.BELT;
        };
    }

    public static boolean isWeapon(ItemStack stack) {
        EquipmentDescriptor descriptor = describe(stack);
        return descriptor != null && descriptor.kind() == EquipmentDescriptor.Kind.WEAPON;
    }

    public static boolean isArmor(ItemStack stack) {
        EquipmentDescriptor descriptor = describe(stack);
        return descriptor != null && descriptor.kind() == EquipmentDescriptor.Kind.ARMOR;
    }

    public static boolean isTwoHanded(ItemStack stack) {
        EquipmentDescriptor descriptor = describe(stack);
        return descriptor != null
                && descriptor.kind() == EquipmentDescriptor.Kind.WEAPON
                && descriptor.handedness() == Handedness.TWO_HANDED;
    }

    public static String handednessTranslationKey(EquipmentDescriptor descriptor) {
        if (descriptor.offHandOnly()) {
            return "tooltip.basicrpgclasses.handedness.off_weapon";
        }
        return switch (descriptor.handedness()) {
            case ONE_HANDED -> "tooltip.basicrpgclasses.handedness.one_handed";
            case VERSATILE -> "tooltip.basicrpgclasses.handedness.versatile";
            case TWO_HANDED -> "tooltip.basicrpgclasses.handedness.two_handed";
            case null -> null;
        };
    }

    private static EquipmentDescriptor profiledWeapon(WeaponProfile profile) {
        String typeKey = switch (profile.family()) {
            case STAFF -> "tooltip.basicrpgclasses.type.staff";
            case SCEPTER -> "tooltip.basicrpgclasses.type.scepter";
            case RAPIER -> "tooltip.basicrpgclasses.type.rapier";
            case ONE_HANDED_SWORD, GREATSWORD -> "tooltip.basicrpgclasses.type.sword";
            case SHIELD -> "tooltip.basicrpgclasses.type.shield";
            case WARHAMMER -> "tooltip.basicrpgclasses.type.hammer";
            case SHORTBOW -> "tooltip.basicrpgclasses.type.shortbow";
            case RECURVE_BOW -> "tooltip.basicrpgclasses.type.recurve_bow";
            case LONGBOW -> "tooltip.basicrpgclasses.type.longbow";
            case KNIFE -> "tooltip.basicrpgclasses.type.knife";
        };
        return new EquipmentDescriptor(
                EquipmentDescriptor.Kind.WEAPON,
                typeKey,
                profile.family() == WeaponFamily.ONE_HANDED_SWORD
                        ? Set.of(RpgClass.WARRIOR, RpgClass.PALADIN)
                        : Set.of(profile.requiredClass()),
                profile.handedness(),
                profile.family() == WeaponFamily.SHIELD,
                null,
                false,
                null,
                null
        );
    }

    private static EquipmentDescriptor vanillaWeapon(
            String typeKey,
            Handedness handedness,
            boolean offHandOnly,
            RpgClass requiredClass
    ) {
        return vanillaWeapon(typeKey, handedness, offHandOnly, Set.of(requiredClass));
    }

    private static EquipmentDescriptor vanillaWeapon(
            String typeKey,
            Handedness handedness,
            boolean offHandOnly,
            Set<RpgClass> allowedClasses
    ) {
        return new EquipmentDescriptor(
                EquipmentDescriptor.Kind.WEAPON,
                typeKey,
                allowedClasses,
                handedness,
                offHandOnly,
                null,
                false,
                null,
                null
        );
    }

    private static EquipmentDescriptor armor(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return null;
        }

        boolean holy = stack.is(ModItemTags.HOLY_ARMOR);
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!holy && !isNamedArmorPiece(path)) {
            return null;
        }

        ArmorWeight weight = holy || stack.is(ModItemTags.HEAVY_ARMOR) ? ArmorWeight.HEAVY : armorWeight(path);
        Set<RpgClass> classes = holy
                ? Set.of(RpgClass.PALADIN)
                : switch (weight) {
                    case LIGHT -> LIGHT_ARMOR_CLASSES;
                    case MEDIUM -> Set.of(RpgClass.HUNTER);
                    case HEAVY -> Set.of(RpgClass.WARRIOR, RpgClass.PALADIN);
                };
        String typeKey = switch (equippable.slot()) {
            case HEAD -> "tooltip.basicrpgclasses.type.helmet";
            case CHEST -> "tooltip.basicrpgclasses.type.chestplate";
            case LEGS -> "tooltip.basicrpgclasses.type.leggings";
            case FEET -> "tooltip.basicrpgclasses.type.boots";
            default -> "tooltip.basicrpgclasses.type.armor";
        };

        return new EquipmentDescriptor(
                EquipmentDescriptor.Kind.ARMOR,
                typeKey,
                classes,
                null,
                false,
                weight,
                holy,
                equippable.slot(),
                null
        );
    }

    private static AccessoryType accessoryType(ItemStack stack) {
        if (stack.getItem() instanceof RpgAccessory accessory) {
            return accessory.accessoryType();
        }
        if (stack.is(ModItemTags.NECKLACES)) {
            return AccessoryType.NECKLACE;
        }
        if (stack.is(ModItemTags.RINGS)) {
            return AccessoryType.RING;
        }
        if (stack.is(ModItemTags.BELTS)) {
            return AccessoryType.BELT;
        }
        return null;
    }

    private static ArmorWeight armorWeight(String path) {
        if (path.startsWith("iron_") || path.startsWith("diamond_") || path.startsWith("netherite_")) {
            return ArmorWeight.HEAVY;
        }
        if (path.startsWith("chainmail_") || path.startsWith("copper_") || path.startsWith("golden_")) {
            return ArmorWeight.MEDIUM;
        }
        return ArmorWeight.LIGHT;
    }

    private static boolean isNamedArmorPiece(String path) {
        return path.endsWith("_helmet")
                || path.endsWith("_chestplate")
                || path.endsWith("_leggings")
                || path.endsWith("_boots");
    }

    private EquipmentRules() {
    }
}
