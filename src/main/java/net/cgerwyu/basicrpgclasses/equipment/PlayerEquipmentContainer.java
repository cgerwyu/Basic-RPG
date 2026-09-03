package net.cgerwyu.basicrpgclasses.equipment;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerEquipmentContainer implements Container {
    private final Player owner;

    public PlayerEquipmentContainer(Player owner) {
        this.owner = owner;
    }

    @Override
    public int getContainerSize() {
        return RpgEquipmentSlotType.COUNT;
    }

    @Override
    public boolean isEmpty() {
        return owner.getData(ModAttachments.PLAYER_EQUIPMENT).stacks().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= RpgEquipmentSlotType.COUNT) {
            return ItemStack.EMPTY;
        }
        return owner.getData(ModAttachments.PLAYER_EQUIPMENT).stacks().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack current = getItem(slot);
        if (current.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        // Do not split the attachment-owned stack in place: the equipment manager
        // needs the intact previous stack to remove its active attribute modifiers.
        ItemStack remaining = current.copy();
        ItemStack removed = remaining.split(count);
        setRaw(slot, remaining);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        if (!current.isEmpty()) {
            setRaw(slot, ItemStack.EMPTY);
        }
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < RpgEquipmentSlotType.COUNT) {
            setRaw(slot, stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        owner.setData(
                ModAttachments.PLAYER_EQUIPMENT,
                owner.getData(ModAttachments.PLAYER_EQUIPMENT).copied()
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive();
    }

    @Override
    public void clearContent() {
        for (RpgEquipmentSlotType slotType : RpgEquipmentSlotType.values()) {
            PlayerEquipmentManager.set(owner, slotType, ItemStack.EMPTY);
        }
    }

    private void setRaw(int slot, ItemStack stack) {
        RpgEquipmentSlotType slotType = RpgEquipmentSlotType.values()[slot];
        PlayerEquipmentManager.set(owner, slotType, stack);
    }
}
