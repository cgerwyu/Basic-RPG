package net.cgerwyu.basicrpgclasses.registry;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.item.HunterBowItem;
import net.cgerwyu.basicrpgclasses.item.AccessoryItem;
import net.cgerwyu.basicrpgclasses.item.LoreItem;
import net.cgerwyu.basicrpgclasses.item.MageStaffItem;
import net.cgerwyu.basicrpgclasses.item.PaladinShieldItem;
import net.cgerwyu.basicrpgclasses.item.RpgWeaponItem;
import net.cgerwyu.basicrpgclasses.item.RpgArmorItem;
import net.cgerwyu.basicrpgclasses.weapon.WeaponProfile;
import net.cgerwyu.basicrpgclasses.weapon.WeaponProfiles;
import net.cgerwyu.basicrpgclasses.equipment.AccessoryType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.component.BlocksAttacks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BasicRPGClasses.MODID);

    public static final DeferredItem<BlockItem> CLASS_WORKBENCH = ITEMS.registerSimpleBlockItem(ModBlocks.CLASS_WORKBENCH);

    public static final DeferredItem<MageStaffItem> APPRENTICE_STAFF = ITEMS.registerItem(
            "apprentice_staff", properties -> new MageStaffItem(WeaponProfiles.APPRENTICE_STAFF, properties.durability(160))
    );
    public static final DeferredItem<RpgWeaponItem> IRON_RAPIER = ironWeapon("iron_rapier", WeaponProfiles.IRON_RAPIER, 280, 2.0F, -2.2F);
    public static final DeferredItem<RpgWeaponItem> IRON_GREATSWORD = ironWeapon("iron_greatsword", WeaponProfiles.IRON_GREATSWORD, 420, 5.0F, -3.2F);
    public static final DeferredItem<RpgWeaponItem> IRON_WARHAMMER = ironWeapon("iron_warhammer", WeaponProfiles.IRON_WARHAMMER, 480, 7.0F, -3.35F);
    public static final DeferredItem<HunterBowItem> SIMPLE_SHORTBOW = bow("simple_shortbow", WeaponProfiles.SIMPLE_SHORTBOW, 260);
    public static final DeferredItem<HunterBowItem> SIMPLE_RECURVE_BOW = bow("simple_recurve_bow", WeaponProfiles.SIMPLE_RECURVE_BOW, 360);
    public static final DeferredItem<HunterBowItem> SIMPLE_LONGBOW = bow("simple_longbow", WeaponProfiles.SIMPLE_LONGBOW, 440);
    public static final DeferredItem<RpgWeaponItem> HUNTING_KNIFE = ironWeapon("hunting_knife", WeaponProfiles.HUNTING_KNIFE, 220, 1.0F, -2.0F);

    public static final DeferredItem<RpgArmorItem> STONE_FANG_HELMET = armor(
            "stone_fang_helmet", ModArmorMaterials.STONE_FANG, ArmorType.HELMET
    );
    public static final DeferredItem<RpgArmorItem> STONE_FANG_CHESTPLATE = armor(
            "stone_fang_chestplate", ModArmorMaterials.STONE_FANG, ArmorType.CHESTPLATE
    );
    public static final DeferredItem<RpgArmorItem> STONE_FANG_LEGGINGS = armor(
            "stone_fang_leggings", ModArmorMaterials.STONE_FANG, ArmorType.LEGGINGS
    );
    public static final DeferredItem<RpgArmorItem> STONE_FANG_BOOTS = armor(
            "stone_fang_boots", ModArmorMaterials.STONE_FANG, ArmorType.BOOTS
    );

    public static final DeferredItem<RpgArmorItem> FIRSTFANG_HELMET = armor(
            "firstfang_helmet", ModArmorMaterials.FIRSTFANG, ArmorType.HELMET
    );
    public static final DeferredItem<RpgArmorItem> FIRSTFANG_CHESTPLATE = armor(
            "firstfang_chestplate", ModArmorMaterials.FIRSTFANG, ArmorType.CHESTPLATE
    );
    public static final DeferredItem<RpgArmorItem> FIRSTFANG_LEGGINGS = armor(
            "firstfang_leggings", ModArmorMaterials.FIRSTFANG, ArmorType.LEGGINGS
    );
    public static final DeferredItem<RpgArmorItem> FIRSTFANG_BOOTS = armor(
            "firstfang_boots", ModArmorMaterials.FIRSTFANG, ArmorType.BOOTS
    );
    public static final DeferredItem<LoreItem> FIRSTFANG_HEART = loreItem("firstfang_heart", 16);
    public static final DeferredItem<LoreItem> FIRSTFANG_SOVEREIGN_SHARD = loreItem("firstfang_sovereign_shard", 16);

    public static final DeferredItem<RpgWeaponItem> FORGEHEART_GREATSWORD = loreWeapon(
            "forgeheart_greatsword", WeaponProfiles.FORGEHEART_GREATSWORD, 720, 7.0F, -3.05F
    );
    public static final DeferredItem<RpgWeaponItem> CRIMSON_DRAGON_GREATSWORD = loreWeapon(
            "crimson_dragon_greatsword", WeaponProfiles.CRIMSON_DRAGON_GREATSWORD, 980, 8.5F, -2.95F
    );
    public static final DeferredItem<RpgWeaponItem> OATHKEEPER_SWORD = loreWeapon(
            "oathkeeper_sword", WeaponProfiles.OATHKEEPER_SWORD, 620, 3.5F, -2.35F
    );
    public static final DeferredItem<RpgWeaponItem> DAWNFIRE_SWORD = loreWeapon(
            "dawnfire_sword", WeaponProfiles.DAWNFIRE_SWORD, 840, 4.5F, -2.30F
    );
    public static final DeferredItem<PaladinShieldItem> RUNIC_BULWARK = shield(
            "runic_bulwark", WeaponProfiles.RUNIC_BULWARK, 640
    );
    public static final DeferredItem<PaladinShieldItem> STORMGUARD_SHIELD = shield(
            "stormguard_shield", WeaponProfiles.STORMGUARD_SHIELD, 760
    );
    public static final DeferredItem<RpgWeaponItem> STORMWING_STAFF = casterWeapon(
            "stormwing_staff", WeaponProfiles.STORMWING_STAFF, 560
    );
    public static final DeferredItem<RpgWeaponItem> FROZEN_SERPENT_STAFF = casterWeapon(
            "frozen_serpent_staff", WeaponProfiles.FROZEN_SERPENT_STAFF, 680
    );
    public static final DeferredItem<RpgWeaponItem> BROODMOTHER_SCEPTER = casterWeapon(
            "broodmother_scepter", WeaponProfiles.BROODMOTHER_SCEPTER, 480
    );
    public static final DeferredItem<RpgWeaponItem> CRIMSON_DAWN_SCEPTER = casterWeapon(
            "crimson_dawn_scepter", WeaponProfiles.CRIMSON_DAWN_SCEPTER, 720
    );
    public static final DeferredItem<HunterBowItem> MOSSFANG_SHORTBOW = loreBow(
            "mossfang_shortbow", WeaponProfiles.MOSSFANG_SHORTBOW, 520
    );
    public static final DeferredItem<HunterBowItem> DUSKSTALKER_LONGBOW = loreBow(
            "duskstalker_longbow", WeaponProfiles.DUSKSTALKER_LONGBOW, 760
    );

    public static final DeferredItem<AccessoryItem> THICKET_HEART_RING = accessory(
            "thicket_heart_ring", AccessoryType.RING
    );
    public static final DeferredItem<AccessoryItem> MOON_EYE_RING = accessory(
            "moon_eye_ring", AccessoryType.RING
    );

    public static final DeferredItem<LoreItem> MOSS_HIDE = loreItem("moss_hide", 64);
    public static final DeferredItem<LoreItem> HEART_OF_THICKET = loreItem("heart_of_thicket", 16);
    public static final DeferredItem<LoreItem> STRONG_SILK = loreItem("strong_silk", 64);
    public static final DeferredItem<LoreItem> BROODMOTHER_EYE = loreItem("broodmother_eye", 16);
    public static final DeferredItem<LoreItem> ANCIENT_PLATE = loreItem("ancient_plate", 64);
    public static final DeferredItem<LoreItem> RUNIC_MECHANISM = loreItem("runic_mechanism", 16);
    public static final DeferredItem<LoreItem> STORM_FEATHER = loreItem("storm_feather", 64);
    public static final DeferredItem<LoreItem> HEART_OF_STORM = loreItem("heart_of_storm", 16);
    public static final DeferredItem<LoreItem> FROST_SCALE = loreItem("frost_scale", 64);
    public static final DeferredItem<LoreItem> FROZEN_HEART = loreItem("frozen_heart", 16);
    public static final DeferredItem<LoreItem> DUSK_HIDE = loreItem("dusk_hide", 64);
    public static final DeferredItem<LoreItem> LUNAR_EYE = loreItem("lunar_eye", 16);
    public static final DeferredItem<LoreItem> CRIMSON_SCALE = loreItem("crimson_scale", 64);
    public static final DeferredItem<LoreItem> CRIMSON_HEART = loreItem("crimson_heart", 16);

    public static final DeferredItem<Item> AMETHYST_LENS = material("amethyst_lens", 16);
    public static final DeferredItem<Item> DEFENDERS_MARK = material("defenders_mark", 16);
    public static final DeferredItem<Item> QUIVER_CLASP = material("quiver_clasp", 16);
    public static final DeferredItem<Item> WIND_FEATHER = material("wind_feather", 32);
    public static final DeferredItem<Item> ENDER_CHARM = material("ender_charm", 16);
    public static final DeferredItem<Item> ARMOR_REINFORCEMENT_KIT = material("armor_reinforcement_kit", 16);
    public static final DeferredItem<Item> SHARPENING_STONE = material("sharpening_stone", 16);
    public static final DeferredItem<Item> SIMPLE_POISON = material("simple_poison", 16);
    public static final DeferredItem<Item> HEALING_CRYSTAL = material("healing_crystal", 16);
    public static final DeferredItem<Item> CONJURED_RATION = material("conjured_ration", 16);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static DeferredItem<RpgWeaponItem> ironWeapon(
            String name,
            net.cgerwyu.basicrpgclasses.weapon.WeaponProfile profile,
            int durability,
            float attackDamage,
            float attackSpeed
    ) {
        return ITEMS.registerItem(name, properties -> new RpgWeaponItem(
                profile,
                properties.sword(ToolMaterial.IRON, attackDamage, attackSpeed).durability(durability)
        ));
    }

    private static DeferredItem<RpgArmorItem> armor(String name, ArmorMaterial material, ArmorType type) {
        return ITEMS.registerItem(name, properties -> new RpgArmorItem(
                name,
                properties
                        .durability(type.getDurability(material.durability()))
                        .attributes(material.createAttributes(type))
                        .enchantable(material.enchantmentValue())
                        .component(
                                DataComponents.EQUIPPABLE,
                                Equippable.builder(type.getSlot())
                                        .setEquipSound(material.equipSound())
                                        .setAsset(material.assetId())
                                        .build()
                        )
                        .repairable(material.repairIngredient())
                        .component(DataComponents.BREAK_SOUND, SoundEvents.ITEM_BREAK)
        ));
    }

    private static DeferredItem<RpgWeaponItem> loreWeapon(
            String name, WeaponProfile profile, int durability, float attackDamage, float attackSpeed
    ) {
        return ITEMS.registerItem(name, properties -> new RpgWeaponItem(
                profile,
                properties.sword(ToolMaterial.DIAMOND, attackDamage, attackSpeed).durability(durability),
                name
        ));
    }

    private static DeferredItem<RpgWeaponItem> casterWeapon(String name, WeaponProfile profile, int durability) {
        return ITEMS.registerItem(name, properties -> new RpgWeaponItem(
                profile, properties.durability(durability), name
        ));
    }

    private static DeferredItem<PaladinShieldItem> shield(String name, WeaponProfile profile, int durability) {
        return ITEMS.registerItem(name, properties -> new PaladinShieldItem(
                profile,
                name,
                properties.durability(durability)
                        .equippableUnswappable(EquipmentSlot.OFFHAND)
                        .delayedComponent(
                                DataComponents.BLOCKS_ATTACKS,
                                context -> new BlocksAttacks(
                                        0.25F,
                                        1.0F,
                                        List.of(new BlocksAttacks.DamageReduction(
                                                90.0F, Optional.empty(), 0.0F, 1.0F
                                        )),
                                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                                        Optional.of(SoundEvents.SHIELD_BLOCK),
                                        Optional.of(SoundEvents.SHIELD_BREAK)
                                )
                        )
                        .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
        ));
    }

    private static DeferredItem<HunterBowItem> bow(String name, net.cgerwyu.basicrpgclasses.weapon.WeaponProfile profile, int durability) {
        return ITEMS.registerItem(name, properties -> new HunterBowItem(profile, properties.durability(durability)));
    }

    private static DeferredItem<HunterBowItem> loreBow(String name, WeaponProfile profile, int durability) {
        return ITEMS.registerItem(name, properties -> new HunterBowItem(
                profile, properties.durability(durability), name
        ));
    }

    private static DeferredItem<Item> material(String name, int stackSize) {
        return ITEMS.registerSimpleItem(name, properties -> properties.stacksTo(stackSize));
    }

    private static DeferredItem<LoreItem> loreItem(String name, int stackSize) {
        return ITEMS.registerItem(name, properties -> new LoreItem(name, properties.stacksTo(stackSize)));
    }

    private static DeferredItem<AccessoryItem> accessory(String name, AccessoryType type) {
        return ITEMS.registerItem(name, properties -> new AccessoryItem(type, name, properties.stacksTo(1)));
    }

    private ModItems() {
    }
}
