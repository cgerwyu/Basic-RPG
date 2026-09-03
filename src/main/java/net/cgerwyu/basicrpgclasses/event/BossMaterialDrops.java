package net.cgerwyu.basicrpgclasses.event;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = BasicRPGClasses.MODID)
public final class BossMaterialDrops {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity defeated = event.getEntity();
        if (defeated.level().isClientSide() || !(event.getSource().getEntity() instanceof Player)) {
            return;
        }

        if (defeated instanceof Ravager) {
            drop(event, ModItems.MOSS_HIDE.get(), 1, 1.0F);
            drop(event, ModItems.HEART_OF_THICKET.get(), 1, 0.18F);
        } else if (defeated instanceof CaveSpider) {
            drop(event, ModItems.STRONG_SILK.get(), 1, 0.20F);
            drop(event, ModItems.BROODMOTHER_EYE.get(), 1, 0.035F);
        } else if (defeated instanceof IronGolem) {
            drop(event, ModItems.ANCIENT_PLATE.get(), 1, 0.35F);
            drop(event, ModItems.RUNIC_MECHANISM.get(), 1, 0.065F);
        } else if (defeated instanceof Phantom) {
            drop(event, ModItems.STORM_FEATHER.get(), 1, 0.30F);
            drop(event, ModItems.HEART_OF_STORM.get(), 1, 0.055F);
        } else if (defeated instanceof Stray) {
            drop(event, ModItems.FROST_SCALE.get(), 1, 0.22F);
            drop(event, ModItems.FROZEN_HEART.get(), 1, 0.045F);
        } else if (defeated instanceof EnderMan) {
            drop(event, ModItems.DUSK_HIDE.get(), 1, 0.14F);
            drop(event, ModItems.LUNAR_EYE.get(), 1, 0.025F);
        } else if (defeated instanceof EnderDragon) {
            drop(event, ModItems.CRIMSON_SCALE.get(), 8, 1.0F);
            drop(event, ModItems.CRIMSON_HEART.get(), 1, 1.0F);
        }
    }

    private static void drop(LivingDropsEvent event, Item item, int count, float chance) {
        LivingEntity defeated = event.getEntity();
        if (defeated.getRandom().nextFloat() >= chance) {
            return;
        }
        event.getDrops().add(new ItemEntity(
                defeated.level(), defeated.getX(), defeated.getY(), defeated.getZ(), new ItemStack(item, count)
        ));
    }

    private BossMaterialDrops() {
    }
}
