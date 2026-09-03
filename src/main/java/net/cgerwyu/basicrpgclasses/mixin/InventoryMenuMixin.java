package net.cgerwyu.basicrpgclasses.mixin;

import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentContainer;
import net.cgerwyu.basicrpgclasses.equipment.RpgEquipmentMenuSlot;
import net.cgerwyu.basicrpgclasses.equipment.RpgEquipmentSlotType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void basicrpgclasses$addEquipmentSlots(
            Inventory inventory,
            boolean active,
            Player owner,
            CallbackInfo callback
    ) {
        InventoryMenu inventoryMenu = (InventoryMenu) (Object) this;

        // Make room below the character paper-doll for its two weapon slots.
        for (int index = 9; index < 45; index++) {
            SlotAccessor slot = (SlotAccessor) inventoryMenu.getSlot(index);
            slot.basicrpgclasses$setY(inventoryMenu.getSlot(index).y + 21);
        }

        // The RPG off-hand slot replaces the vanilla shield slot.
        SlotAccessor vanillaOffHand = (SlotAccessor) inventoryMenu.getSlot(45);
        vanillaOffHand.basicrpgclasses$setX(-2000);
        vanillaOffHand.basicrpgclasses$setY(-2000);

        PlayerEquipmentContainer equipment = new PlayerEquipmentContainer(owner);
        AbstractContainerMenuAccessor menu = (AbstractContainerMenuAccessor) this;

        menu.basicrpgclasses$addSlot(new RpgEquipmentMenuSlot(
                equipment, owner, RpgEquipmentSlotType.MAIN_WEAPON, 28, 80
        ));
        menu.basicrpgclasses$addSlot(new RpgEquipmentMenuSlot(
                equipment, owner, RpgEquipmentSlotType.OFF_WEAPON, 48, 80
        ));
        menu.basicrpgclasses$addSlot(new RpgEquipmentMenuSlot(
                equipment, owner, RpgEquipmentSlotType.NECKLACE, 77, 8
        ));
        menu.basicrpgclasses$addSlot(new RpgEquipmentMenuSlot(
                equipment, owner, RpgEquipmentSlotType.RING_LEFT, 77, 26
        ));
        menu.basicrpgclasses$addSlot(new RpgEquipmentMenuSlot(
                equipment, owner, RpgEquipmentSlotType.RING_RIGHT, 77, 44
        ));
        menu.basicrpgclasses$addSlot(new RpgEquipmentMenuSlot(
                equipment, owner, RpgEquipmentSlotType.BELT, 77, 62
        ));
    }
}
