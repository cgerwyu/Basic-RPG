package net.cgerwyu.basicrpgclasses.mixin;

import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Inject(
            method = "add(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void basicrpgclasses$keepPickedUpWeaponsOutOfHotbar(
            int requestedSlot,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (requestedSlot != -1 || !EquipmentRules.isWeapon(stack)) {
            return;
        }

        Inventory inventory = (Inventory) (Object) this;
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                callback.setReturnValue(inventory.add(slot, stack));
                return;
            }
        }

        // A full main inventory means the pickup must remain in the world; the
        // hotbar is never used as an overflow path for weapons.
        callback.setReturnValue(false);
    }
}
