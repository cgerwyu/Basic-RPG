package net.cgerwyu.basicrpgclasses.registry;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BasicRPGClasses.MODID);

    public static final DeferredBlock<Block> CLASS_WORKBENCH = BLOCKS.registerSimpleBlock(
            "class_workbench",
            properties -> properties.mapColor(MapColor.WOOD).strength(2.5F)
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private ModBlocks() {
    }
}
