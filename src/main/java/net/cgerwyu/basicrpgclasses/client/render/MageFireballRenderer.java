package net.cgerwyu.basicrpgclasses.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.cgerwyu.basicrpgclasses.skill.entity.MageFireball;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class MageFireballRenderer extends ThrownItemRenderer<MageFireball> {
    private static final Identifier GLOW_TEXTURE = BasicRPGClasses.id("textures/vfx/soft_glow.png");
    private static final int FULL_BRIGHT = 0x00F000F0;

    public MageFireballRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0F, true);
    }

    @Override
    public ThrownItemRenderState createRenderState() {
        return new MageFireballRenderState();
    }

    @Override
    public void extractRenderState(MageFireball entity, ThrownItemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((MageFireballRenderState) state).skillScale = SkillScaling.fireballVisualScale(entity.skillRank());
        ((MageFireballRenderState) state).animationTime = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(
            ThrownItemRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        float scale = ((MageFireballRenderState) state).skillScale;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.popPose();

        MageFireballRenderState fireballState = (MageFireballRenderState) state;
        float pulse = 0.92F + 0.10F * (float) Math.sin(fireballState.animationTime * 0.85F);
        submitOrbLayer(poseStack, submitNodeCollector, scale * 1.85F * pulse, 0x6AFF5A18,
                fireballState.animationTime * 4.0F);
        submitOrbLayer(poseStack, submitNodeCollector, scale * 0.92F * pulse, 0xE8FFF2D0,
                -fireballState.animationTime * 6.0F);
    }

    private static void submitOrbLayer(PoseStack poseStack, SubmitNodeCollector collector,
                                       float size, int color, float rotation) {
        for (int plane = 0; plane < 3; plane++) {
            poseStack.pushPose();
            if (plane == 1) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            } else if (plane == 2) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation + plane * 37.0F));
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentEmissive(GLOW_TEXTURE),
                    (pose, vertices) -> emitQuad(pose, vertices, size, color)
            );
            poseStack.popPose();
        }
    }

    private static void emitQuad(PoseStack.Pose pose, VertexConsumer vertices, float size, int color) {
        float half = size * 0.5F;
        vertex(pose, vertices, -half, -half, 0.0F, 0.0F, 1.0F, color);
        vertex(pose, vertices, half, -half, 0.0F, 1.0F, 1.0F, color);
        vertex(pose, vertices, half, half, 0.0F, 1.0F, 0.0F, color);
        vertex(pose, vertices, -half, half, 0.0F, 0.0F, 0.0F, color);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float z,
                               float u, float v, int color) {
        vertices.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static final class MageFireballRenderState extends ThrownItemRenderState {
        private float skillScale = 1.0F;
        private float animationTime;
    }
}
