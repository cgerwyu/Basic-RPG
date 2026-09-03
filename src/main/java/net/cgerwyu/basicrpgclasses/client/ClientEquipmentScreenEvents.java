package net.cgerwyu.basicrpgclasses.client;

import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.cgerwyu.basicrpgclasses.network.payload.EquipInventoryItemPayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientEquipmentScreenEvents {
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || (!(screen instanceof InventoryScreen)
                && !(screen instanceof CreativeModeInventoryScreen))
                || !ClientEquipmentScreenRenderer.hasEquipmentLayout(screen)
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        Slot hovered = screen.getHoveredSlot();
        int sourceSlot = hovered == null ? -1 : screen.getMenu().slots.indexOf(hovered);
        if (sourceSlot < InventoryMenu.INV_SLOT_START
                || sourceSlot >= InventoryMenu.USE_ROW_SLOT_END
                || EquipmentRules.describe(hovered.getItem()) == null) {
            return;
        }

        ClientPacketDistributor.sendToServer(new EquipInventoryItemPayload(sourceSlot));
        event.setCanceled(true);
    }

    public static void onRenderForeground(ScreenEvent.Render.Foreground event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen
                && (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) {
            ClientEquipmentScreenRenderer.renderForeground(screen, event.getGuiGraphics());
        }
    }

    private ClientEquipmentScreenEvents() {
    }
}
