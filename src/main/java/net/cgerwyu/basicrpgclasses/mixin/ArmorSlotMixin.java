package net.cgerwyu.basicrpgclasses.mixin;

import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorSlot.class)
public abstract class ArmorSlotMixin {
    @Shadow @Final private LivingEntity owner;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void basicrpgclasses$checkArmorClass(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (owner instanceof Player player && EquipmentRules.isArmor(stack) && !EquipmentRules.canUse(player, stack)) {
            callback.setReturnValue(false);
        }
    }
}
