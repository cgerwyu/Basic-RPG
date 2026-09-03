package net.cgerwyu.basicrpgclasses.equipment;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.client.CombatModeController;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.List;

public final class PlayerEquipmentManager {
    public static final int FIRST_CUSTOM_MENU_SLOT = 46;
    public static final int MAIN_WEAPON_MENU_SLOT = FIRST_CUSTOM_MENU_SLOT;
    public static final int OFF_WEAPON_MENU_SLOT = FIRST_CUSTOM_MENU_SLOT + 1;
    public static final int NECKLACE_MENU_SLOT = FIRST_CUSTOM_MENU_SLOT + 2;
    public static final int RING_LEFT_MENU_SLOT = FIRST_CUSTOM_MENU_SLOT + 3;
    public static final int RING_RIGHT_MENU_SLOT = FIRST_CUSTOM_MENU_SLOT + 4;
    public static final int BELT_MENU_SLOT = FIRST_CUSTOM_MENU_SLOT + 5;
    public static final int LAST_CUSTOM_MENU_SLOT = BELT_MENU_SLOT;

    public static ItemStack get(Player player, RpgEquipmentSlotType slotType) {
        return player.getData(ModAttachments.PLAYER_EQUIPMENT).get(slotType);
    }

    public static boolean hasMainWeapon(Player player) {
        return !get(player, RpgEquipmentSlotType.MAIN_WEAPON).isEmpty();
    }

    public static ItemStack customHandItem(Player player, EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> get(player, RpgEquipmentSlotType.MAIN_WEAPON);
            case OFFHAND -> get(player, RpgEquipmentSlotType.OFF_WEAPON);
            default -> ItemStack.EMPTY;
        };
    }

    public static void setCustomHandItem(Player player, EquipmentSlot slot, ItemStack stack) {
        RpgEquipmentSlotType slotType = switch (slot) {
            case MAINHAND -> RpgEquipmentSlotType.MAIN_WEAPON;
            case OFFHAND -> RpgEquipmentSlotType.OFF_WEAPON;
            default -> null;
        };
        if (slotType != null) {
            set(player, slotType, stack);
        }
    }

    public static void set(Player player, RpgEquipmentSlotType slotType, ItemStack stack) {
        ItemStack previous = get(player, slotType);
        player.setData(
                ModAttachments.PLAYER_EQUIPMENT,
                player.getData(ModAttachments.PLAYER_EQUIPMENT).with(slotType, stack)
        );
        EquipmentSlot vanillaSlot = vanillaHandSlot(slotType);
        if (!player.level().isClientSide() && vanillaSlot != null) {
            updateWeaponAttributes(player, vanillaSlot, previous, stack);
        }
    }

    public static boolean combatModeActive(Player player) {
        if (player.level().isClientSide() && player.isLocalPlayer()) {
            return CombatModeController.active();
        }
        return player.getData(ModAttachments.PLAYER_COMBAT_MODE).active();
    }

    public static void refreshWeaponAttributes(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        boolean active = player.getData(ModAttachments.PLAYER_COMBAT_MODE).active();
        refreshSlotAttributes(player, EquipmentSlot.MAINHAND, get(player, RpgEquipmentSlotType.MAIN_WEAPON), active);
        refreshSlotAttributes(player, EquipmentSlot.OFFHAND, get(player, RpgEquipmentSlotType.OFF_WEAPON), active);
    }

    public static boolean keepWeaponsOutOfHotbar(ServerPlayer player) {
        boolean changed = false;
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack stack = player.getInventory().getItem(hotbarSlot);
            if (!EquipmentRules.isWeapon(stack)) {
                continue;
            }

            int inventorySlot = firstEmptyMainInventorySlot(player);
            player.getInventory().setItem(hotbarSlot, ItemStack.EMPTY);
            if (inventorySlot >= 0) {
                player.getInventory().setItem(inventorySlot, stack);
            } else {
                player.drop(stack, false);
            }
            changed = true;
        }
        if (changed) {
            player.inventoryMenu.broadcastChanges();
        }
        return changed;
    }

    public static boolean isEquippedWeapon(Player player, ItemStack stack) {
        return stack == get(player, RpgEquipmentSlotType.MAIN_WEAPON)
                || stack == get(player, RpgEquipmentSlotType.OFF_WEAPON);
    }

    public static boolean equipFromInventory(ServerPlayer player, int sourceSlotIndex) {
        InventoryMenu menu = player.inventoryMenu;
        if (sourceSlotIndex < InventoryMenu.INV_SLOT_START
                || sourceSlotIndex >= InventoryMenu.USE_ROW_SLOT_END
                || sourceSlotIndex >= menu.slots.size()) {
            return false;
        }

        Slot source = menu.getSlot(sourceSlotIndex);
        ItemStack sourceStack = source.getItem();
        EquipmentDescriptor descriptor = EquipmentRules.describe(sourceStack);
        if (descriptor == null) {
            return false;
        }
        if (!EquipmentRules.canUse(player, sourceStack)) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.wrong_equipment_class"));
            return false;
        }

        int targetSlotIndex = targetMenuSlot(menu, descriptor);
        if (targetSlotIndex < 0 || targetSlotIndex >= menu.slots.size()) {
            return false;
        }

        if (targetSlotIndex == MAIN_WEAPON_MENU_SLOT
                && descriptor.handedness() == net.cgerwyu.basicrpgclasses.weapon.Handedness.TWO_HANDED) {
            moveOffWeaponToInventory(player, menu);
        }

        Slot target = menu.getSlot(targetSlotIndex);
        if (!target.mayPlace(sourceStack)) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.equipment_slot_blocked"));
            return false;
        }

        ItemStack incoming = sourceStack.copyAndClear();
        ItemStack previous = target.getItem().copyAndClear();
        target.setByPlayer(incoming, previous);
        source.setByPlayer(previous, incoming);
        target.setChanged();
        source.setChanged();
        menu.broadcastChanges();
        player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.item_equipped", incoming.getHoverName()));
        return true;
    }

    public static int menuSlot(RpgEquipmentSlotType slotType) {
        return FIRST_CUSTOM_MENU_SLOT + slotType.index();
    }

    public static boolean unequipInvalid(ServerPlayer player) {
        boolean changed = false;
        for (RpgEquipmentSlotType slotType : List.of(
                RpgEquipmentSlotType.MAIN_WEAPON,
                RpgEquipmentSlotType.OFF_WEAPON
        )) {
            ItemStack stack = get(player, slotType);
            if (!stack.isEmpty() && !EquipmentRules.canUse(player, stack)) {
                setCustomHandItem(
                        player,
                        slotType == RpgEquipmentSlotType.MAIN_WEAPON ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND,
                        ItemStack.EMPTY
                );
                returnToInventory(player, stack);
                changed = true;
            }
        }

        ItemStack main = get(player, RpgEquipmentSlotType.MAIN_WEAPON);
        ItemStack off = get(player, RpgEquipmentSlotType.OFF_WEAPON);
        if (EquipmentRules.isTwoHanded(main) && !off.isEmpty()) {
            setCustomHandItem(player, EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            returnToInventory(player, off);
            changed = true;
        }

        for (EquipmentSlot slot : List.of(
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        )) {
            ItemStack stack = player.getItemBySlot(slot);
            if (EquipmentRules.isArmor(stack) && !EquipmentRules.canUse(player, stack)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                returnToInventory(player, stack);
                changed = true;
            }
        }
        return changed;
    }

    private static int targetMenuSlot(InventoryMenu menu, EquipmentDescriptor descriptor) {
        if (descriptor.kind() == EquipmentDescriptor.Kind.ARMOR) {
            return 8 - descriptor.armorSlot().getIndex();
        }
        if (descriptor.kind() == EquipmentDescriptor.Kind.WEAPON) {
            return descriptor.offHandOnly() ? OFF_WEAPON_MENU_SLOT : MAIN_WEAPON_MENU_SLOT;
        }
        return switch (descriptor.accessoryType()) {
            case NECKLACE -> NECKLACE_MENU_SLOT;
            case BELT -> BELT_MENU_SLOT;
            case RING -> menu.getSlot(RING_LEFT_MENU_SLOT).hasItem() && !menu.getSlot(RING_RIGHT_MENU_SLOT).hasItem()
                    ? RING_RIGHT_MENU_SLOT
                    : RING_LEFT_MENU_SLOT;
        };
    }

    private static void moveOffWeaponToInventory(ServerPlayer player, InventoryMenu menu) {
        Slot offWeapon = menu.getSlot(OFF_WEAPON_MENU_SLOT);
        if (!offWeapon.hasItem()) {
            return;
        }

        ItemStack displaced = offWeapon.getItem().copyAndClear();
        offWeapon.setByPlayer(ItemStack.EMPTY, displaced);
        offWeapon.setChanged();
        returnToInventory(player, displaced);
    }

    private static void returnToInventory(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static EquipmentSlot vanillaHandSlot(RpgEquipmentSlotType slotType) {
        return switch (slotType) {
            case MAIN_WEAPON -> EquipmentSlot.MAINHAND;
            case OFF_WEAPON -> EquipmentSlot.OFFHAND;
            default -> null;
        };
    }

    private static void updateWeaponAttributes(
            Player player,
            EquipmentSlot slot,
            ItemStack previous,
            ItemStack current
    ) {
        removeModifiers(player, slot, previous);
        if (player.getData(ModAttachments.PLAYER_COMBAT_MODE).active()) {
            addModifiers(player, slot, current);
        }
    }

    private static void refreshSlotAttributes(Player player, EquipmentSlot slot, ItemStack stack, boolean active) {
        removeModifiers(player, slot, stack);
        if (active) {
            addModifiers(player, slot, stack);
        }
    }

    private static void removeModifiers(Player player, EquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        stack.forEachModifier(slot, (attribute, modifier) -> {
            AttributeInstance instance = player.getAttributes().getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
            }
        });
    }

    private static void addModifiers(Player player, EquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        stack.forEachModifier(slot, (attribute, modifier) -> {
            AttributeInstance instance = player.getAttributes().getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
                instance.addTransientModifier(modifier);
            }
        });
    }

    private static int firstEmptyMainInventorySlot(Player player) {
        for (int slot = 9; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private PlayerEquipmentManager() {
    }
}
