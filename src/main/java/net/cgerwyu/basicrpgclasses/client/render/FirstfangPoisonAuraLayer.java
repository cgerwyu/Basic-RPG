package net.cgerwyu.basicrpgclasses.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Animated poison inlays rendered on Mojang's normal humanoid armor geometry.
 * The layer is texture-only: it deliberately adds no Blockbench cubes or
 * protruding model parts.
 */
public final class FirstfangPoisonAuraLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final int FRAME_COUNT = 8;
    private static final Identifier[] OUTER_AURA = frames("outer");
    private static final Identifier[] LEGGINGS_AURA = frames("leggings");

    private final ArmorModelSet<HumanoidModel<AvatarRenderState>> armorModels;

    public FirstfangPoisonAuraLayer(
            RenderLayerParent<AvatarRenderState, PlayerModel> renderer,
            ArmorModelSet<HumanoidModel<AvatarRenderState>> armorModels
    ) {
        super(renderer);
        this.armorModels = armorModels;
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            AvatarRenderState state,
            float yRot,
            float xRot
    ) {
        if (state.isInvisible) {
            return;
        }

        int frame = Math.floorMod((int) (state.ageInTicks / 5.0F), FRAME_COUNT);
        renderPiece(state.chestEquipment, EquipmentSlot.CHEST, OUTER_AURA[frame], poseStack, collector, lightCoords, state);
        renderPiece(state.legsEquipment, EquipmentSlot.LEGS, LEGGINGS_AURA[frame], poseStack, collector, lightCoords, state);
        renderPiece(state.feetEquipment, EquipmentSlot.FEET, OUTER_AURA[frame], poseStack, collector, lightCoords, state);
    }

    private void renderPiece(
            ItemStack stack,
            EquipmentSlot slot,
            Identifier texture,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            AvatarRenderState state
    ) {
        if (!isFirstfangPiece(stack, slot)) {
            return;
        }

        collector.order(13).submitModel(
                armorModels.get(slot),
                state,
                poseStack,
                RenderTypes.entityTranslucentEmissive(texture, true),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                state.outlineColor,
                null
        );
    }

    private static boolean isFirstfangPiece(ItemStack stack, EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> stack.getItem() == ModItems.FIRSTFANG_CHESTPLATE.get();
            case LEGS -> stack.getItem() == ModItems.FIRSTFANG_LEGGINGS.get();
            case FEET -> stack.getItem() == ModItems.FIRSTFANG_BOOTS.get();
            default -> false;
        };
    }

    private static Identifier[] frames(String layer) {
        Identifier[] textures = new Identifier[FRAME_COUNT];
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            textures[frame] = BasicRPGClasses.id(
                    "textures/entity/armor/firstfang_aura_" + layer + "_" + frame + ".png"
            );
        }
        return textures;
    }
}
