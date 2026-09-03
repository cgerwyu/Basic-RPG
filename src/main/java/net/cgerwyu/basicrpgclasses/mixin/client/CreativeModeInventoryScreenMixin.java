package net.cgerwyu.basicrpgclasses.mixin.client;

import net.cgerwyu.basicrpgclasses.client.ClientEquipmentScreenRenderer;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @ModifyArgs(
            method = "selectTab",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"
            )
    )
    private void basicrpgclasses$positionEquipmentSlots(Args args) {
        int index = args.get(1);
        switch (index) {
            case PlayerEquipmentManager.MAIN_WEAPON_MENU_SLOT -> {
                args.set(2, 126);
                args.set(3, 18);
            }
            case PlayerEquipmentManager.OFF_WEAPON_MENU_SLOT -> {
                args.set(2, 146);
                args.set(3, 18);
            }
            case PlayerEquipmentManager.NECKLACE_MENU_SLOT -> {
                args.set(2, 174);
                args.set(3, 18);
            }
            case PlayerEquipmentManager.RING_LEFT_MENU_SLOT -> {
                args.set(2, 174);
                args.set(3, 40);
            }
            case PlayerEquipmentManager.RING_RIGHT_MENU_SLOT -> {
                args.set(2, 174);
                args.set(3, 62);
            }
            case PlayerEquipmentManager.BELT_MENU_SLOT -> {
                args.set(2, 174);
                args.set(3, 84);
            }
            default -> {
            }
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void basicrpgclasses$renderEquipmentPanels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        ClientEquipmentScreenRenderer.renderBackground(
                (CreativeModeInventoryScreen) (Object) this,
                graphics
        );
    }
}
