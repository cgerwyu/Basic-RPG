package net.cgerwyu.basicrpgclasses.event;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BasicRPGClasses.MODID)
public final class ClassEquipmentEvents {
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!canUse(player, player.getMainHandItem())) {
            event.setCanceled(true);
            warnWrongClass(player);
        } else if (EquipmentRules.isWeapon(player.getMainHandItem())
                && !PlayerEquipmentManager.isEquippedWeapon(player, player.getMainHandItem())) {
            event.setCanceled(true);
            warnEquipFirst(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!canUse(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            warnWrongClass(event.getEntity());
        } else if (EquipmentRules.isWeapon(event.getItemStack())
                && !PlayerEquipmentManager.isEquippedWeapon(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            warnEquipFirst(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            PlayerEquipmentManager.keepWeaponsOutOfHotbar(player);
            PlayerEquipmentManager.refreshWeaponAttributes(player);
            if (PlayerEquipmentManager.unequipInvalid(player)) {
                player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.invalid_equipment_removed"));
            }
        }
    }

    private static boolean canUse(Player player, ItemStack stack) {
        return EquipmentRules.canUse(player, stack);
    }

    private static void warnWrongClass(Player player) {
        if (!player.level().isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.wrong_equipment_class"));
        }
    }

    private static void warnEquipFirst(Player player) {
        if (!player.level().isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.weapon_must_be_equipped"));
        }
    }

    private ClassEquipmentEvents() {
    }
}
