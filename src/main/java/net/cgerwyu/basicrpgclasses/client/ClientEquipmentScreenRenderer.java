package net.cgerwyu.basicrpgclasses.client;

import net.cgerwyu.basicrpgclasses.equipment.EquipmentDescriptor;
import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentContainer;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.cgerwyu.basicrpgclasses.equipment.RpgEquipmentSlotType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ClientEquipmentScreenRenderer {
    private static final int SLOT_BACKGROUND = 0xFF2A2A31;
    private static final int SLOT_BORDER = 0xFF8B8B93;
    private static final int VALID_HIGHLIGHT = 0x8048D66B;
    private static final int INVALID_HIGHLIGHT = 0x80D94B4B;
    private static final int HOTBAR_INVALID = 0x70C92D2D;

    public static boolean hasEquipmentLayout(AbstractContainerScreen<?> screen) {
        AbstractContainerMenu menu = screen.getMenu();
        return menu.slots.size() > PlayerEquipmentManager.LAST_CUSTOM_MENU_SLOT
                && menu.getSlot(PlayerEquipmentManager.MAIN_WEAPON_MENU_SLOT).container
                instanceof PlayerEquipmentContainer;
    }

    public static void renderBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics) {
        if (!hasEquipmentLayout(screen)) {
            return;
        }

        int left = screen.getLeftPos();
        int top = screen.getTopPos();
        if (screen instanceof CreativeModeInventoryScreen) {
            graphics.fill(left + 121, top + 13, left + 169, top + 41, 0xD018181E);
            graphics.outline(left + 121, top + 13, 48, 28, 0xFF5F5F68);
            graphics.fill(left + 169, top + 7, left + 196, top + 105, 0xD018181E);
            graphics.outline(left + 169, top + 7, 27, 98, 0xFF5F5F68);
        } else {
            // The lower inventory is shifted down so the paper-doll can be
            // surrounded by armor, weapons and jewelry like an MMO character sheet.
            graphics.fill(left + 3, top + 82, left + 173, top + 184, 0xFFC6C6C6);
            graphics.outline(left + 3, top + 82, 170, 102, 0xFF55555D);
            for (int index = 9; index < 45; index++) {
                drawPlainSlotBase(screen, graphics, screen.getMenu().getSlot(index));
            }

            graphics.fill(left + 23, top + 75, left + 71, top + 103, 0xD018181E);
            graphics.outline(left + 23, top + 75, 48, 28, 0xFF5F5F68);
        }

        drawSlotBase(screen, graphics, RpgEquipmentSlotType.MAIN_WEAPON, "screen.basicrpgclasses.slot.main.short");
        drawSlotBase(screen, graphics, RpgEquipmentSlotType.OFF_WEAPON, "screen.basicrpgclasses.slot.off.short");
        drawSlotBase(screen, graphics, RpgEquipmentSlotType.NECKLACE, "screen.basicrpgclasses.slot.necklace.short");
        drawSlotBase(screen, graphics, RpgEquipmentSlotType.RING_LEFT, "screen.basicrpgclasses.slot.ring.short");
        drawSlotBase(screen, graphics, RpgEquipmentSlotType.RING_RIGHT, "screen.basicrpgclasses.slot.ring.short");
        drawSlotBase(screen, graphics, RpgEquipmentSlotType.BELT, "screen.basicrpgclasses.slot.belt.short");
    }

    public static void renderForeground(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics
    ) {
        if (!hasEquipmentLayout(screen)) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();

        ItemStack candidate = menu.getCarried();
        if (candidate.isEmpty() && screen.getHoveredSlot() != null) {
            candidate = screen.getHoveredSlot().getItem();
        }

        EquipmentDescriptor descriptor = EquipmentRules.describe(candidate);
        if (descriptor != null) {
            if (descriptor.kind() == EquipmentDescriptor.Kind.WEAPON) {
                for (int slotIndex = 36;
                     slotIndex < 45;
                     slotIndex++) {
                    highlight(screen, graphics, menu.getSlot(slotIndex), HOTBAR_INVALID);
                }
            }

            for (int target : targetSlots(menu, descriptor)) {
                Slot slot = menu.getSlot(target);
                boolean allowed = Minecraft.getInstance().player != null
                        && EquipmentRules.canUse(Minecraft.getInstance().player, candidate)
                        && slot.mayPlace(candidate);
                highlight(screen, graphics, slot, allowed ? VALID_HIGHLIGHT : INVALID_HIGHLIGHT);
            }
        }

        if (Minecraft.getInstance().player != null && EquipmentRules.isTwoHanded(
                PlayerEquipmentManager.get(Minecraft.getInstance().player, RpgEquipmentSlotType.MAIN_WEAPON)
        )) {
            Slot off = menu.getSlot(PlayerEquipmentManager.OFF_WEAPON_MENU_SLOT);
            int x = screen.getLeftPos() + off.x;
            int y = screen.getTopPos() + off.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x70380000);
            graphics.centeredText(Minecraft.getInstance().font, "×", x + 8, y + 3, 0xFFFF5555);
        }
    }

    private static void drawSlotBase(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            RpgEquipmentSlotType slotType,
            String shortLabelKey
    ) {
        Slot slot = screen.getMenu().getSlot(PlayerEquipmentManager.menuSlot(slotType));
        int x = screen.getLeftPos() + slot.x;
        int y = screen.getTopPos() + slot.y;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BACKGROUND);
        graphics.outline(x - 1, y - 1, 18, 18, SLOT_BORDER);
        if (!slot.hasItem()) {
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.translatable(shortLabelKey),
                    x + 8,
                    y + 4,
                    0xFF777780
            );
        }
    }

    private static void drawPlainSlotBase(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            Slot slot
    ) {
        int x = screen.getLeftPos() + slot.x;
        int y = screen.getTopPos() + slot.y;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.outline(x - 1, y - 1, 18, 18, 0xFF3E3E45);
    }

    private static List<Integer> targetSlots(AbstractContainerMenu menu, EquipmentDescriptor descriptor) {
        List<Integer> targets = new ArrayList<>();
        if (descriptor.kind() == EquipmentDescriptor.Kind.ARMOR) {
            targets.add(8 - descriptor.armorSlot().getIndex());
        } else if (descriptor.kind() == EquipmentDescriptor.Kind.WEAPON) {
            if (!descriptor.offHandOnly()) {
                targets.add(PlayerEquipmentManager.MAIN_WEAPON_MENU_SLOT);
            }
            if (descriptor.offHandOnly()
                    || descriptor.handedness() == net.cgerwyu.basicrpgclasses.weapon.Handedness.ONE_HANDED) {
                targets.add(PlayerEquipmentManager.OFF_WEAPON_MENU_SLOT);
            }
        } else {
            switch (descriptor.accessoryType()) {
                case NECKLACE -> targets.add(PlayerEquipmentManager.NECKLACE_MENU_SLOT);
                case RING -> {
                    targets.add(PlayerEquipmentManager.RING_LEFT_MENU_SLOT);
                    targets.add(PlayerEquipmentManager.RING_RIGHT_MENU_SLOT);
                }
                case BELT -> targets.add(PlayerEquipmentManager.BELT_MENU_SLOT);
            }
        }
        return targets;
    }

    private static void highlight(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            Slot slot,
            int color
    ) {
        int x = screen.getLeftPos() + slot.x;
        int y = screen.getTopPos() + slot.y;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, color);
    }

    private ClientEquipmentScreenRenderer() {
    }
}
