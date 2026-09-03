package net.cgerwyu.basicrpgclasses.skill;

import net.cgerwyu.basicrpgclasses.network.payload.SkillVfxPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side entry point for custom skill visuals. It only broadcasts compact
 * animation descriptions; all fullbright geometry is generated on clients.
 */
public final class SkillParticleEffects {
    private static final double TRACKING_RANGE = 112.0;

    public static void skyRayCross(ServerLevel level, Vec3 impact) {
        world(level, SkillVfxType.SKY_CROSS, impact, impact.add(0.0, 52.0, 0.0),
                0xFFD35A, 1.0F, 12);
    }

    public static void lightningArc(ServerLevel level, Vec3 start, Vec3 end) {
        world(level, SkillVfxType.LIGHTNING_ARC, start, end, 0x58B8FF, 1.0F, 8);
    }

    public static void meteorFlightPath(ServerLevel level, Vec3 start, Vec3 target, int rank, int durationTicks) {
        float progress = (rank - 1.0F) / (SkillDefinitions.MAX_SKILL_RANK - 1.0F);
        float scale = 1.35F + 2.65F * progress;
        world(level, SkillVfxType.METEOR_FLIGHT, start, target, 0xFF4B14, scale, durationTicks);
    }

    public static void meteorImpact(ServerLevel level, Vec3 target, float radius) {
        world(level, SkillVfxType.METEOR_IMPACT, target.add(0.0, 0.25, 0.0), target,
                0xFF5A18, Math.max(1.0F, radius * 0.42F), 18);
    }

    public static void groundTremor(ServerLevel level, Vec3 origin, Vec3 forward, double range, double angleDegrees) {
        double halfWidth = Math.tan(Math.toRadians(angleDegrees)) * range;
        world(level, SkillVfxType.GROUND_CONE, origin.add(0.0, 0.08, 0.0),
                origin.add(forward.scale(range)).add(0.0, 0.08, 0.0),
                0xFFD36A, (float) Math.max(1.0, halfWidth), 16);
    }

    public static void groundSmoke(ServerLevel level, Vec3 origin, float radius) {
        level.sendParticles(ParticleTypes.POOF, origin.x, origin.y + 0.18, origin.z,
                56, radius * 0.48, 0.22, radius * 0.48, 0.055);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y + 0.12, origin.z,
                28, radius * 0.34, 0.14, radius * 0.34, 0.025);
    }

    public static void frostVapor(ServerLevel level, Vec3 center, float radius) {
        level.sendParticles(ParticleTypes.WHITE_SMOKE, center.x, center.y + 0.16, center.z,
                72, radius * 0.55, 0.30, radius * 0.55, 0.018);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.12, center.z,
                42, radius * 0.50, 0.22, radius * 0.50, 0.022);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.28, center.z,
                36, radius * 0.52, 0.32, radius * 0.52, 0.012);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.10, center.z,
                28, radius * 0.42, 0.16, radius * 0.42, 0.030);
    }

    public static void mageGlideVapor(ServerPlayer player, long gameTime) {
        Vec3 feet = player.position().add(0.0, 0.08, 0.0);
        double phase = gameTime * 0.38;
        for (int layer = 0; layer < 4; layer++) {
            double height = layer * 0.28;
            double radius = 0.08 + layer * 0.14;
            double angle = phase + layer * 1.72;
            double x = feet.x + Math.cos(angle) * radius;
            double z = feet.z + Math.sin(angle) * radius;
            player.level().sendParticles(ParticleTypes.WHITE_SMOKE, x, feet.y + height, z,
                    2, 0.035, 0.055, 0.035, 0.018);
            if (layer % 2 == 0) {
                player.level().sendParticles(ParticleTypes.SNOWFLAKE, x, feet.y + height, z,
                        1, 0.025, 0.04, 0.025, 0.008);
            }
        }
    }

    public static void fireballAura(ServerLevel level, Entity fireball, float scale, int durationTicks) {
        Vec3 motion = fireball.getDeltaMovement();
        Vec3 tail = motion.lengthSqr() < 1.0E-5
                ? new Vec3(0.0, 0.0, -1.8)
                : motion.normalize().scale(-2.8);
        attached(level, SkillVfxType.FIREBALL_AURA, fireball, tail, 0xFF3818, scale, durationTicks);
    }

    public static void burst(ServerLevel level, Vec3 position, int color, float scale, int durationTicks) {
        world(level, SkillVfxType.BURST, position, position, color, scale, durationTicks);
    }

    public static void areaRing(ServerLevel level, Vec3 position, int color, float radius, int durationTicks) {
        world(level, SkillVfxType.AREA_RING, position, position, color, radius, durationTicks);
    }

    public static void hunterTargetField(ServerLevel level, Vec3 position, float radius, int durationTicks) {
        world(level, SkillVfxType.HUNTER_TARGET_FIELD, position.add(0.0, 0.08, 0.0), position,
                0x46CFFF, radius, durationTicks);
    }

    public static void bloodDrain(ServerLevel level, Vec3 impact, Entity warrior, float scale) {
        Vec3 target = warrior.position().add(0.0, warrior.getBbHeight() * 0.55, 0.0);
        world(level, SkillVfxType.BLOOD_DRAIN, impact, target, 0xE3264F, scale, 11);
    }

    public static void piercingVolley(ServerLevel level, Vec3 start, Vec3 impact, float width) {
        piercingVolley(level, start, impact, 0x35CFFF, width);
    }

    public static void piercingVolley(ServerLevel level, Vec3 start, Vec3 impact, int color, float width) {
        world(level, SkillVfxType.PIERCING_VOLLEY, start, impact, color, width, 18);
    }

    public static void healingField(ServerLevel level, Vec3 position, float radius, int durationTicks) {
        world(level, SkillVfxType.HEALING_FIELD, position.add(0.0, 0.08, 0.0), position,
                0x45FF83, radius, durationTicks);
    }

    public static void holyField(ServerLevel level, Vec3 position, float radius, int durationTicks) {
        world(level, SkillVfxType.HEALING_FIELD, position.add(0.0, 0.08, 0.0), position,
                0xFFF2A6, radius, durationTicks);
    }

    public static void frostField(ServerLevel level, Vec3 position, float radius, int durationTicks) {
        world(level, SkillVfxType.FROST_FIELD, position.add(0.0, 0.08, 0.0), position,
                0x6FDCFF, radius, durationTicks);
    }

    public static void travelStreak(ServerLevel level, Vec3 start, Vec3 end, int color,
                                    float scale, int durationTicks) {
        world(level, SkillVfxType.TRAVEL_STREAK, start, end, color, scale, durationTicks);
    }

    public static void sonicDash(ServerLevel level, Vec3 start, Vec3 end, float scale) {
        world(level, SkillVfxType.SONIC_DASH, start.add(0.0, 0.9, 0.0), end.add(0.0, 0.9, 0.0),
                0xC1122A, scale, 14);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, start.x, start.y + 0.25, start.z,
                26, 0.45, 0.35, 0.45, 0.035);
        level.sendParticles(ParticleTypes.FLAME, start.x, start.y + 0.5, start.z,
                18, 0.34, 0.42, 0.34, 0.018);
    }

    public static void divineSlashWave(ServerLevel level, Vec3 start, Vec3 end, float scale) {
        world(level, SkillVfxType.DIVINE_SLASH_WAVE, start, end, 0xFFFFC640, scale, 16);
    }

    public static void warriorLeapImpact(ServerLevel level, Vec3 position, float scale) {
        world(level, SkillVfxType.WARRIOR_LEAP_IMPACT, position.add(0.0, 0.06, 0.0), position,
                0x9BE5FF, scale, 20);
        frostVapor(level, position, Math.max(1.15F, scale));
    }

    public static void bulwarkShields(ServerLevel level, ServerPlayer player, float scale, int durationTicks) {
        attached(level, SkillVfxType.BULWARK_SHIELDS, player, Vec3.ZERO,
                0xFFFFD36A, scale, durationTicks);
    }

    public static void priestBeam(ServerLevel level, Vec3 start, Vec3 end, float scale) {
        world(level, SkillVfxType.PRIEST_BEAM, start, end, 0xFFFFD96A, scale, 6);
    }

    public static void attached(ServerLevel level, SkillVfxType type, Entity entity, Vec3 direction,
                                int color, float scale, int durationTicks) {
        Vec3 start = entity.position();
        Vec3 end = start.add(direction);
        send(level, new SkillVfxPayload(
                type.id(), entity.getId(),
                start.x, start.y, start.z,
                end.x, end.y, end.z,
                color, scale, durationTicks
        ), start);
    }

    public static void projectileTrail(ServerLevel level, Entity projectile, int color, float scale,
                                       int durationTicks) {
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 direction = motion.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, -1.8) : motion.normalize().scale(-2.8);
        attached(level, SkillVfxType.PROJECTILE_TRAIL, projectile, direction, color, scale, durationTicks);
    }

    public static void world(ServerLevel level, SkillVfxType type, Vec3 start, Vec3 end,
                             int color, float scale, int durationTicks) {
        send(level, new SkillVfxPayload(
                type.id(), -1,
                start.x, start.y, start.z,
                end.x, end.y, end.z,
                color, scale, durationTicks
        ), start);
    }

    private static void send(ServerLevel level, SkillVfxPayload payload, Vec3 origin) {
        PacketDistributor.sendToPlayersNear(
                level, null, origin.x, origin.y, origin.z, TRACKING_RANGE, payload
        );
    }

    private SkillParticleEffects() {
    }
}
