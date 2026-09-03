package net.cgerwyu.basicrpgclasses;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.network.ModPayloads;
import net.cgerwyu.basicrpgclasses.registry.ModBlocks;
import net.cgerwyu.basicrpgclasses.registry.ModCreativeTabs;
import net.cgerwyu.basicrpgclasses.registry.ModEntities;
import net.cgerwyu.basicrpgclasses.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BasicRPGClasses.MODID)
public final class BasicRPGClasses {
    public static final String MODID = "basicrpgclasses";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public BasicRPGClasses(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
