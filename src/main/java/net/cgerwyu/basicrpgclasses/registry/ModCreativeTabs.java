package net.cgerwyu.basicrpgclasses.registry;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BasicRPGClasses.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.basicrpgclasses.main"))
                    .icon(() -> ModItems.APPRENTICE_STAFF.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CLASS_WORKBENCH.get());
                        output.accept(ModItems.APPRENTICE_STAFF.get());
                        output.accept(ModItems.IRON_RAPIER.get());
                        output.accept(ModItems.IRON_GREATSWORD.get());
                        output.accept(ModItems.IRON_WARHAMMER.get());
                        output.accept(ModItems.SIMPLE_SHORTBOW.get());
                        output.accept(ModItems.SIMPLE_RECURVE_BOW.get());
                        output.accept(ModItems.SIMPLE_LONGBOW.get());
                        output.accept(ModItems.HUNTING_KNIFE.get());
                        output.accept(ModItems.STONE_FANG_HELMET.get());
                        output.accept(ModItems.STONE_FANG_CHESTPLATE.get());
                        output.accept(ModItems.STONE_FANG_LEGGINGS.get());
                        output.accept(ModItems.STONE_FANG_BOOTS.get());
                        output.accept(ModItems.FIRSTFANG_HELMET.get());
                        output.accept(ModItems.FIRSTFANG_CHESTPLATE.get());
                        output.accept(ModItems.FIRSTFANG_LEGGINGS.get());
                        output.accept(ModItems.FIRSTFANG_BOOTS.get());
                        output.accept(ModItems.FIRSTFANG_HEART.get());
                        output.accept(ModItems.FIRSTFANG_SOVEREIGN_SHARD.get());
                        output.accept(ModItems.FORGEHEART_GREATSWORD.get());
                        output.accept(ModItems.CRIMSON_DRAGON_GREATSWORD.get());
                        output.accept(ModItems.OATHKEEPER_SWORD.get());
                        output.accept(ModItems.DAWNFIRE_SWORD.get());
                        output.accept(ModItems.RUNIC_BULWARK.get());
                        output.accept(ModItems.STORMGUARD_SHIELD.get());
                        output.accept(ModItems.STORMWING_STAFF.get());
                        output.accept(ModItems.FROZEN_SERPENT_STAFF.get());
                        output.accept(ModItems.BROODMOTHER_SCEPTER.get());
                        output.accept(ModItems.CRIMSON_DAWN_SCEPTER.get());
                        output.accept(ModItems.MOSSFANG_SHORTBOW.get());
                        output.accept(ModItems.DUSKSTALKER_LONGBOW.get());
                        output.accept(ModItems.THICKET_HEART_RING.get());
                        output.accept(ModItems.MOON_EYE_RING.get());
                        output.accept(ModItems.MOSS_HIDE.get());
                        output.accept(ModItems.HEART_OF_THICKET.get());
                        output.accept(ModItems.STRONG_SILK.get());
                        output.accept(ModItems.BROODMOTHER_EYE.get());
                        output.accept(ModItems.ANCIENT_PLATE.get());
                        output.accept(ModItems.RUNIC_MECHANISM.get());
                        output.accept(ModItems.STORM_FEATHER.get());
                        output.accept(ModItems.HEART_OF_STORM.get());
                        output.accept(ModItems.FROST_SCALE.get());
                        output.accept(ModItems.FROZEN_HEART.get());
                        output.accept(ModItems.DUSK_HIDE.get());
                        output.accept(ModItems.LUNAR_EYE.get());
                        output.accept(ModItems.CRIMSON_SCALE.get());
                        output.accept(ModItems.CRIMSON_HEART.get());
                        output.accept(ModItems.AMETHYST_LENS.get());
                        output.accept(ModItems.DEFENDERS_MARK.get());
                        output.accept(ModItems.QUIVER_CLASP.get());
                        output.accept(ModItems.WIND_FEATHER.get());
                        output.accept(ModItems.ENDER_CHARM.get());
                        output.accept(ModItems.ARMOR_REINFORCEMENT_KIT.get());
                        output.accept(ModItems.SHARPENING_STONE.get());
                        output.accept(ModItems.SIMPLE_POISON.get());
                        output.accept(ModItems.HEALING_CRYSTAL.get());
                        output.accept(ModItems.CONJURED_RATION.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }

    private ModCreativeTabs() {
    }
}
