package net.cgerwyu.basicrpgclasses.mixin;

import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final private int slot;
    @Shadow @Final public Container container;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void basicrpgclasses$keepWeaponsOutOfHotbar(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (container instanceof Inventory && slot >= 0 && slot < 9 && EquipmentRules.isWeapon(stack)) {
            callback.setReturnValue(false);
        }
    }
}
