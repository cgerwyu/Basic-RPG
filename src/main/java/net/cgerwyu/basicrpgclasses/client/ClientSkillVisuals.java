package net.cgerwyu.basicrpgclasses.client;

import com.mojang.math.Axis;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;

public final class ClientSkillVisuals {
    private static final Map<Integer, SpinAnimation> WHIRLWIND_SPINS = new HashMap<>();

    public static void startWhirlwind(int entityId, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long startTick = minecraft.level.getGameTime();
        WHIRLWIND_SPINS.put(entityId, new SpinAnimation(startTick, startTick + Math.max(1, durationTicks)));
    }

    public static void onRenderPlayerPre(RenderPlayerEvent.Pre<?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        int entityId = event.getRenderState().id;
        SpinAnimation spin = WHIRLWIND_SPINS.get(entityId);
        long gameTime = minecraft.level.getGameTime();
        if (spin != null && spin.endTick() > gameTime) {
            float progress = Math.clamp(
                    (gameTime + event.getPartialTick() - spin.startTick()) / (float) Math.max(1L, spin.endTick() - spin.startTick()),
                    0.0F,
                    1.0F
            );
            event.getPoseStack().pushPose();
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees(progress * -360.0F));
        } else if (spin != null) {
            WHIRLWIND_SPINS.remove(entityId);
        }

        if (minecraft.level.getEntity(entityId) instanceof LivingEntity entity
                && entity.hasEffect(MobEffects.INVISIBILITY)
                && entity.getData(ModAttachments.PLAYER_CLASS).rpgClass() == RpgClass.HUNTER) {
            event.getRenderState().rightHandItemState.clear();
            event.getRenderState().leftHandItemState.clear();
            event.getRenderState().rightHandItemStack = ItemStack.EMPTY;
            event.getRenderState().leftHandItemStack = ItemStack.EMPTY;
            event.getRenderState().rightArmPose = HumanoidModel.ArmPose.EMPTY;
            event.getRenderState().leftArmPose = HumanoidModel.ArmPose.EMPTY;
        }

        if (minecraft.level.getEntity(entityId) == minecraft.player
                && CombatModeController.active()
                && CombatModeController.castHeld()
                && PlayerEquipmentManager.hasMainWeapon(minecraft.player)) {
            RpgClass rpgClass = minecraft.player.getData(ModAttachments.PLAYER_CLASS).rpgClass();
            if (rpgClass == RpgClass.MAGE || rpgClass == RpgClass.PRIEST) {
                event.getRenderState().rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
        }
    }

    public static void onRenderPlayerPost(RenderPlayerEvent.Post<?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        SpinAnimation spin = WHIRLWIND_SPINS.get(event.getRenderState().id);
        if (spin != null && spin.endTick() > minecraft.level.getGameTime()) {
            event.getPoseStack().popPose();
        }
    }

    private record SpinAnimation(long startTick, long endTick) {
    }

    private ClientSkillVisuals() {
    }
}
