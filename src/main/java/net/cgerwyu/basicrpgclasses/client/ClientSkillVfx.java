package net.cgerwyu.basicrpgclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.item.HunterBowItem;
import net.cgerwyu.basicrpgclasses.network.payload.SkillVfxPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkyRayVfxPayload;
import net.cgerwyu.basicrpgclasses.skill.SkillVfxType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Fullbright textured skill geometry. The bright core plus several larger,
 * translucent layers simulates bloom even without a shader pack.
 */
public final class ClientSkillVfx {
    private static final Identifier SOFT_GLOW = BasicRPGClasses.id("textures/vfx/soft_glow.png");
    private static final Identifier ENERGY_STREAK = BasicRPGClasses.id("textures/vfx/energy_streak.png");
    private static final Identifier MAGIC_RING = BasicRPGClasses.id("textures/vfx/magic_ring.png");
    private static final Identifier WHIRLWIND_RING = BasicRPGClasses.id("textures/vfx/whirlwind_ring.png");
    private static final Identifier GOLDEN_WINGS = BasicRPGClasses.id("textures/vfx/golden_wings.png");
    private static final Identifier DIVINE_SLASH = BasicRPGClasses.id("textures/vfx/divine_slash.png");
    private static final Identifier WARRIOR_LEAP_FROST = BasicRPGClasses.id("textures/vfx/warrior_leap_frost.png");
    private static final Identifier PALADIN_BULWARK_SHIELD = BasicRPGClasses.id("textures/vfx/paladin_bulwark_shield.png");
    private static final Identifier PRIEST_SOLAR_BEAM = BasicRPGClasses.id("textures/vfx/priest_solar_beam.png");
    private static final int FULL_BRIGHT = 0x00F000F0;

    private static final ContextKey<List<EffectRenderState>> EFFECTS = new ContextKey<>(
            Identifier.fromNamespaceAndPath(BasicRPGClasses.MODID, "skill_vfx")
    );
    private static final List<ActiveEffect> ACTIVE_EFFECTS = new ArrayList<>();
    private static ClientLevel activeLevel;
    private static boolean debugShowcaseShown;

    public static void addEffect(SkillVfxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        ensureLevel(level);
        long startTick = level.getGameTime();
        SkillVfxType type = SkillVfxType.byId(payload.effectId());
        if ((type == SkillVfxType.BOW_CHARGE && payload.entityId() >= 0)
                || type == SkillVfxType.TARGET_RING) {
            for (int index = 0; index < ACTIVE_EFFECTS.size(); index++) {
                ActiveEffect existing = ACTIVE_EFFECTS.get(index);
                if (existing.type == type && existing.entityId == payload.entityId()) {
                    ACTIVE_EFFECTS.set(index, new ActiveEffect(
                            type,
                            payload.entityId(),
                            new Vec3(payload.x(), payload.y(), payload.z()),
                            new Vec3(payload.endX(), payload.endY(), payload.endZ()),
                            payload.color(),
                            Math.max(0.05F, payload.scale()),
                            existing.startTick,
                            startTick + Math.max(1, payload.durationTicks())
                    ));
                    return;
                }
            }
        }
        ACTIVE_EFFECTS.add(new ActiveEffect(
                type,
                payload.entityId(),
                new Vec3(payload.x(), payload.y(), payload.z()),
                new Vec3(payload.endX(), payload.endY(), payload.endZ()),
                payload.color(),
                Math.max(0.05F, payload.scale()),
                startTick,
                startTick + Math.max(1, payload.durationTicks())
        ));
    }

    /** Compatibility path for the dedicated sky-ray packet. */
    public static void addSkyRay(SkyRayVfxPayload payload) {
        addEffect(new SkillVfxPayload(
                SkillVfxType.SKY_CROSS.id(), -1,
                payload.x(), payload.y(), payload.z(),
                payload.x(), payload.y() + 52.0, payload.z(),
                0x39FF64, 1.0F, payload.durationTicks()
        ));
    }

    public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        ClientLevel level = event.getLevel();
        ensureLevel(level);
        startDebugShowcaseIfRequested(level);
        long gameTime = level.getGameTime();
        ACTIVE_EFFECTS.removeIf(effect -> effect.endTick <= gameTime
                || effect.entityId >= 0
                && level.getEntity(effect.entityId) == null
                && gameTime > effect.startTick + 2L);
        if (ACTIVE_EFFECTS.isEmpty()) {
            return;
        }

        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float animationTime = gameTime + partialTick;
        List<EffectRenderState> renderStates = new ArrayList<>(ACTIVE_EFFECTS.size());
        for (ActiveEffect effect : ACTIVE_EFFECTS) {
            Vec3 start = effect.start;
            Vec3 end = effect.end;
            if (effect.entityId >= 0) {
                Entity entity = level.getEntity(effect.entityId);
                if (entity != null) {
                    Vec3 interpolatedPosition = entity.getPosition(partialTick);
                    Vec3 originalTravel = effect.end.subtract(effect.start);
                    if (effect.type == SkillVfxType.BOW_CHARGE) {
                        Vec3 look = entity.getViewVector(partialTick).normalize();
                        start = bowReticleOrigin(entity, look, interpolatedPosition, partialTick);
                        end = start.add(look);
                    } else if (effect.type == SkillVfxType.HUNTER_AFTERIMAGE) {
                        start = interpolatedPosition.add(0.0, entity.getBbHeight() * 0.5, 0.0);
                        Vec3 movement = entity.getDeltaMovement().multiply(1.0, 0.0, 1.0);
                        if (movement.lengthSqr() < 1.0E-4) {
                            continue;
                        }
                        end = start.add(movement.normalize());
                    } else if (effect.type == SkillVfxType.FIREBALL_AURA) {
                        start = interpolatedPosition.add(0.0, entity.getBbHeight() * 0.5, 0.0);
                        Vec3 movement = entity.getDeltaMovement();
                        end = movement.lengthSqr() < 1.0E-5
                                ? start.add(originalTravel)
                                : start.subtract(movement.normalize().scale(originalTravel.length()));
                    } else if (effect.type == SkillVfxType.HOLY_WINGS) {
                        Vec3 forward = entity.getViewVector(partialTick).multiply(1.0, 0.0, 1.0);
                        forward = forward.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
                        start = interpolatedPosition.add(0.0, entity.getBbHeight() * 0.66, 0.0).subtract(forward.scale(0.20));
                        end = start.add(forward);
                    } else if (effect.type == SkillVfxType.SHIELD || effect.type == SkillVfxType.BULWARK_SHIELDS) {
                        // Shields rise from the floor, rather than growing from the torso.
                        start = interpolatedPosition;
                        end = start.add(originalTravel);
                    } else if (effect.type == SkillVfxType.ARCANE_SHIELD) {
                        start = interpolatedPosition.add(0.0, entity.getBbHeight() * 0.50, 0.0);
                        end = start.add(originalTravel);
                    } else {
                        start = interpolatedPosition.add(0.0, entity.getBbHeight() * 0.5, 0.0);
                        end = start.add(originalTravel);
                    }
                }
            }
            float duration = Math.max(1.0F, effect.endTick - effect.startTick);
            float progress = Math.clamp((animationTime - effect.startTick) / duration, 0.0F, 1.0F);
            renderStates.add(new EffectRenderState(
                    effect.type, start, end, effect.color, effect.scale, progress, animationTime
            ));
        }
        event.getRenderState().setRenderData(EFFECTS, List.copyOf(renderStates));
    }

    private static Vec3 bowReticleOrigin(Entity entity, Vec3 look, Vec3 interpolatedPosition, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == entity && minecraft.options.getCameraType().isFirstPerson()) {
            return entity.getEyePosition(partialTick).add(look.scale(1.45));
        }
        if (!(entity instanceof LivingEntity living)) {
            return interpolatedPosition.add(0.0, entity.getBbHeight() * 0.68, 0.0).add(look.scale(0.35));
        }

        InteractionHand bowHand;
        if (living.isUsingItem()) {
            bowHand = living.getUsedItemHand();
        } else if (living.getMainHandItem().getItem() instanceof HunterBowItem) {
            bowHand = InteractionHand.MAIN_HAND;
        } else {
            bowHand = InteractionHand.OFF_HAND;
        }
        HumanoidArm arm = bowHand == InteractionHand.MAIN_HAND
                ? living.getMainArm()
                : living.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        Vec3 horizontalLook = look.multiply(1.0, 0.0, 1.0);
        horizontalLook = horizontalLook.lengthSqr() < 1.0E-5
                ? new Vec3(0.0, 0.0, 1.0)
                : horizontalLook.normalize();
        Vec3 right = new Vec3(-horizontalLook.z, 0.0, horizontalLook.x);
        double side = arm == HumanoidArm.RIGHT ? 0.43 : -0.43;
        return interpolatedPosition
                .add(0.0, entity.getBbHeight() * 0.76, 0.0)
                .add(right.scale(side))
                .add(look.scale(0.58));
    }

    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        List<EffectRenderState> effects = event.getLevelRenderState().getRenderData(EFFECTS);
        if (effects == null || effects.isEmpty()) {
            return;
        }

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        for (EffectRenderState effect : effects) {
            poseStack.pushPose();
            Vec3 cameraOffset = effect.start.subtract(camera);
            poseStack.translate(cameraOffset.x, cameraOffset.y, cameraOffset.z);
            renderEffect(event, poseStack, effect);
            poseStack.popPose();
        }
    }

    private static void renderEffect(SubmitCustomGeometryEvent event, PoseStack poseStack, EffectRenderState effect) {
        float pulse = 0.94F + 0.08F * (float) Math.sin(effect.animationTime * 0.55F);
        float fade = endFade(effect.progress);
        Vec3 travel = effect.end.subtract(effect.start);
        switch (effect.type) {
            case BURST -> renderBurst(event, poseStack, effect.color, effect.scale * pulse, effect.progress, fade);
            case AREA_RING -> renderAreaRing(event, poseStack, effect.color, effect.scale, effect.progress, fade, effect.animationTime);
            case TARGET_RING -> renderTargetRing(event, poseStack, effect.color, effect.scale, fade);
            case SHIELD -> renderShield(event, poseStack, effect.color, effect.scale * pulse, fade, effect.animationTime);
            case ARCANE_SHIELD -> renderArcaneShield(event, poseStack, effect.scale * pulse, fade, effect.animationTime);
            case TRAVEL_STREAK, PROJECTILE_TRAIL -> renderTravelStreak(event, poseStack, travel, effect.color, effect.scale, fade);
            case SLASH_ORBIT -> renderSlashOrbit(event, poseStack, effect.color, effect.scale, fade, effect.animationTime);
            case LIGHTNING_ARC -> renderLightning(event, poseStack, travel, effect.color, effect.scale, fade, effect.animationTime);
            case METEOR_FLIGHT -> renderMeteorFlight(event, poseStack, travel, effect.color, effect.scale, effect.progress, fade);
            case METEOR_IMPACT -> renderMeteorImpact(event, poseStack, effect.color, effect.scale, effect.progress, fade);
            case GROUND_CONE -> renderGroundCone(event, poseStack, travel, effect.color, effect.scale, effect.progress, fade);
            case HEALING_FIELD -> renderHealingField(event, poseStack, effect.color, effect.scale, effect.progress, fade, effect.animationTime);
            case WIND_TRAIL -> renderWindTrail(event, poseStack, travel, effect.color, effect.scale, fade, effect.animationTime);
            case FROST_FIELD -> renderFrostField(event, poseStack, effect.color, effect.scale, effect.progress, fade, effect.animationTime);
            case SKY_CROSS -> renderSkyCross(event, poseStack, effect.start, effect.color, effect.scale, fade, effect.animationTime);
            case BOW_CHARGE -> renderBowCharge(event, poseStack, travel, effect.color, effect.scale,
                    effect.progress, fade, effect.animationTime);
            case TAUNT_ARROWS -> renderTauntArrows(event, poseStack, effect.color, effect.scale,
                    effect.progress, fade, effect.animationTime);
            case FORTIFY_SHIELDS -> renderFortifyShields(event, poseStack, effect.color, effect.scale,
                    fade, effect.animationTime);
            case KINETIC_BURST -> renderKineticBurst(event, poseStack, travel, effect.color, effect.scale,
                    effect.progress, fade, effect.animationTime);
            case WARRIOR_AURA -> renderWarriorAura(event, poseStack, effect.color, effect.scale,
                    fade, effect.animationTime);
            case BLOOD_DRAIN -> renderBloodDrain(event, poseStack, travel, effect.color, effect.scale,
                    effect.progress, fade, effect.animationTime);
            case HUNTER_TARGET_FIELD -> renderHunterTargetField(event, poseStack, effect.color, effect.scale,
                    effect.progress, fade, effect.animationTime);
            case HUNTER_AFTERIMAGE -> renderHunterAfterimage(event, poseStack, travel, effect.color,
                    effect.scale, fade, effect.animationTime);
            case HUNTER_CLOAK -> renderHunterCloak(event, poseStack, effect.color, effect.scale,
                    effect.progress, fade, effect.animationTime);
            case PIERCING_VOLLEY -> renderPiercingVolley(event, poseStack, travel, effect.color,
                    effect.scale, effect.progress, fade, effect.animationTime);
            case FIREBALL_AURA -> renderFireballAura(event, poseStack, travel, effect.color,
                    effect.scale, fade, effect.animationTime);
            case HOLY_WINGS -> renderHolyWings(event, poseStack, travel, effect.color, effect.scale,
                    fade, effect.animationTime);
            case SONIC_DASH -> renderSonicDash(event, poseStack, travel, effect.color, effect.scale,
                    effect.progress, fade, effect.animationTime);
            case DIVINE_SLASH_WAVE -> renderDivineSlashWave(event, poseStack, travel, effect.scale,
                    effect.progress, fade);
            case WARRIOR_LEAP_IMPACT -> renderWarriorLeapImpact(event, poseStack, effect.scale, fade);
            case BULWARK_SHIELDS -> renderBulwarkShields(event, poseStack, effect.scale, fade);
            case PRIEST_BEAM -> renderPriestBeam(event, poseStack, travel, effect.scale, fade);
        }
    }

    private static void renderBurst(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                    float scale, float progress, float fade) {
        float size = scale * (0.45F + easeOut(progress) * 1.1F);
        drawGlowCross(event, poseStack, size * 1.55F, withAlpha(color, (int) (85 * fade)));
        drawGlowCross(event, poseStack, size, withAlpha(0xFFFFFF, (int) (220 * fade)));
        drawHorizontal(event, poseStack, MAGIC_RING, size * 1.35F,
                withAlpha(color, (int) (170 * fade)), progress * 100.0F);
    }

    private static void renderSonicDash(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                        int color, float scale, float progress, float fade, float time) {
        if (travel.lengthSqr() < 1.0E-5) {
            return;
        }
        Vec3 direction = travel.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
        for (int strand = 0; strand < 9; strand++) {
            double offset = (strand - 4) * scale * 0.17;
            double waviness = Math.sin(time * 0.35 + strand * 1.8) * scale * 0.10;
            Vec3 from = side.scale(offset).add(0.0, (strand % 3 - 1) * 0.18, 0.0);
            Vec3 to = travel.scale(0.88).add(side.scale(offset + waviness)).add(0.0, (strand % 2) * 0.22, 0.0);
            drawSegment(event, poseStack, from, to, ENERGY_STREAK, scale * (0.055F + (strand % 2) * 0.025F),
                    withAlpha(strand % 3 == 0 ? 0x2B000A : color, (int) (175 * fade * (1.0 - progress * 0.35))));
        }
        drawGlowCross(event, poseStack, scale * 0.85F, withAlpha(0x6E0716, (int) (90 * fade)));
    }

    private static void renderDivineSlashWave(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                              float scale, float progress, float fade) {
        if (travel.lengthSqr() < 1.0E-5) {
            return;
        }
        Vec3 direction = travel.normalize();
        Vec3 position = travel.scale(easeOut(progress));
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(1.0F, 0.0F, 0.0F),
                new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)));
        submitPlane(event, poseStack, DIVINE_SLASH, scale * 4.0F, scale * 1.45F,
                withAlpha(0xFFFFFF, (int) (235 * fade)));
        poseStack.popPose();
    }

    private static void renderWarriorLeapImpact(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                                float scale, float fade) {
        drawHorizontal(event, poseStack, WARRIOR_LEAP_FROST, scale * 1.48F,
                withAlpha(0xBDEFFF, (int) (225 * fade)), 0.0F);
    }

    private static void renderBulwarkShields(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                              float scale, float fade) {
        float radius = scale * 0.90F;
        for (int panel = 0; panel < 6; panel++) {
            float angle = panel * 60.0F;
            double radians = Math.toRadians(angle);
            poseStack.pushPose();
            poseStack.translate(Math.cos(radians) * radius, scale * 0.78F, Math.sin(radians) * radius);
            poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
            submitPlane(event, poseStack, PALADIN_BULWARK_SHIELD, scale * 0.82F, scale * 1.12F,
                    withAlpha(0xFFF4BF, (int) (185 * fade)));
            poseStack.popPose();
        }
    }

    private static void renderPriestBeam(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                         float scale, float fade) {
        double length = travel.length();
        if (length < 1.0E-5) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(travel.x * 0.5, travel.y * 0.5, travel.z * 0.5);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F),
                new Vector3f((float) travel.x, (float) travel.y, (float) travel.z).normalize()));
        submitPlane(event, poseStack, PRIEST_SOLAR_BEAM, scale * 1.15F, (float) length,
                withAlpha(0xFFFFFF, (int) (225 * fade)));
        poseStack.popPose();
    }

    private static void renderAreaRing(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                       float scale, float progress, float fade, float time) {
        float size = scale * (0.72F + 0.28F * easeOut(progress));
        drawHorizontal(event, poseStack, MAGIC_RING, size,
                withAlpha(color, (int) (205 * fade)), time * 1.8F);
        drawHorizontal(event, poseStack, SOFT_GLOW, size * 0.92F,
                withAlpha(color, (int) (54 * fade)), -time * 0.7F);
    }

    private static void renderShield(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                     float scale, float fade, float time) {
        int rgb = color & 0xFFFFFF;
        if ((rgb & 0x0000FF) > (rgb >> 16 & 0xFF) * 1.25F) {
            renderArcaneShield(event, poseStack, scale, fade, time);
            return;
        }
        if ((rgb >> 16 & 0xFF) > 180 && (rgb >> 8 & 0xFF) > 130) {
            renderHolyShieldDome(event, poseStack, color, scale, fade, time);
            return;
        }
        float radius = scale * 0.82F;
        float height = scale * 1.75F;
        drawHorizontal(event, poseStack, MAGIC_RING, radius,
                withAlpha(color, (int) (205 * fade)), time * 1.5F);
        poseStack.pushPose();
        poseStack.translate(0.0, height, 0.0);
        drawHorizontal(event, poseStack, MAGIC_RING, radius,
                withAlpha(color, (int) (90 * fade)), -time * 1.1F);
        poseStack.popPose();
        for (int index = 0; index < 14; index++) {
            double angle = Math.PI * 2.0 * index / 14.0;
            Vec3 bottom = new Vec3(Math.cos(angle) * radius, 0.04, Math.sin(angle) * radius);
            Vec3 top = bottom.add(0.0, height, 0.0);
            drawSegment(event, poseStack, bottom, top, ENERGY_STREAK, scale * 0.12F,
                    withAlpha(color, (int) (42 * fade)));
        }
    }

    private static void renderArcaneShield(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                           float scale, float fade, float time) {
        float radius = scale * 0.92F;
        for (int latitude = 0; latitude < 7; latitude++) {
            float y = -radius + latitude * radius * 0.33F;
            float ringRadius = (float) Math.sqrt(Math.max(0.08, radius * radius - y * y));
            drawRingXZ(event, poseStack, ringRadius, 36, time * (latitude % 2 == 0 ? 0.015F : -0.012F),
                    0x63BFFF, (int) (55 * fade), 0.045F);
            for (int orb = 0; orb < 2; orb++) {
                double angle = time * 0.08 + latitude * 1.37 + orb * Math.PI;
                poseStack.pushPose();
                poseStack.translate(Math.cos(angle) * ringRadius, y, Math.sin(angle) * ringRadius);
                drawGlowCross(event, poseStack, scale * 0.18F, withAlpha(0x9CDFFF, (int) (120 * fade)));
                poseStack.popPose();
            }
        }
        drawGlowCross(event, poseStack, radius * 1.55F, withAlpha(0x3E96FF, (int) (30 * fade)));
    }

    private static void renderHolyShieldDome(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                              float scale, float fade, float time) {
        float radius = scale * 0.90F;
        float height = scale * 1.62F;
        for (int layer = 0; layer <= 7; layer++) {
            float fraction = layer / 7.0F;
            float ringRadius = radius * (float) Math.sqrt(Math.max(0.0F, 1.0F - fraction * fraction));
            poseStack.pushPose();
            poseStack.translate(0.0, fraction * height, 0.0);
            drawRingXZ(event, poseStack, Math.max(0.025F, ringRadius), 40, 0.0F,
                    color, (int) ((layer == 0 ? 190 : 90) * fade), layer == 0 ? 0.07F : 0.035F);
            poseStack.popPose();
        }
        for (int meridian = 0; meridian < 12; meridian++) {
            double angle = meridian * Math.PI * 2.0 / 12.0;
            Vec3 previous = new Vec3(Math.cos(angle) * radius, 0.04, Math.sin(angle) * radius);
            for (int layer = 1; layer <= 7; layer++) {
                float fraction = layer / 7.0F;
                float ringRadius = radius * (float) Math.sqrt(Math.max(0.0F, 1.0F - fraction * fraction));
                Vec3 next = new Vec3(Math.cos(angle) * ringRadius, fraction * height, Math.sin(angle) * ringRadius);
                drawSegment(event, poseStack, previous, next, ENERGY_STREAK, scale * 0.027F,
                        withAlpha(color, (int) (58 * fade)));
                previous = next;
            }
        }
        for (int ray = 0; ray < 12; ray++) {
            double angle = ray * Math.PI * 2.0 / 12.0;
            Vec3 base = new Vec3(Math.cos(angle) * radius, 0.04, Math.sin(angle) * radius);
            Vec3 outward = base.add(new Vec3(Math.cos(angle) * 0.36, 0.12, Math.sin(angle) * 0.36));
            drawSegment(event, poseStack, base, outward, ENERGY_STREAK,
                    scale * 0.035F, withAlpha(0xFFE69A, (int) (150 * fade)));
        }
        drawGlowCross(event, poseStack, radius * 1.35F, withAlpha(color, (int) (45 * fade)));
    }

    private static void renderTravelStreak(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                           int color, float scale, float fade) {
        if (travel.lengthSqr() < 1.0E-5) {
            renderBurst(event, poseStack, color, scale, 0.7F, fade);
            return;
        }
        drawSegment(event, poseStack, Vec3.ZERO, travel, ENERGY_STREAK, scale * 0.72F,
                withAlpha(color, (int) (110 * fade)));
        drawSegment(event, poseStack, Vec3.ZERO, travel, ENERGY_STREAK, scale * 0.30F,
                withAlpha(0xFFFFFF, (int) (235 * fade)));
        poseStack.pushPose();
        poseStack.translate(travel.x, travel.y, travel.z);
        drawGlowCross(event, poseStack, scale * 1.1F, withAlpha(color, (int) (95 * fade)));
        poseStack.popPose();
    }

    private static void renderFireballAura(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                           int color, float scale, float fade, float time) {
        if (travel.lengthSqr() > 1.0E-5) {
            drawSegment(event, poseStack, travel, Vec3.ZERO, ENERGY_STREAK, scale * 0.72F,
                    withAlpha(0xFF3A16, (int) (145 * fade)));
            drawSegment(event, poseStack, travel.scale(0.72), Vec3.ZERO, ENERGY_STREAK, scale * 0.27F,
                    withAlpha(0xFFD18A, (int) (235 * fade)));
        }
        float pulse = 0.92F + 0.11F * (float) Math.sin(time * 0.82F);
        drawGlowCross(event, poseStack, scale * 1.85F * pulse,
                withAlpha(color, (int) (135 * fade)));
        drawGlowCross(event, poseStack, scale * 0.82F * pulse,
                withAlpha(0xFFF0C2, (int) (235 * fade)));
    }

    private static void renderSlashOrbit(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                         float scale, float fade, float time) {
        drawHorizontal(event, poseStack, WHIRLWIND_RING, scale,
                withAlpha(0xFFFFFF, (int) (245 * fade)), -time * 62.0F);
    }

    private static void renderLightning(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                        int color, float scale, float fade, float time) {
        if (travel.lengthSqr() < 1.0E-5) {
            return;
        }
        Vec3 direction = travel.normalize();
        Vec3 side = direction.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-5 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = direction.cross(side).normalize();
        Vec3 previous = Vec3.ZERO;
        int segments = Math.clamp((int) Math.ceil(travel.length() * 1.2), 7, 18);
        for (int index = 1; index <= segments; index++) {
            double amount = index / (double) segments;
            double envelope = Math.sin(amount * Math.PI);
            double jitterA = Math.sin(index * 9.71 + time * 1.93) * 0.24 * envelope * scale;
            double jitterB = Math.cos(index * 6.37 - time * 2.41) * 0.19 * envelope * scale;
            Vec3 point = travel.scale(amount).add(side.scale(jitterA)).add(up.scale(jitterB));
            drawSegment(event, poseStack, previous, point, ENERGY_STREAK, 0.34F * scale,
                    withAlpha(color, (int) (135 * fade)));
            drawSegment(event, poseStack, previous, point, ENERGY_STREAK, 0.13F * scale,
                    withAlpha(0xFFFFFF, (int) (245 * fade)));
            previous = point;
        }
    }

    private static void renderMeteorFlight(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                           int color, float scale, float progress, float fade) {
        Vec3 current = travel.scale(easeIn(progress));
        Vec3 direction = travel.lengthSqr() < 1.0E-5 ? new Vec3(0.0, -1.0, 0.0) : travel.normalize();
        Vec3 tail = current.subtract(direction.scale(5.0 * scale));
        drawSegment(event, poseStack, tail, current, ENERGY_STREAK, scale * 1.55F,
                withAlpha(color, (int) (120 * fade)));
        drawSegment(event, poseStack, tail, current, ENERGY_STREAK, scale * 0.52F,
                withAlpha(0xFFF4D0, (int) (245 * fade)));
        poseStack.pushPose();
        poseStack.translate(current.x, current.y, current.z);
        drawGlowCross(event, poseStack, scale * 2.25F, withAlpha(color, (int) (115 * fade)));
        drawGlowCross(event, poseStack, scale * 1.05F, withAlpha(0xFFFFFF, (int) (245 * fade)));
        poseStack.popPose();
    }

    private static void renderMeteorImpact(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                           float scale, float progress, float fade) {
        float expansion = scale * (0.25F + easeOut(progress));
        drawHorizontal(event, poseStack, MAGIC_RING, expansion * 2.3F,
                withAlpha(color, (int) (210 * fade)), progress * 130.0F);
        drawGlowCross(event, poseStack, expansion * 2.8F, withAlpha(color, (int) (120 * fade)));
        drawGlowCross(event, poseStack, expansion * 1.2F, withAlpha(0xFFFFFF, (int) (235 * fade)));
        for (int index = 0; index < 6; index++) {
            double angle = index * Math.PI / 3.0;
            Vec3 end = new Vec3(Math.cos(angle) * expansion * 1.8, 0.45 + expansion * 0.25,
                    Math.sin(angle) * expansion * 1.8);
            drawSegment(event, poseStack, new Vec3(0.0, 0.2, 0.0), end, ENERGY_STREAK, scale * 0.35F,
                    withAlpha(color, (int) (150 * fade)));
        }
    }

    private static void renderGroundCone(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                         int color, float scale, float progress, float fade) {
        Vec3 flat = new Vec3(travel.x, 0.04, travel.z);
        if (flat.lengthSqr() < 1.0E-5) {
            return;
        }
        float maximumRadius = (float) flat.length();
        for (int wave = 0; wave < 3; wave++) {
            float waveProgress = Math.clamp(progress * 1.45F - wave * 0.18F, 0.0F, 1.0F);
            if (waveProgress <= 0.0F) {
                continue;
            }
            float radius = maximumRadius * easeOut(waveProgress);
            int alpha = (int) (205 * fade * (1.0F - waveProgress * 0.58F));
            drawContinuousRingXZ(event, poseStack, radius, 48, progress * 0.42F + wave * 0.17F,
                    wave == 1 ? 0x8C1724 : color, alpha, 0.13F + wave * 0.025F);
        }
    }

    private static void renderHealingField(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                           float scale, float progress, float fade, float time) {
        drawHorizontal(event, poseStack, MAGIC_RING, scale,
                withAlpha(color, (int) (210 * fade)), time * 1.8F);
        drawHorizontal(event, poseStack, SOFT_GLOW, scale * 0.82F,
                withAlpha(color, (int) (58 * fade)), 0.0F);
        for (int layer = 1; layer <= 3; layer++) {
            poseStack.pushPose();
            poseStack.translate(0.0, layer * 0.8 + Math.sin(time * 0.18 + layer) * 0.12, 0.0);
            drawHorizontal(event, poseStack, MAGIC_RING, scale * (1.0F - layer * 0.13F),
                    withAlpha(color, (int) ((120 - layer * 20) * fade)), -time * 2.0F + layer * 25.0F);
            poseStack.popPose();
        }
        drawGlowCross(event, poseStack, scale * (0.7F + progress * 0.2F),
                withAlpha(0xE8FFF0, (int) (80 * fade)));
        if ((color & 0x00F00000) != 0) {
            // Golden holy field: a translucent dome with sun-like rays.
            for (int ray = 0; ray < 12; ray++) {
                double angle = ray * Math.PI * 2.0 / 12.0;
                Vec3 tip = new Vec3(Math.cos(angle) * scale * 0.95, 0.18, Math.sin(angle) * scale * 0.95);
                drawSegment(event, poseStack, new Vec3(0.0, scale * 1.35, 0.0), tip,
                        ENERGY_STREAK, scale * 0.035F, withAlpha(0xFFE49A, (int) (105 * fade)));
            }
            drawGlowCross(event, poseStack, scale * 1.15F, withAlpha(0xFFD15A, (int) (45 * fade)));
        }
    }

    private static void renderWindTrail(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                        int color, float scale, float fade, float time) {
        Vec3 direction = travel.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : travel.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
        for (int lane = -1; lane <= 1; lane++) {
            double wave = Math.sin(time * 0.45 + lane * 2.1) * 0.18;
            Vec3 start = direction.scale(-2.7 * scale).add(side.scale(lane * 0.25 + wave));
            Vec3 end = direction.scale(0.25).add(side.scale(lane * 0.16));
            drawSegment(event, poseStack, start, end, ENERGY_STREAK, scale * 0.23F,
                    withAlpha(color, (int) ((135 - Math.abs(lane) * 25) * fade)));
        }
    }

    private static void renderFrostField(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                         float scale, float progress, float fade, float time) {
        float size = scale * (0.35F + 0.65F * easeOut(progress));
        drawHorizontal(event, poseStack, MAGIC_RING, size,
                withAlpha(color, (int) (220 * fade)), -time * 2.2F);
        drawHorizontal(event, poseStack, SOFT_GLOW, size * 0.95F,
                withAlpha(color, (int) (75 * fade)), time);
        for (int index = 0; index < 6; index++) {
            double angle = Math.PI * 2.0 * index / 6.0;
            Vec3 outer = new Vec3(Math.cos(angle) * size * 0.5, 0.15, Math.sin(angle) * size * 0.5);
            drawSegment(event, poseStack, Vec3.ZERO, outer, ENERGY_STREAK, 0.14F,
                    withAlpha(0xE6FBFF, (int) (180 * fade)));
        }
    }

    private static void renderSkyCross(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 origin,
                                       int color, float scale, float fade, float time) {
        double portalHeight = 13.0 + Math.abs(Math.sin(origin.x * 0.71 + origin.z * 0.37)) * 8.0;
        Vec3 portal = new Vec3(0.0, portalHeight, 0.0);
        Vec3 ground = new Vec3(0.0, 0.08, 0.0);
        drawSegment(event, poseStack, portal, ground, ENERGY_STREAK,
                0.95F * scale, withAlpha(color, (int) (105 * fade)));
        drawSegment(event, poseStack, portal, ground, ENERGY_STREAK,
                0.28F * scale, withAlpha(0xF3FFF5, (int) (240 * fade)));
        poseStack.pushPose();
        poseStack.translate(0.0, portalHeight, 0.0);
        drawHorizontal(event, poseStack, MAGIC_RING, 2.25F * scale,
                withAlpha(color, (int) (215 * fade)), time * 2.2F);
        drawHorizontal(event, poseStack, SOFT_GLOW, 1.8F * scale,
                withAlpha(color, (int) (64 * fade)), -time * 0.9F);
        drawGlowCross(event, poseStack, 1.6F * scale, withAlpha(0xFFFFFF, (int) (150 * fade)));
        poseStack.popPose();
        drawSegment(event, poseStack, ground.add(-2.2 * scale, 0.0, 0.0),
                ground.add(2.2 * scale, 0.0, 0.0), ENERGY_STREAK, 0.42F * scale,
                withAlpha(color, (int) (170 * fade)));
        drawSegment(event, poseStack, ground.add(0.0, 0.0, -2.2 * scale),
                ground.add(0.0, 0.0, 2.2 * scale), ENERGY_STREAK, 0.42F * scale,
                withAlpha(color, (int) (170 * fade)));
    }

    private static void renderHolyWings(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel, int color,
                                        float scale, float fade, float time) {
        Vec3 direction = travel.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : travel.normalize();
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationTo(
                new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f((float) -direction.x, 0.0F, (float) -direction.z)
        ));
        float flap = 0.98F + 0.02F * (float) Math.sin(time * 0.30F);
        submitPlane(event, poseStack, GOLDEN_WINGS, scale * 2.25F * flap, scale * 1.52F * flap,
                withAlpha(color, (int) (235 * fade)));
        poseStack.popPose();
    }

    /** A calm, hollow placement ring: no fill, no rotation and no flickering pulse. */
    private static void renderTargetRing(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                         float radius, float fade) {
        drawRingXZ(event, poseStack, radius, 48, 0.0F, color, (int) (225 * fade), 0.075F);
        drawRingXZ(event, poseStack, Math.max(0.2F, radius - 0.13F), 48, 0.0F,
                color, (int) (120 * fade), 0.028F);
    }

    private static void renderBowCharge(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                        int color, float scale, float progress, float fade, float time) {
        poseStack.pushPose();
        if (travel.lengthSqr() > 1.0E-5) {
            Vector3f target = new Vector3f((float) travel.x, (float) travel.y, (float) travel.z).normalize();
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), target));
        }
        float size = scale * (0.72F + progress * 0.28F);
        drawPlanarReticle(event, poseStack, size * 0.5F, color, fade, 0.0F);
        submitPlane(event, poseStack, SOFT_GLOW, size * 0.62F, size * 0.62F,
                withAlpha(color, (int) (58 * fade)));
        poseStack.popPose();
    }

    private static void renderTauntArrows(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                          float scale, float progress, float fade, float time) {
        float contraction = 0.45F + 0.55F * (1.0F - easeOut(progress));
        for (int index = 0; index < 8; index++) {
            double angle = index * Math.PI / 4.0 + time * 0.025;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x);
            double y = Math.sin(time * 0.18 + index * 1.7) * 0.22;
            Vec3 tip = radial.scale(scale * contraction).add(0.0, y, 0.0);
            Vec3 tail = radial.scale(scale * (contraction + 0.42F)).add(0.0, y, 0.0);
            int arrowColor = withAlpha(color, (int) (220 * fade));
            drawSegment(event, poseStack, tail, tip, ENERGY_STREAK, 0.13F, arrowColor);
            Vec3 headBase = tip.add(radial.scale(0.42));
            drawSegment(event, poseStack, headBase.add(tangent.scale(0.22)), tip,
                    ENERGY_STREAK, 0.15F, arrowColor);
            drawSegment(event, poseStack, headBase.subtract(tangent.scale(0.22)), tip,
                    ENERGY_STREAK, 0.15F, arrowColor);
        }
        drawGlowCross(event, poseStack, 1.05F, withAlpha(color, (int) (56 * fade)));
    }

    private static void renderFortifyShields(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                             float scale, float fade, float time) {
        for (int index = 0; index < 4; index++) {
            double angle = index * Math.PI / 2.0 + time * 0.065;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x);
            Vec3 center = radial.scale(1.12 * scale).add(0.0,
                    Math.sin(time * 0.16 + index * 1.4) * 0.12, 0.0);
            drawShieldGlyph(event, poseStack, center, tangent, scale * 0.58F,
                    withAlpha(color, (int) (220 * fade)));
        }
    }

    private static void drawShieldGlyph(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 center,
                                        Vec3 horizontal, float size, int color) {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3[] points = {
                center.add(horizontal.scale(-0.58 * size)).add(up.scale(0.38 * size)),
                center.add(up.scale(0.62 * size)),
                center.add(horizontal.scale(0.58 * size)).add(up.scale(0.38 * size)),
                center.add(horizontal.scale(0.48 * size)).add(up.scale(-0.18 * size)),
                center.add(up.scale(-0.70 * size)),
                center.add(horizontal.scale(-0.48 * size)).add(up.scale(-0.18 * size))
        };
        submitShieldFill(event, poseStack, points,
                withAlpha(color, (color >>> 24) * 42 / 100));
        for (int index = 0; index < points.length; index++) {
            drawSegment(event, poseStack, points[index], points[(index + 1) % points.length],
                    ENERGY_STREAK, 0.12F * size, color);
            poseStack.pushPose();
            poseStack.translate(points[index].x, points[index].y, points[index].z);
            drawGlowCross(event, poseStack, size * 0.22F,
                    withAlpha(color, Math.min(255, (color >>> 24) + 22)));
            poseStack.popPose();
        }
    }

    private static void submitShieldFill(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                         Vec3[] points, int color) {
        Vec3 center = Vec3.ZERO;
        for (Vec3 point : points) {
            center = center.add(point);
        }
        Vec3 polygonCenter = center.scale(1.0 / points.length);
        event.getSubmitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(SOFT_GLOW),
                (pose, vertices) -> {
                    for (int index = 0; index < points.length; index++) {
                        Vec3 first = points[index];
                        Vec3 second = points[(index + 1) % points.length];
                        shieldFillVertex(pose, vertices, polygonCenter, color, 1.0F);
                        shieldFillVertex(pose, vertices, first, color, 1.0F);
                        shieldFillVertex(pose, vertices, second, color, 1.0F);
                        shieldFillVertex(pose, vertices, polygonCenter, color, 1.0F);
                        shieldFillVertex(pose, vertices, polygonCenter, color, -1.0F);
                        shieldFillVertex(pose, vertices, second, color, -1.0F);
                        shieldFillVertex(pose, vertices, first, color, -1.0F);
                        shieldFillVertex(pose, vertices, polygonCenter, color, -1.0F);
                    }
                }
        );
    }

    private static void shieldFillVertex(PoseStack.Pose pose, VertexConsumer vertices, Vec3 point,
                                         int color, float normalZ) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(color)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, normalZ);
    }

    private static void renderKineticBurst(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                           int color, float scale, float progress, float fade, float time) {
        float reach = scale * (0.18F + easeOut(progress) * 1.55F);
        for (int index = 0; index < 14; index++) {
            double angle = index * 2.399963229728653 + time * 0.018;
            double y = -0.65 + 1.3 * ((index * 5 % 14) / 13.0);
            double horizontal = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            Vec3 direction = new Vec3(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal);
            Vec3 inner = direction.scale(reach * 0.28);
            Vec3 outer = direction.scale(reach * (0.72 + (index % 3) * 0.14));
            drawSegment(event, poseStack, inner, outer, ENERGY_STREAK,
                    scale * (index % 2 == 0 ? 0.16F : 0.10F),
                    withAlpha(index % 3 == 0 ? 0xFFFFFF : color, (int) (210 * fade)));
        }
        drawGlowCross(event, poseStack, reach * 0.95F, withAlpha(color, (int) (92 * fade)));
        drawGlowCross(event, poseStack, reach * 0.38F, withAlpha(0xFFFFFF, (int) (225 * fade)));
    }

    private static void renderWarriorAura(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                          float scale, float fade, float time) {
        // Low, non-orbiting heat haze: only from the feet to the waist.
        for (int cloud = 0; cloud < 10; cloud++) {
            double phase = (time * 0.08 + cloud * 0.173) % 1.0;
            double angle = cloud * 2.399963229728653;
            double radius = scale * (0.10 + (cloud % 3) * 0.10);
            poseStack.pushPose();
            poseStack.translate(Math.cos(angle) * radius, scale * (-0.92 + phase * 0.95), Math.sin(angle) * radius);
            drawGlowCross(event, poseStack, scale * (0.26F + (cloud % 2) * 0.07F),
                    withAlpha(cloud % 3 == 0 ? 0x3A0008 : color,
                            (int) ((36 + cloud % 2 * 20) * fade * (1.0 - phase * 0.45))));
            poseStack.popPose();
        }
    }

    private static void renderBloodDrain(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                         int color, float scale, float progress, float fade, float time) {
        drawGlowCross(event, poseStack, scale * 0.85F * (1.0F - progress * 0.45F),
                withAlpha(color, (int) (120 * fade)));
        if (travel.lengthSqr() < 1.0E-5) {
            return;
        }
        Vec3 direction = travel.normalize();
        Vec3 side = direction.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-5 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = direction.cross(side).normalize();
        for (int index = 0; index < 4; index++) {
            float amount = Math.clamp(progress * 1.35F - index * 0.13F, 0.0F, 1.0F);
            double wave = Math.sin(amount * Math.PI * 3.0 + index * 1.9 + time * 0.08) * scale * 0.28;
            Vec3 point = travel.scale(amount)
                    .add(side.scale(wave))
                    .add(up.scale(Math.cos(amount * Math.PI * 2.0 + index) * scale * 0.16));
            Vec3 tail = point.subtract(direction.scale(scale * 0.55));
            drawSegment(event, poseStack, tail, point, ENERGY_STREAK, scale * 0.18F,
                    withAlpha(index == 0 ? 0xFF7A91 : color, (int) (205 * fade)));
            poseStack.pushPose();
            poseStack.translate(point.x, point.y, point.z);
            drawGlowCross(event, poseStack, scale * 0.38F, withAlpha(color, (int) (105 * fade)));
            poseStack.popPose();
        }
    }

    private static void renderHunterTargetField(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                                float scale, float progress, float fade, float time) {
        float radius = scale * (0.45F + 0.55F * easeOut(progress));
        drawRingXZ(event, poseStack, radius, 40, time * 0.012F, color, (int) (215 * fade), 0.11F);
        drawRingXZ(event, poseStack, radius * 0.62F, 32, -time * 0.018F,
                0xD8F8FF, (int) (170 * fade), 0.075F);
        for (int index = 0; index < 4; index++) {
            double angle = index * Math.PI / 2.0;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            drawSegment(event, poseStack, direction.scale(radius * 0.22).add(0.0, 0.04, 0.0),
                    direction.scale(radius * 0.92).add(0.0, 0.04, 0.0),
                    ENERGY_STREAK, 0.10F, withAlpha(color, (int) (190 * fade)));
        }
        drawHorizontal(event, poseStack, SOFT_GLOW, radius * 0.74F,
                withAlpha(color, (int) (42 * fade)), 0.0F);
    }

    private static void renderHunterAfterimage(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                               int color, float scale, float fade, float time) {
        Vec3 direction = travel.multiply(1.0, 0.0, 1.0);
        direction = direction.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
        for (int index = 0; index < 28; index++) {
            double row = index % 7;
            double lineLife = (time * 0.13 + index * 0.173) % 1.0;
            double depth = 0.35 + (index / 7) * 0.48 + row * 0.12 + lineLife * 0.72;
            double sideOffset = ((index * 5 % 11) / 10.0 - 0.5) * 1.35 * scale;
            double y = -0.78 + ((index * 3 % 13) / 12.0) * 1.55;
            double shimmer = Math.sin(time * 0.31 + index * 2.17) * 0.09;
            Vec3 start = direction.scale(-depth).add(side.scale(sideOffset + shimmer)).add(0.0, y, 0.0);
            double length = scale * (0.18 + (index % 5) * 0.045);
            Vec3 end = start.subtract(direction.scale(length));
            int alpha = (int) ((70 + (index % 4) * 22) * fade * (1.0 - lineLife));
            drawSegment(event, poseStack, start, end, ENERGY_STREAK, scale * 0.055F,
                    withAlpha(index % 4 == 0 ? 0xD5FAFF : color, alpha));
        }
        poseStack.pushPose();
        poseStack.translate(0.0, -0.88, 0.0);
        drawHorizontal(event, poseStack, SOFT_GLOW, scale * 0.95F,
                withAlpha(0x42BFFF, (int) (105 * fade)), time * 0.8F);
        drawRingXZ(event, poseStack, scale * 0.72F, 24, time * 0.035F,
                0x8CEBFF, (int) (185 * fade), 0.075F);
        poseStack.popPose();
    }

    private static void renderHunterCloak(SubmitCustomGeometryEvent event, PoseStack poseStack, int color,
                                          float scale, float progress, float fade, float time) {
        for (int index = 0; index < 12; index++) {
            double angle = index * Math.PI / 6.0 + time * 0.045;
            double radius = scale * (0.36 + 0.16 * Math.sin(index * 2.1 + time * 0.13));
            double lift = -0.8 + ((progress * 2.2 + index * 0.17) % 1.0) * 1.8;
            Vec3 start = new Vec3(Math.cos(angle) * radius, lift - 0.32, Math.sin(angle) * radius);
            Vec3 end = new Vec3(Math.cos(angle + 0.25) * radius * 0.72, lift + 0.36,
                    Math.sin(angle + 0.25) * radius * 0.72);
            drawSegment(event, poseStack, start, end, ENERGY_STREAK, scale * 0.12F,
                    withAlpha(index % 2 == 0 ? color : 0xA4F5FF, (int) (125 * fade)));
        }
        drawGlowCross(event, poseStack, scale * 1.05F, withAlpha(color, (int) (38 * fade)));
    }

    private static void renderPiercingVolley(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 travel,
                                             int color, float scale, float progress, float fade, float time) {
        if (travel.lengthSqr() < 1.0E-5) {
            return;
        }
        Vec3 direction = travel.normalize();
        Vec3 side = direction.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-5 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = direction.cross(side).normalize();
        drawSegment(event, poseStack, Vec3.ZERO, travel, ENERGY_STREAK, scale * 0.95F,
                withAlpha(color, (int) (105 * fade)));
        drawSegment(event, poseStack, Vec3.ZERO, travel, ENERGY_STREAK, scale * 0.34F,
                withAlpha(0xF1FCFF, (int) (245 * fade)));
        int segments = Math.clamp((int) Math.ceil(travel.length() * 0.8), 12, 32);
        for (int strand = 0; strand < 4; strand++) {
            Vec3 previous = Vec3.ZERO;
            for (int index = 1; index <= segments; index++) {
                double amount = index / (double) segments;
                double envelope = Math.sin(amount * Math.PI);
                double angle = amount * Math.PI * 5.0 + strand * Math.PI / 2.0;
                double radius = scale * 0.48 * envelope;
                Vec3 point = travel.scale(amount)
                        .add(side.scale(Math.cos(angle) * radius))
                        .add(up.scale(Math.sin(angle) * radius));
                drawSegment(event, poseStack, previous, point, ENERGY_STREAK, scale * 0.13F,
                        withAlpha(strand % 2 == 0 ? color : 0xA6F1FF, (int) (155 * fade)));
                previous = point;
            }
        }
        poseStack.pushPose();
        poseStack.translate(travel.x, travel.y, travel.z);
        Vector3f target = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), target));
        float impactSize = scale * 0.72F;
        submitPlane(event, poseStack, SOFT_GLOW, impactSize * 2.15F, impactSize * 2.15F,
                withAlpha(color, (int) (88 * fade)));
        poseStack.popPose();
    }

    private static void drawPlanarReticle(SubmitCustomGeometryEvent event, PoseStack poseStack, float radius,
                                          int color, float fade, float time) {
        drawRingXY(event, poseStack, radius, 32, time * 0.026F, color, (int) (225 * fade), 0.075F);
        drawRingXY(event, poseStack, radius * 0.64F, 24, -time * 0.039F,
                0xDDF9FF, (int) (175 * fade), 0.055F);
        for (int index = 0; index < 4; index++) {
            double angle = index * Math.PI / 2.0;
            Vec3 direction = new Vec3(Math.cos(angle), Math.sin(angle), 0.0);
            drawSegment(event, poseStack, direction.scale(radius * 0.24), direction.scale(radius * 0.92),
                    ENERGY_STREAK, radius * 0.075F, withAlpha(color, (int) (205 * fade)));
            Vec3 tangent = new Vec3(-direction.y, direction.x, 0.0);
            Vec3 notch = direction.scale(radius * 1.08);
            drawSegment(event, poseStack, notch.subtract(tangent.scale(radius * 0.11)),
                    notch.add(tangent.scale(radius * 0.11)), ENERGY_STREAK, radius * 0.065F,
                    withAlpha(0xFFFFFF, (int) (220 * fade)));
        }
    }

    private static void drawRingXY(SubmitCustomGeometryEvent event, PoseStack poseStack, float radius,
                                   int segments, float rotation, int color, int alpha, float thickness) {
        for (int index = 0; index < segments; index++) {
            if (index % (segments / 4) == 0) {
                continue;
            }
            double first = rotation + index * Math.PI * 2.0 / segments;
            double second = rotation + (index + 1) * Math.PI * 2.0 / segments;
            drawSegment(event, poseStack,
                    new Vec3(Math.cos(first) * radius, Math.sin(first) * radius, 0.0),
                    new Vec3(Math.cos(second) * radius, Math.sin(second) * radius, 0.0),
                    ENERGY_STREAK, thickness, withAlpha(color, alpha));
        }
    }

    private static void drawRingXZ(SubmitCustomGeometryEvent event, PoseStack poseStack, float radius,
                                   int segments, float rotation, int color, int alpha, float thickness) {
        for (int index = 0; index < segments; index++) {
            if (index % (segments / 4) == 0) {
                continue;
            }
            double first = rotation + index * Math.PI * 2.0 / segments;
            double second = rotation + (index + 1) * Math.PI * 2.0 / segments;
            drawSegment(event, poseStack,
                    new Vec3(Math.cos(first) * radius, 0.045, Math.sin(first) * radius),
                    new Vec3(Math.cos(second) * radius, 0.045, Math.sin(second) * radius),
                    ENERGY_STREAK, thickness, withAlpha(color, alpha));
        }
    }

    private static void drawContinuousRingXZ(SubmitCustomGeometryEvent event, PoseStack poseStack, float radius,
                                             int segments, float rotation, int color, int alpha, float thickness) {
        for (int index = 0; index < segments; index++) {
            double first = rotation + index * Math.PI * 2.0 / segments;
            double second = rotation + (index + 1) * Math.PI * 2.0 / segments;
            drawSegment(event, poseStack,
                    new Vec3(Math.cos(first) * radius, 0.055, Math.sin(first) * radius),
                    new Vec3(Math.cos(second) * radius, 0.055, Math.sin(second) * radius),
                    ENERGY_STREAK, thickness, withAlpha(color, alpha));
        }
    }

    private static void drawGlowCross(SubmitCustomGeometryEvent event, PoseStack poseStack, float size, int color) {
        submitPlane(event, poseStack, SOFT_GLOW, size, size, color);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        submitPlane(event, poseStack, SOFT_GLOW, size, size, color);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        submitPlane(event, poseStack, SOFT_GLOW, size, size, color);
        poseStack.popPose();
    }

    private static void drawHorizontal(SubmitCustomGeometryEvent event, PoseStack poseStack, Identifier texture,
                                       float size, int color, float rotationDegrees) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.035, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
        submitPlane(event, poseStack, texture, size * 2.0F, size * 2.0F, color);
        poseStack.popPose();
    }

    private static void drawSegment(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 from, Vec3 to,
                                    Identifier texture, float thickness, int color) {
        Vec3 difference = to.subtract(from);
        double length = difference.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 midpoint = from.add(to).scale(0.5);
        Vector3f target = new Vector3f((float) difference.x, (float) difference.y,
                (float) difference.z).normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(1.0F, 0.0F, 0.0F), target);

        poseStack.pushPose();
        poseStack.translate(midpoint.x, midpoint.y, midpoint.z);
        poseStack.mulPose(rotation);
        submitPlane(event, poseStack, texture, (float) length, thickness, color);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        submitPlane(event, poseStack, texture, (float) length, thickness, color);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void submitPlane(SubmitCustomGeometryEvent event, PoseStack poseStack, Identifier texture,
                                    float width, float height, int color) {
        event.getSubmitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(texture),
                (pose, vertices) -> emitDoubleSidedQuad(pose, vertices, width, height, color)
        );
    }

    private static void emitDoubleSidedQuad(PoseStack.Pose pose, VertexConsumer vertices,
                                            float width, float height, int color) {
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        vertex(pose, vertices, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, color, 1.0F);
        vertex(pose, vertices, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, color, 1.0F);
        vertex(pose, vertices, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, color, 1.0F);
        vertex(pose, vertices, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, color, 1.0F);
        vertex(pose, vertices, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, color, -1.0F);
        vertex(pose, vertices, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, color, -1.0F);
        vertex(pose, vertices, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, color, -1.0F);
        vertex(pose, vertices, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, color, -1.0F);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float z,
                               float u, float v, int color, float normalZ) {
        vertices.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, normalZ);
    }

    private static float easeOut(float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeIn(float progress) {
        return progress * progress;
    }

    private static float endFade(float progress) {
        if (progress <= 0.72F) {
            return Math.clamp(progress / 0.08F, 0.0F, 1.0F);
        }
        return Math.clamp((1.0F - progress) / 0.28F, 0.0F, 1.0F);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0xFFFFFF);
    }

    private static void ensureLevel(ClientLevel level) {
        if (activeLevel != level) {
            ACTIVE_EFFECTS.clear();
            debugShowcaseShown = false;
            activeLevel = level;
        }
    }

    /** Dev-only visual smoke test: add -Dbasicrpgclasses.testVfx=true to the client JVM. */
    private static void startDebugShowcaseIfRequested(ClientLevel level) {
        Entity player = Minecraft.getInstance().player;
        if (debugShowcaseShown || player == null || !player.isAlive()
                || !Boolean.getBoolean("basicrpgclasses.testVfx")) {
            return;
        }
        debugShowcaseShown = true;
        Vec3 center = player.position();
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        forward = forward.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        addDebugEffect(SkillVfxType.SHIELD, player.getId(), center, center, 0x5EBBFF, 1.7F, 1200);
        addDebugEffect(SkillVfxType.SLASH_ORBIT, player.getId(), center, center, 0xFFD76A, 2.5F, 1200);
        addDebugEffect(SkillVfxType.HEALING_FIELD, -1, center.add(right.scale(4.0)), center,
                0x45FF83, 3.0F, 1200);
        addDebugEffect(SkillVfxType.FROST_FIELD, -1, center.add(right.scale(-4.0)), center,
                0x6FDCFF, 3.0F, 1200);
        addDebugEffect(SkillVfxType.LIGHTNING_ARC, -1, center.add(0.0, 1.2, 0.0),
                center.add(forward.scale(9.0)).add(0.0, 1.2, 0.0), 0x55CCFF, 1.0F, 1200);
        addDebugEffect(SkillVfxType.SKY_CROSS, -1, center.add(forward.scale(11.0)), center,
                0x39FF64, 1.0F, 1200);
    }

    private static void addDebugEffect(SkillVfxType type, int entityId, Vec3 start, Vec3 end,
                                       int color, float scale, int duration) {
        addEffect(new SkillVfxPayload(
                type.id(), entityId,
                start.x, start.y, start.z,
                end.x, end.y, end.z,
                color, scale, duration
        ));
    }

    private record ActiveEffect(SkillVfxType type, int entityId, Vec3 start, Vec3 end,
                                int color, float scale, long startTick, long endTick) {
    }

    private record EffectRenderState(SkillVfxType type, Vec3 start, Vec3 end,
                                     int color, float scale, float progress, float animationTime) {
    }

    private ClientSkillVfx() {
    }
}
