package net.cgerwyu.basicrpgclasses.mixin;

import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipmentMixin {
    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void basicrpgclasses$getRpgWeapon(
            EquipmentSlot slot,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        if ((Object) this instanceof Player player && slot.getType() == EquipmentSlot.Type.HAND) {
            ItemStack custom = PlayerEquipmentManager.customHandItem(player, slot);
            if (!custom.isEmpty() && PlayerEquipmentManager.combatModeActive(player)) {
                callback.setReturnValue(custom);
            }
        }
    }

    @Inject(
            method = "setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void basicrpgclasses$setRpgWeapon(
            EquipmentSlot slot,
            ItemStack stack,
            boolean insideTransaction,
            CallbackInfo callback
    ) {
        if (!((Object) this instanceof Player player) || slot.getType() != EquipmentSlot.Type.HAND) {
            return;
        }

        ItemStack previous = PlayerEquipmentManager.customHandItem(player, slot);
        if (previous.isEmpty() && !EquipmentRules.isWeapon(stack)) {
            return;
        }

        PlayerEquipmentManager.setCustomHandItem(player, slot, stack);
        if (!insideTransaction) {
            player.onEquipItem(slot, previous, stack);
        }
        callback.cancel();
    }
}
