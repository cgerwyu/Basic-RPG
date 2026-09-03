package net.cgerwyu.basicrpgclasses.mixin.client;

import net.cgerwyu.basicrpgclasses.client.CombatModeController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftHotbarMixin {
    @Redirect(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z")
    )
    private boolean basicrpgclasses$blockItemHotbarClicks(KeyMapping keyMapping) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (CombatModeController.active()) {
            for (KeyMapping hotbarKey : minecraft.options.keyHotbarSlots) {
                if (keyMapping == hotbarKey) {
                    while (keyMapping.consumeClick()) {
                        // Do not leave queued repeats that could select an item after combat mode ends.
                    }
                    keyMapping.setDown(false);
                    return false;
                }
            }
        }

        return keyMapping.consumeClick();
    }
}
