package net.cgerwyu.basicrpgclasses.registry;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.skill.entity.MageFireball;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(BasicRPGClasses.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MageFireball>> MAGE_FIREBALL =
            ENTITIES.registerEntityType(
                    "mage_fireball",
                    MageFireball::new,
                    MobCategory.MISC,
                    builder -> builder
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .fireImmune()
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    private ModEntities() {
    }
}
