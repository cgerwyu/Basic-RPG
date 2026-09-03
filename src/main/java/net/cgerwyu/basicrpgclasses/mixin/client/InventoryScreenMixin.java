package net.cgerwyu.basicrpgclasses.mixin.client;

import net.cgerwyu.basicrpgclasses.client.ClientEquipmentScreenRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void basicrpgclasses$renderEquipmentPanels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        ClientEquipmentScreenRenderer.renderBackground((InventoryScreen) (Object) this, graphics);
    }
}
