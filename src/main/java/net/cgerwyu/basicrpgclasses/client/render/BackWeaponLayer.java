package net.cgerwyu.basicrpgclasses.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.cgerwyu.basicrpgclasses.equipment.RpgEquipmentSlotType;
import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.cgerwyu.basicrpgclasses.item.ProfiledWeapon;
import net.cgerwyu.basicrpgclasses.weapon.WeaponFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class BackWeaponLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public BackWeaponLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            AvatarRenderState state,
            float yRot,
            float xRot
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player) || PlayerEquipmentManager.combatModeActive(player)) {
            return;
        }

        ItemStack main = PlayerEquipmentManager.get(player, RpgEquipmentSlotType.MAIN_WEAPON);
        ItemStack off = PlayerEquipmentManager.get(player, RpgEquipmentSlotType.OFF_WEAPON);
        PlayerModel model = getParentModel();
        renderStowedItem(player, model, main, false, poseStack, submitNodeCollector, lightCoords, state.outlineColor);
        renderStowedItem(player, model, off, true, poseStack, submitNodeCollector, lightCoords, state.outlineColor);
    }

    private static void renderStowedItem(
            Player player,
            PlayerModel model,
            ItemStack stack,
            boolean offHand,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int outlineColor
    ) {
        if (stack.isEmpty() || !EquipmentRules.isWeapon(stack)) {
            return;
        }

        StowedTransform transform = transformFor(stack, offHand);
        ItemStackRenderState itemState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(
                itemState,
                stack,
                ItemDisplayContext.FIXED,
                player
        );
        poseStack.pushPose();
        transform.anchor().apply(model, poseStack);
        poseStack.translate(transform.x(), transform.y(), transform.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(transform.yRotation()));
        poseStack.mulPose(Axis.XP.rotationDegrees(transform.xRotation()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(transform.zRotation()));
        poseStack.scale(transform.scale(), transform.scale(), transform.scale());
        itemState.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    private static StowedTransform transformFor(ItemStack stack, boolean offHand) {
        WeaponFamily family = stack.getItem() instanceof ProfiledWeapon weapon
                ? weapon.profile().family()
                : null;
        if (family == null) {
            return offHand
                    ? new StowedTransform(StowedAnchor.BODY, -0.18F, 0.62F, 0.14F, 0.0F, 180.0F, 35.0F, 1.0F)
                    : new StowedTransform(StowedAnchor.BODY, 0.0F, 0.62F, 0.14F, 0.0F, 180.0F, 180.0F, 1.0F);
        }

        return switch (family) {
            // Back-mounted weapons sit directly against the body. Scale values
            // compensate for the model's FIXED transform so their final size is
            // the same as the corresponding third-person hand transform.
            case GREATSWORD -> new StowedTransform(StowedAnchor.BODY, -0.08F, 0.61F, 0.14F, 0.0F, 180.0F, 0.0F, 1.57F);
            case RAPIER -> new StowedTransform(StowedAnchor.BODY, -0.08F, 0.61F, 0.14F, 0.0F, 180.0F, 0.0F, 0.85F);
            case WARHAMMER -> new StowedTransform(StowedAnchor.BODY, 0.02F, 0.60F, 0.14F, 0.0F, 180.0F, 180.0F, 0.85F);

            // Staff heads stay above the shoulder; bows follow the same diagonal
            // instead of crossing through the torso.
            // The sprite itself is diagonal, so the stowed roll must compensate for
            // that baked-in angle. From behind, the head points up-right and the
            // shaft crosses the lower center of the back at roughly 14 degrees above the
            // ground, similar to Link's low, almost-horizontal quiver placement.
            case STAFF -> new StowedTransform(StowedAnchor.BODY, -0.12F, 0.34F, 0.14F, 0.0F, 180.0F, 203.0F, 1.35F);
            case SHORTBOW, RECURVE_BOW, LONGBOW ->
                    new StowedTransform(StowedAnchor.BODY, 0.08F, 0.40F, 0.14F, 0.0F, 180.0F, 0.0F, 0.85F);

            // Sheathed weapons remain on the outside of the left thigh, but are
            // anchored to the torso so they do not swing with the walking leg.
            // A 225-degree roll leaves the grip forward and the blade/head trailing
            // backward at roughly forty-five degrees.
            case ONE_HANDED_SWORD ->
                    new StowedTransform(StowedAnchor.BODY, 0.30F, 0.76F, 0.0F, 0.0F, 90.0F, 225.0F, 0.85F);
            case SCEPTER -> new StowedTransform(StowedAnchor.BODY, 0.30F, 0.76F, 0.0F, 0.0F, 90.0F, 225.0F, 0.65F);
            case KNIFE -> new StowedTransform(StowedAnchor.RIGHT_LEG, -0.18F, 0.05F, 0.0F, 0.0F, -90.0F, 0.0F, 0.85F);

            // Shields sit upright, higher, and on the exact center line.
            case SHIELD -> new StowedTransform(StowedAnchor.BODY, 0.0F, 0.45F, 0.14F, 0.0F, 180.0F, 180.0F, 1.0F);
        };
    }

    private enum StowedAnchor {
        BODY,
        LEFT_LEG,
        RIGHT_LEG;

        private void apply(PlayerModel model, PoseStack poseStack) {
            switch (this) {
                case BODY -> model.body.translateAndRotate(poseStack);
                case LEFT_LEG -> model.leftLeg.translateAndRotate(poseStack);
                case RIGHT_LEG -> model.rightLeg.translateAndRotate(poseStack);
            }
        }
    }

    private record StowedTransform(
            StowedAnchor anchor,
            float x,
            float y,
            float z,
            float xRotation,
            float yRotation,
            float zRotation,
            float scale
    ) {
    }
}
