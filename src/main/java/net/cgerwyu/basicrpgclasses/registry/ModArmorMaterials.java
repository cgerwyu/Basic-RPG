package net.cgerwyu.basicrpgclasses.registry;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

public final class ModArmorMaterials {
    public static final ResourceKey<EquipmentAsset> STONE_FANG_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, BasicRPGClasses.id("stone_fang")
    );
    public static final ResourceKey<EquipmentAsset> FIRSTFANG_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, BasicRPGClasses.id("firstfang")
    );

    public static final ArmorMaterial STONE_FANG = new ArmorMaterial(
            60,
            defense(5, 9, 10, 6),
            12,
            SoundEvents.ARMOR_EQUIP_TURTLE,
            4.0F,
            0.10F,
            ModItemTags.REPAIRS_STONE_FANG_ARMOR,
            STONE_FANG_ASSET
    );

    public static final ArmorMaterial FIRSTFANG = new ArmorMaterial(
            85,
            defense(6, 11, 12, 7),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            6.0F,
            0.15F,
            ModItemTags.REPAIRS_FIRSTFANG_ARMOR,
            FIRSTFANG_ASSET
    );

    private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        return Map.of(
                ArmorType.BOOTS, boots,
                ArmorType.LEGGINGS, leggings,
                ArmorType.CHESTPLATE, chestplate,
                ArmorType.HELMET, helmet,
                ArmorType.BODY, chestplate
        );
    }

    private ModArmorMaterials() {
    }
}
