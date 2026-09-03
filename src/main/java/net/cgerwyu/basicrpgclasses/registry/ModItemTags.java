package net.cgerwyu.basicrpgclasses.registry;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> HOLY_ARMOR = tag("holy_armor");
    public static final TagKey<Item> HEAVY_ARMOR = tag("heavy_armor");
    public static final TagKey<Item> REPAIRS_STONE_FANG_ARMOR = tag("repairs_stone_fang_armor");
    public static final TagKey<Item> REPAIRS_FIRSTFANG_ARMOR = tag("repairs_firstfang_armor");
    public static final TagKey<Item> NECKLACES = tag("accessories/necklaces");
    public static final TagKey<Item> RINGS = tag("accessories/rings");
    public static final TagKey<Item> BELTS = tag("accessories/belts");

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, BasicRPGClasses.id(path));
    }

    private ModItemTags() {
    }
}
