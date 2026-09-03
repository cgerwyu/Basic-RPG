package net.cgerwyu.basicrpgclasses.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RpgEquipmentMenuSlot extends Slot {
    private final Player owner;
    private final RpgEquipmentSlotType slotType;

    public RpgEquipmentMenuSlot(
            PlayerEquipmentContainer container,
            Player owner,
            RpgEquipmentSlotType slotType,
            int x,
            int y
    ) {
        super(container, slotType.index(), x, y);
        this.owner = owner;
        this.slotType = slotType;
    }

    public RpgEquipmentSlotType slotType() {
        return slotType;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return EquipmentRules.canPlace(owner, slotType, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isActive() {
        return slotType != RpgEquipmentSlotType.OFF_WEAPON
                || !EquipmentRules.isTwoHanded(PlayerEquipmentManager.get(owner, RpgEquipmentSlotType.MAIN_WEAPON));
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        super.setByPlayer(stack, previous);
        EquipmentSlot vanillaSlot = switch (slotType) {
            case MAIN_WEAPON -> EquipmentSlot.MAINHAND;
            case OFF_WEAPON -> EquipmentSlot.OFFHAND;
            default -> null;
        };
        if (vanillaSlot != null) {
            owner.onEquipItem(vanillaSlot, previous, stack);
        }
    }
}
