package net.cgerwyu.basicrpgclasses.skill;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.cgerwyu.basicrpgclasses.skill.entity.MageFireball;
import net.cgerwyu.basicrpgclasses.combat.PvpBalance;

import java.util.List;
import java.util.UUID;

public final class SkillExecutor {
    public static boolean execute(ServerPlayer player, SkillDefinition definition, int rank) {
        return switch (definition.id()) {
            case WHIRLWIND -> whirlwind(player, rank);
            case FORTIFY -> fortify(player, rank);
            case PROVOKE -> provoke(player, rank);
            case GROUND_STUN -> groundStun(player, rank);
            case SHIELD_BASH -> shieldBash(player, rank);
            case BATTLE_CRY -> battleCry(player, rank);
            case BERSERK -> berserk(player, rank);
            case EXECUTION -> execution(player, rank);
            case ULTRA_THRUST -> ultraThrust(player, rank);
            case WARRIOR_LEAP -> warriorLeap(player, rank);
            case WARRIOR_WHIRLWIND -> whirlwind(player, rank);
            case FIREBALL -> fireball(player, rank);
            case HEAL -> heal(player, rank);
            case HOLY_BOLT -> holyBolt(player, rank);
            case SOLAR_BEAM -> solarBeam(player, rank);
            case BLINK -> blink(player, rank);
            case MAGIC_SHIELD -> magicShield(player, rank);
            case FROST_NOVA -> frostNova(player, rank);
            case METEOR -> meteor(player, rank);
            case SKY_RAYS -> skyRays(player, rank);
            case CHAIN_LIGHTNING -> chainLightning(player, rank);
            case RESTORATION -> restoration(player, rank);
            case HEALING_HALO -> healingHalo(player, rank);
            case RESURRECTION -> resurrection(player, rank);
            case BLESSING -> blessing(player, rank);
            case HOLY_SHIELD -> holyShield(player, rank);
            case CLEANSE -> cleanse(player, rank);
            case HOLY_STORM -> holyStorm(player, rank);
            case PALADIN_HEAL -> paladinHeal(player, rank);
            case PALADIN_BLESSING -> paladinBlessing(player, rank);
            case DIVINE_BULWARK -> divineBulwark(player, rank);
            case DIVINE_SLASH -> divineSlash(player);
            case DASH -> dash(player, rank);
            case WINDRUN -> windrun(player, rank);
            case CAMOUFLAGE -> camouflage(player, rank);
            case MULTISHOT -> multishot(player, rank);
            case ARROW_RAIN -> arrowRain(player, rank);
            case POWER_SHOT -> powerShot(player, rank);
            case FROST_ARROWS -> frostArrows(player);
            case WARRIOR_VITALITY, MAGE_VITALITY, HUNTER_VITALITY, MAGE_GLIDE,
                 MAGE_MANA_REGEN, HUNTER_FALL_TRAINING, HUNTER_CLIMBING,
                 HUNTER_MANA_REGEN, HUNTER_DRAW_SPEED, HUNTER_SHOT_POWER,
                 WARRIOR_VAMPIRISM, PRIEST_VITALITY, PRIEST_MANA_REGEN,
                 PALADIN_VITALITY, PALADIN_ARMOR_TRAINING, PALADIN_MANA_STRIKE -> false;
            case NONE -> false;
        };
    }

    private static boolean whirlwind(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(SkillScaling.whirlwindRadius(rank)),
                target -> target != player && target.isAlive() && isHostileTarget(player, target)
        );
        float damage = SkillScaling.whirlwindDamage(rank);
        for (LivingEntity target : targets) {
            target.hurtServer(level, player.damageSources().playerAttack(player), damage);
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        SkillRuntimeEffects.startWhirlwind(player);
        SkillParticleEffects.attached(level, SkillVfxType.SLASH_ORBIT, player, Vec3.ZERO,
                0xFFFFFF, (float) SkillScaling.whirlwindRadius(rank), 12);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.35F, 0.72F);
        return true;
    }

    private static boolean fortify(ServerPlayer player, int rank) {
        int duration = SkillScaling.fortifyDurationTicks(rank);
        double radius = SkillScaling.fortifyRadius(rank);
        List<ServerPlayer> protectedPlayers = player.level().getEntitiesOfClass(
                ServerPlayer.class,
                player.getBoundingBox().inflate(radius),
                target -> target.isAlive()
                        && PvpBalance.friendlyPlayer(player, target)
                        && target.position().distanceToSqr(player.position()) <= radius * radius
        );
        protectedPlayers.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        int protectedCount = 0;
        for (ServerPlayer target : protectedPlayers) {
            if (protectedCount++ >= 6) {
                break;
            }
            target.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    duration,
                    SkillScaling.fortifyResistanceAmplifier(rank),
                    false,
                    true,
                    true
            ), player);
            target.addEffect(new MobEffectInstance(
                    MobEffects.ABSORPTION,
                    duration,
                    SkillScaling.fortifyAbsorptionAmplifier(rank),
                    false,
                    true,
                    true
            ), player);
            SkillParticleEffects.attached(player.level(), SkillVfxType.FORTIFY_SHIELDS, target, Vec3.ZERO,
                    0xFFD84A, 1.0F, duration);
        }
        return true;
    }

    private static boolean provoke(ServerPlayer player, int rank) {
        double radius = SkillScaling.provokeRadius(rank);
        int duration = SkillScaling.provokeDurationTicks(rank);
        List<Monster> targets = player.level().getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(radius),
                Entity::isAlive
        );
        for (Monster target : targets) {
            target.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true), player);
            if (rank >= 8) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true), player);
            }
        }
        List<ServerPlayer> challengedPlayers = player.level().getEntitiesOfClass(
                ServerPlayer.class,
                player.getBoundingBox().inflate(radius),
                target -> target != player && target.isAlive() && PvpBalance.canDamagePlayer(player, target)
                        && target.position().distanceToSqr(player.position()) <= radius * radius
        );
        challengedPlayers.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        int pvpDuration = Math.min(duration, 80);
        for (ServerPlayer target : challengedPlayers.stream().limit(6).toList()) {
            SkillRuntimeEffects.startChallenge(target, player, pvpDuration);
            SkillRuntimeEffects.breakCamouflage(target);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, pvpDuration, 0, false, false, true), player);
        }
        SkillParticleEffects.attached(player.level(), SkillVfxType.TAUNT_ARROWS, player, Vec3.ZERO,
                0xEF334E, (float) Math.min(3.6, 2.4 + radius * 0.08), 22);
        return true;
    }

    private static boolean fireball(ServerPlayer player, int rank) {
        int projectileCount = SkillScaling.fireballVolleyCount(rank);
        Vec3 look = player.getLookAngle().normalize();
        SkillParticleEffects.areaRing(player.level(), player.position().add(0.0, 0.06, 0.0),
                0xFF5A24, 1.25F + rank * 0.025F, 14);
        spawnFireball(player, rank, look);
        SkillRuntimeEffects.scheduleFireballVolley(player, rank, projectileCount - 1, look);
        return true;
    }

    static void spawnFireball(ServerPlayer player, int rank, Vec3 direction) {
        ServerLevel level = player.level();
        MageFireball fireball = new MageFireball(level, player, rank, direction);
        level.addFreshEntity(fireball);
        SkillParticleEffects.fireballAura(level, fireball,
                0.82F + rank * 0.025F, 12);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static boolean heal(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        float amount = SkillScaling.healAmount(rank);
        LivingEntity target = aimedFriendlyTarget(player, 20.0);
        if (target == null) {
            target = player;
        }
        target.heal(amount * PvpBalance.healingMultiplier(player));
        SkillParticleEffects.attached(level, SkillVfxType.BURST, target, Vec3.ZERO,
                0xFFF4B0, Math.max(0.7F, target.getBbWidth()), 18);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.65F);
        return true;
    }

    private static boolean holyBolt(ServerPlayer player, int rank) {
        LivingEntity target = aimedHostileTarget(player, 28.0);
        if (target == null) {
            return false;
        }
        ServerLevel level = player.level();
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.55));
        Vec3 impact = target.getEyePosition();
        target.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.holyBoltDamage(rank));
        SkillParticleEffects.piercingVolley(level, start, impact, 0xFFFFD04A, 0.55F);
        SkillParticleEffects.world(level, SkillVfxType.KINETIC_BURST, impact, start, 0xFFFFE6A0, 0.78F, 12);
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, 1.7F);
        return true;
    }

    private static boolean solarBeam(ServerPlayer player, int rank) {
        SkillRuntimeEffects.startSolarBeam(player, rank);
        return true;
    }

    static boolean solarBeamPulse(ServerPlayer player, int rank) {
        LivingEntity target = aimedLivingTarget(player, 24.0);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.55));
        Vec3 end = target == null ? start.add(player.getLookAngle().scale(24.0)) : target.getEyePosition();
        if (target != null) {
            if (isFriendlyHealingTarget(player, target)) {
                target.heal(SkillScaling.solarBeamHeal(rank) * PvpBalance.healingMultiplier(player));
            } else if (isHostileTarget(player, target)) {
                target.hurtServer(player.level(), player.damageSources().playerAttack(player), SkillScaling.solarBeamDamage(rank));
            }
        }
        SkillParticleEffects.priestBeam(player.level(), start, end, 0.42F);
        return true;
    }

    private static boolean isFriendlyHealingTarget(ServerPlayer caster, LivingEntity target) {
        return target.isAlive() && (target instanceof ServerPlayer playerTarget
                ? PvpBalance.friendlyPlayer(caster, playerTarget)
                : target instanceof Mob && !(target instanceof Enemy));
    }

    private static double horizontalDistanceSqr(Entity first, Entity second) {
        double x = first.getX() - second.getX();
        double z = first.getZ() - second.getZ();
        return x * x + z * z;
    }

    private static boolean blink(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        Vec3 start = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        double maxDistance = SkillScaling.blinkDistance(rank);
        Vec3 safest = start;

        for (double distance = 0.5; distance <= maxDistance; distance += 0.5) {
            Vec3 candidate = start.add(direction.scale(distance));
            AABB movedBox = player.getBoundingBox().move(candidate.subtract(start));
            if (!level.noCollision(player, movedBox) || level.containsAnyLiquid(movedBox)) {
                break;
            }
            safest = candidate;
        }

        if (safest.distanceToSqr(start) < 0.25) {
            return false;
        }
        SkillParticleEffects.travelStreak(level, start.add(0.0, 1.0, 0.0), safest.add(0.0, 1.0, 0.0),
                0xB566FF, 1.0F, 12);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.2F);
        player.teleportTo(safest.x, safest.y, safest.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, SkillScaling.blinkProtectionTicks(rank), 0, false, false, true), player);
        SkillParticleEffects.burst(level, safest.add(0.0, 1.0, 0.0), 0xD6A4FF, 1.0F, 10);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.5F);
        return true;
    }

    private static boolean magicShield(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        double radius = SkillScaling.magicShieldCleanseRadius(rank);
        removeHarmfulEffects(player);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius, 3.0, radius),
                target -> target != player
                        && isFriendlyHealingTarget(player, target)
                        && horizontalDistanceSqr(player, target) <= radius * radius
                        && Math.abs(target.getY() - player.getY()) <= 3.0
        );
        targets.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        int cleansedPlayers = 0;
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer && cleansedPlayers++ >= 5) {
                continue;
            }
            removeHarmfulEffects(target);
        }
        SkillParticleEffects.attached(level, SkillVfxType.ARCANE_SHIELD, player, Vec3.ZERO,
                0x5EBBFF, 1.72F, 100);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.45F);
        return true;
    }

    private static void removeHarmfulEffects(LivingEntity target) {
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> harmfulEffects = target.getActiveEffects()
                .stream()
                .filter(instance -> instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .map(MobEffectInstance::getEffect)
                .toList();
        harmfulEffects.forEach(target::removeEffect);
    }

    private static boolean frostNova(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        Vec3 center = aimedLivingOrBlockPoint(player, 20.0);
        double radius = SkillScaling.frostNovaRadius(rank);
        int duration = SkillScaling.frostNovaSlowTicks(rank);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(radius),
                target -> target != player && target.isAlive() && isHostileTarget(player, target)
                        && target.position().distanceToSqr(center) <= radius * radius
        );
        targets.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        int controlledPlayers = 0;
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer && controlledPlayers++ >= 5) {
                continue;
            }
            target.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.frostNovaDamage(rank));
            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    PvpBalance.softControlDuration(target, duration, 40),
                    PvpBalance.softControlAmplifier(target, SkillScaling.frostNovaSlowAmplifier(rank)),
                    false,
                    true,
                    true
            ), player);
        }
        SkillParticleEffects.frostField(level, center, (float) radius, 16);
        SkillParticleEffects.frostVapor(level, center, (float) radius);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.75F);
        return true;
    }

    private static boolean meteor(ServerPlayer player, int rank) {
        Vec3 target = aimedGroundPoint(player, SkillScaling.meteorCastRange(rank));
        SkillRuntimeEffects.scheduleMeteor(player, rank, target);
        SkillParticleEffects.areaRing(player.level(), target, 0xFF5A18,
                (float) SkillScaling.meteorRadius(rank), 24);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.55F);
        return true;
    }

    static void impactMeteor(ServerPlayer player, int rank, Vec3 target) {
        if (!player.isAlive()) {
            return;
        }
        ServerLevel level = player.level();
        double radius = SkillScaling.meteorRadius(rank);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(target, target).inflate(radius, 3.5, radius),
                living -> living != player && living.isAlive() && isHostileTarget(player, living)
        );
        for (LivingEntity living : targets) {
            double distance = Math.sqrt(living.distanceToSqr(target));
            float multiplier = (float) Math.max(0.45, 1.0 - distance / (radius * 1.8));
            if (living.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.meteorDamage(rank) * multiplier)) {
                living.igniteForSeconds(living instanceof ServerPlayer ? 2.0F : 3.0F + rank * 0.15F);
            }
        }
        SkillParticleEffects.meteorImpact(level, target, (float) radius);
        int particleMultiplier = Math.max(1, (int) Math.ceil(SkillScaling.meteorSizeMultiplier(rank)));
        level.sendParticles(ParticleTypes.EXPLOSION, target.x, target.y + 0.6, target.z, 24 * particleMultiplier, radius * 0.55, 0.9, radius * 0.55, 0.08);
        level.sendParticles(ParticleTypes.FLAME, target.x, target.y + 0.4, target.z, 240 * particleMultiplier, radius * 0.85, 1.2, radius * 0.85, 0.2);
        level.sendParticles(ParticleTypes.LAVA, target.x, target.y + 0.7, target.z, 75 * particleMultiplier, radius * 0.72, 1.0, radius * 0.72, 0.14);
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, target.x, target.y + 0.4, target.z, 3 * particleMultiplier, 0.25, 0.15, 0.25, 0.02);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4F, 0.65F);
    }

    private static boolean skyRays(ServerPlayer player, int rank) {
        Vec3 center = aimedGroundPoint(player, 36.0);
        SkillRuntimeEffects.startSkyRays(player, rank, center);
        SkillParticleEffects.areaRing(player.level(), center, 0xFFFFD04A,
                (float) SkillScaling.skyRaysRadius(rank), 26);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.1F, 1.35F);
        return true;
    }

    static void skyRaysPulse(ServerPlayer player, int rank, Vec3 center) {
        ServerLevel level = player.level();
        double radius = SkillScaling.skyRaysRadius(rank);
        List<LivingEntity> candidates = new java.util.ArrayList<>(level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(radius, 10.0, radius),
                target -> target != player && target.isAlive() && isHostileTarget(player, target)
                        && target.position().distanceToSqr(center) <= radius * radius
        ));
        int bestPriority = candidates.stream().mapToInt(SkillExecutor::skyRayPriority).min().orElse(3);
        candidates.removeIf(target -> skyRayPriority(target) != bestPriority);
        candidates.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        int rayCount = Math.min(SkillScaling.skyRaysTargets(rank), candidates.size());
        if (rayCount == 0) {
            rayCount = SkillScaling.skyRaysTargets(rank);
            for (int index = 0; index < rayCount; index++) {
                double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
                double distance = 2.0 + level.getRandom().nextDouble() * Math.max(1.0, radius - 2.0);
                Vec3 point = findSurface(level, center.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance));
                SkillParticleEffects.skyRayCross(level, point.add(0.0, 0.15, 0.0));
            }
        } else {
            int offset = (int) ((level.getGameTime() / 8L) % Math.max(1, candidates.size()));
            for (int index = 0; index < rayCount; index++) {
                LivingEntity target = candidates.get((offset + index) % candidates.size());
                Vec3 impact = findSurface(level, target.position());
                SkillParticleEffects.skyRayCross(level, impact);
                target.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.skyRaysDamage(rank));
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.65F, 1.65F);
    }

    private static int skyRayPriority(LivingEntity target) {
        if (target instanceof Enemy || target instanceof Monster) {
            return 0;
        }
        if (target instanceof Mob) {
            return 1;
        }
        if (target instanceof net.minecraft.world.entity.player.Player) {
            return 2;
        }
        return 1;
    }

    private static boolean chainLightning(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle().normalize();
        List<LivingEntity> candidates = new java.util.ArrayList<>(level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(24.0),
                target -> canChainLightningHit(player, target)
                        && player.hasLineOfSight(target)
                        && directionDot(player.getEyePosition(), target.getEyePosition(), look) >= 0.975
        ));
        candidates.sort(java.util.Comparator
                .comparingInt(SkillExecutor::skyRayPriority)
                .thenComparingDouble(player::distanceToSqr));
        if (candidates.isEmpty()) {
            return false;
        }

        java.util.Set<UUID> hit = new java.util.HashSet<>();
        LivingEntity current = candidates.getFirst();
        Vec3 start = player.getEyePosition().add(look.scale(0.7));
        float damage = SkillScaling.chainLightningDamage(rank);
        for (int jump = 0; jump < SkillScaling.chainLightningTargets(rank) && current != null; jump++) {
            Vec3 end = current.getEyePosition();
            SkillParticleEffects.lightningArc(level, start, end);
            current.hurtServer(level, player.damageSources().playerAttack(player), damage);
            hit.add(current.getUUID());
            start = end;
            LivingEntity previous = current;
            current = level.getEntitiesOfClass(
                            LivingEntity.class,
                            previous.getBoundingBox().inflate(SkillScaling.chainLightningJumpRange(rank)),
                            target -> canChainLightningHit(player, target)
                                    && previous.hasLineOfSight(target) && !hit.contains(target.getUUID())
                    ).stream()
                    .min(java.util.Comparator
                            .comparingInt(SkillExecutor::skyRayPriority)
                            .thenComparingDouble(previous::distanceToSqr))
                    .orElse(null);
            damage *= (float) SkillScaling.chainLightningFalloff(rank);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.75F, 1.35F);
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.15F);
        return true;
    }

    private static boolean canChainLightningHit(ServerPlayer caster, LivingEntity target) {
        return target != caster
                && target.isAlive()
                && (!(target instanceof ServerPlayer other) || PvpBalance.canDamagePlayer(caster, other));
    }

    private static boolean dash(ServerPlayer player, int rank) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4) {
            double yaw = Math.toRadians(player.getYRot());
            horizontalLook = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        }
        horizontalLook = horizontalLook.normalize();
        Vec3 right = new Vec3(-horizontalLook.z, 0.0, horizontalLook.x);
        var input = player.getLastClientInput();
        Vec3 direction = Vec3.ZERO;
        if (input.forward()) {
            direction = direction.add(horizontalLook);
        }
        if (input.backward()) {
            direction = direction.subtract(horizontalLook);
        }
        if (input.right()) {
            direction = direction.add(right);
        }
        if (input.left()) {
            direction = direction.subtract(right);
        }
        if (direction.lengthSqr() < 1.0E-4) {
            direction = horizontalLook;
        }
        double speed = SkillScaling.dashSpeed(rank);
        Vec3 velocity = direction.normalize().scale(speed);
        if (Math.abs(velocity.y) < 0.18) {
            velocity = velocity.add(0.0, 0.18, 0.0);
        }
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        SkillRuntimeEffects.startDashFallProtection(player);
        SkillParticleEffects.travelStreak(player.level(), player.position().add(0.0, 0.9, 0.0),
                player.position().add(velocity.scale(2.2)).add(0.0, 0.9, 0.0),
                0x83F5FF, 0.75F, 10);
        return true;
    }

    private static boolean windrun(ServerPlayer player, int rank) {
        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED,
                SkillScaling.windrunDurationTicks(rank),
                SkillScaling.windrunSpeedAmplifier(rank),
                false,
                true,
                true
        ), player);
        SkillRuntimeEffects.startWindrun(player, SkillScaling.windrunDurationTicks(rank));
        SkillParticleEffects.attached(player.level(), SkillVfxType.HUNTER_AFTERIMAGE, player,
                player.getLookAngle().multiply(1.0, 0.0, 1.0), 0xD9FFFF, 0.8F,
                SkillScaling.windrunDurationTicks(rank));
        return true;
    }

    private static boolean camouflage(ServerPlayer player, int rank) {
        int duration = SkillScaling.camouflageDurationTicks(rank);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true), player);
        SkillRuntimeEffects.startCamouflage(player, duration);
        List<Monster> nearby = player.level().getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(48.0),
                monster -> monster.getTarget() == player
        );
        nearby.forEach(monster -> monster.setTarget(null));
        SkillParticleEffects.attached(player.level(), SkillVfxType.HUNTER_CLOAK, player, Vec3.ZERO,
                0x55CFFF, 1.35F, 16);
        return true;
    }

    private static boolean multishot(ServerPlayer player, int rank) {
        boolean active = SkillRuntimeEffects.toggleMultishot(player);
        player.sendOverlayMessage(Component.translatable(
                active ? "message.basicrpgclasses.multishot_on" : "message.basicrpgclasses.multishot_off"
        ));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.7F, active ? 1.35F : 0.8F);
        return true;
    }

    private static boolean arrowRain(ServerPlayer player, int rank) {
        SkillRuntimeEffects.armArrowRain(player, rank);
        player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.arrow_rain_armed"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8F, 0.7F);
        return true;
    }

    static void spawnRainArrow(
            ServerPlayer player,
            int rank,
            Vec3 center,
            LivingEntity forcedTarget,
            UUID castId
    ) {
        ServerLevel level = player.level();
        double radius = SkillScaling.arrowRainRadius(rank);
        LivingEntity target = forcedTarget;
        double angle = target == null
                ? level.getRandom().nextDouble() * Math.PI * 2.0
                : Math.atan2(target.getZ() - center.z, target.getX() - center.x) + Math.PI;
        double distance = target == null ? Math.sqrt(level.getRandom().nextDouble()) * radius : 8.0;
        double x = (target == null ? center.x : target.getX()) + Math.cos(angle) * distance;
        double z = (target == null ? center.z : target.getZ()) + Math.sin(angle) * distance;
        double y = (target == null ? center.y : target.getEyeY()) + 4.0 + level.getRandom().nextDouble() * 1.5;
        ItemStack arrowStack = net.minecraft.world.item.Items.ARROW.getDefaultInstance();
        Arrow arrow = new Arrow(level, x, y, z, arrowStack, player.getMainHandItem().copy());
        arrow.setOwner(player);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        int shotPowerRank = player.getData(net.cgerwyu.basicrpgclasses.data.ModAttachments.PLAYER_CLASS)
                .skillRank(SkillId.HUNTER_SHOT_POWER);
        arrow.setBaseDamage(SkillScaling.arrowRainArrowDamage(rank)
                * SkillScaling.hunterShotDamageMultiplier(shotPowerRank));
        Vec3 aim = target == null
                ? center.add((level.getRandom().nextDouble() - 0.5) * radius, 0.2, (level.getRandom().nextDouble() - 0.5) * radius)
                : target.getEyePosition().add(target.getDeltaMovement().scale(2.2));
        Vec3 direction = aim.subtract(arrow.position()).normalize();
        arrow.shoot(direction.x, direction.y, direction.z,
                4.0F * (float) SkillScaling.hunterShotVelocityMultiplier(shotPowerRank), 0.0F);
        SkillRuntimeEffects.markArrowRainDamageProjectile(arrow, castId, player);
        level.addFreshEntity(arrow);
        SkillParticleEffects.projectileTrail(level, arrow, 0x8FE8FF, 0.52F, 60);
    }

    private static boolean powerShot(ServerPlayer player, int rank) {
        SkillRuntimeEffects.armPowerShot(player, rank);
        player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.power_shot_armed"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.8F, 1.45F);
        return true;
    }

    static void impactPowerShot(ServerPlayer player, int rank, Vec3 start, Vec3 impact) {
        ServerLevel level = player.level();
        Vec3 travel = impact.subtract(start);
        double range = travel.length();
        if (range < 0.05) {
            return;
        }
        Vec3 direction = travel.normalize();
        double width = SkillScaling.powerShotWidth(rank);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(start, impact).inflate(width),
                target -> target != player && target.isAlive() && isHostileTarget(player, target)
                        && distanceToRay(start, direction, target.getEyePosition(), range + 1.0) <= width
        );
        int shotPowerRank = player.getData(net.cgerwyu.basicrpgclasses.data.ModAttachments.PLAYER_CLASS)
                .skillRank(SkillId.HUNTER_SHOT_POWER);
        for (LivingEntity target : targets) {
            target.hurtServer(level, player.damageSources().playerAttack(player),
                    SkillScaling.powerShotDamage(rank) * (float) SkillScaling.hunterShotDamageMultiplier(shotPowerRank));
            target.addDeltaMovement(direction.scale(0.7));
        }
        SkillParticleEffects.piercingVolley(level, start, impact, (float) width);
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.2F, 1.25F);
    }

    private static boolean frostArrows(ServerPlayer player) {
        boolean active = SkillRuntimeEffects.toggleFrostArrows(player);
        player.sendOverlayMessage(Component.translatable(
                active ? "message.basicrpgclasses.frost_arrows_on" : "message.basicrpgclasses.frost_arrows_off"
        ));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.75F, active ? 1.65F : 0.85F);
        return true;
    }

    private static boolean groundStun(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-4) {
            return false;
        }
        forward = forward.normalize();
        double closeRadius = SkillScaling.groundStunRadius(rank);
        double range = SkillScaling.groundStunRange(rank);
        double coneDot = Math.cos(Math.toRadians(65.0));
        int requestedDuration = SkillScaling.groundStunDurationTicks(rank);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range, 2.5, range),
                target -> target != player && target.isAlive() && isHostileTarget(player, target)
        );
        targets.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        int controlledPlayers = 0;
        for (LivingEntity target : targets) {
            Vec3 offset = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            double distance = offset.length();
            boolean close = distance <= closeRadius;
            boolean inCone = distance <= range
                    && distance > 1.0E-4
                    && offset.normalize().dot(forward) >= coneDot;
            if (!close && !inCone) {
                continue;
            }
            if (target instanceof ServerPlayer && controlledPlayers++ >= 5) {
                continue;
            }
            target.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.groundStunDamage(rank));
            int duration = PvpBalance.claimHardControl(target, requestedDuration, 16);
            if (duration > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 8, false, true, true), player);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2, false, true, true), player);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true), player);
                SkillRuntimeEffects.startGroundStun(target, duration);
                SkillRuntimeEffects.startMobilityLock(target, target instanceof ServerPlayer ? 40 : duration);
            }
        }

        SkillParticleEffects.groundTremor(level, player.position(), forward, range, 65.0);
        SkillParticleEffects.groundSmoke(level, player.position(), (float) range);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.65F);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return true;
    }

    private static boolean shieldBash(ServerPlayer player, int rank) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() < 1.0E-4) {
            return false;
        }
        look = look.normalize();
        double range = SkillScaling.shieldBashRange(rank);
        Vec3 forward = look;
        LivingEntity target = level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(range, 2.0, range),
                        living -> living != player && living.isAlive() && isHostileTarget(player, living)
                                && player.hasLineOfSight(living)
                                && directionDot(player.position(), living.position(), forward) >= 0.90
                ).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        if (target == null) {
            return false;
        }
        lungeToward(player, target, 1.65);
        int duration = PvpBalance.claimHardControl(target, SkillScaling.shieldBashStunTicks(rank), 12);
        target.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.shieldBashDamage(rank));
        if (duration > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 8, false, true, true), player);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true), player);
            SkillRuntimeEffects.startGroundStun(target, duration);
            SkillRuntimeEffects.startMobilityLock(target, target instanceof ServerPlayer ? 50 : duration);
        }
        Vec3 impact = target.getEyePosition();
        SkillParticleEffects.world(level, SkillVfxType.KINETIC_BURST,
                impact, player.getEyePosition(), 0xE7EDF7, 1.15F, 12);
        level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.2F, 0.65F);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return true;
    }

    private static boolean battleCry(ServerPlayer player, int rank) {
        int duration = SkillScaling.battleCryDurationTicks(rank);
        player.addEffect(new MobEffectInstance(
                MobEffects.STRENGTH,
                duration,
                SkillScaling.battleCryStrengthAmplifier(rank),
                false,
                true,
                true
        ), player);
        player.addEffect(new MobEffectInstance(
                MobEffects.HASTE,
                duration,
                SkillScaling.battleCryHasteAmplifier(rank),
                false,
                true,
                true
        ), player);
        SkillParticleEffects.attached(player.level(), SkillVfxType.WARRIOR_AURA, player, Vec3.ZERO,
                0xEF334E, 1.25F, duration);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.82F);
        return true;
    }

    private static boolean berserk(ServerPlayer player, int rank) {
        int duration = SkillScaling.berserkDurationTicks(rank);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, rank >= 10 ? 1 : 0, false, true, true), player);
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, duration, rank >= 7 ? 1 : 0, false, true, true), player);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, true, true), player);
        SkillParticleEffects.attached(player.level(), SkillVfxType.WARRIOR_AURA, player, Vec3.ZERO,
                0xFF3A28, 1.45F, duration);
        return true;
    }

    private static boolean ultraThrust(ServerPlayer player, int rank) {
        LivingEntity target = aimedHostileTarget(player, 18.0);
        if (target == null) {
            return false;
        }
        SkillRuntimeEffects.startUltraThrustCast(player, rank, target);
        return true;
    }

    /** Resolves after the warrior has completed the charge-up. */
    static boolean finishUltraThrust(ServerPlayer player, int rank, UUID targetId) {
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target)
                || !isHostileTarget(player, target)) {
            return false;
        }
        ServerLevel level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 impact = target.getEyePosition();
        Vec3 direction = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        direction = direction.lengthSqr() < 1.0E-4 ? player.getLookAngle().multiply(1.0, 0.0, 1.0) : direction.normalize();
        Vec3 destination = target.position().subtract(direction.scale(1.25));
        AABB destinationBox = player.getBoundingBox().move(destination.subtract(player.position()));
        if (!level.noCollision(player, destinationBox)) {
            return false;
        }
        target.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.ultraThrustDamage(rank));
        SkillParticleEffects.sonicDash(level, player.position(), destination, 1.25F);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        SkillParticleEffects.world(level, SkillVfxType.KINETIC_BURST, impact, start, 0x27000A, 1.45F, 16);
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.2F, 0.55F);
        return true;
    }

    private static boolean warriorLeap(ServerPlayer player, int rank) {
        LivingEntity target = aimedHostileTarget(player, SkillScaling.warriorLeapRange(rank));
        if (target == null) {
            return false;
        }
        ServerLevel level = player.level();
        Vec3 flat = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        if (flat.lengthSqr() < 1.0E-4) {
            return false;
        }
        Vec3 direction = flat.normalize();
        Vec3 landing = target.position().subtract(direction.scale(1.6));
        double distance = flat.length();
        Vec3 leapVelocity = direction.scale(Math.clamp(distance * 0.10, 0.75, 1.55)).add(0.0, 0.68, 0.0);
        SkillParticleEffects.travelStreak(level, player.position().add(0.0, 0.9, 0.0),
                landing.add(0.0, 1.1, 0.0), 0xC73435, 0.72F, 12);
        player.setDeltaMovement(leapVelocity);
        player.hurtMarked = true;
        SkillRuntimeEffects.startDashFallProtection(player);
        target.hurtServer(level, player.damageSources().playerAttack(player), SkillScaling.warriorLeapDamage(rank));
        SkillParticleEffects.warriorLeapImpact(level, landing, 1.25F);
        level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.85F, 0.72F);
        return true;
    }

    private static boolean execution(ServerPlayer player, int rank) {
        LivingEntity target = aimedHostileTarget(player, 7.0);
        if (target == null) {
            return false;
        }
        float damage = SkillScaling.executionDamage(rank);
        if (target.getHealth() <= target.getMaxHealth() * 0.30F) {
            damage *= 1.75F;
        }
        target.hurtServer(player.level(), player.damageSources().playerAttack(player), damage);
        lungeToward(player, target, 1.8);
        SkillParticleEffects.world(player.level(), SkillVfxType.KINETIC_BURST,
                target.getEyePosition(), player.getEyePosition(), 0xC51F32, 1.6F, 14);
        return true;
    }

    private static boolean restoration(ServerPlayer player, int rank) {
        LivingEntity target = aimedFriendlyTarget(player, 20.0);
        SkillRuntimeEffects.startRestoration(player, target == null ? player : target, rank);
        return true;
    }

    private static boolean healingHalo(ServerPlayer player, int rank) {
        double radius = SkillScaling.holyRadius(rank);
        float amount = SkillScaling.healingHaloAmount(rank) * PvpBalance.healingMultiplier(player);
        for (LivingEntity target : friendlyTargets(player, radius, 8)) {
            target.heal(amount);
            SkillParticleEffects.attached(player.level(), SkillVfxType.BURST, target, Vec3.ZERO,
                    0xFFFFD8, 0.8F, 22);
        }
        SkillParticleEffects.holyField(player.level(), player.position(), (float) radius, 34);
        return true;
    }

    private static boolean resurrection(ServerPlayer player, int rank) {
        ServerPlayer target = player.level().getServer().getPlayerList().getPlayers().stream()
                .filter(other -> other != player && other.level() == player.level() && !other.isAlive())
                .filter(other -> other.distanceToSqr(player) <= 16.0 * 16.0)
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        if (target == null) {
            return false;
        }
        Vec3 deathPosition = target.position();
        ServerPlayer revived = player.level().getServer().getPlayerList().respawn(
                target, true, Entity.RemovalReason.KILLED
        );
        revived.teleportTo(deathPosition.x, deathPosition.y, deathPosition.z);
        revived.setHealth(Math.max(1.0F, revived.getMaxHealth() * (0.25F + rank * 0.01F)));
        removeHarmfulEffects(revived);
        revived.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true), player);
        SkillParticleEffects.attached(player.level(), SkillVfxType.FIREBALL_AURA, revived, Vec3.ZERO,
                0xFF8A12, 2.1F, 50);
        SkillParticleEffects.attached(player.level(), SkillVfxType.BURST, revived, Vec3.ZERO,
                0xFFFF58, 2.5F, 28);
        return true;
    }

    private static boolean blessing(ServerPlayer player, int rank) {
        int duration = SkillScaling.priestBuffTicks(rank);
        for (LivingEntity target : friendlyTargets(player, SkillScaling.holyRadius(rank), 8)) {
            target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 0, false, true, true), player);
            target.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, true, true), player);
            SkillParticleEffects.attached(player.level(), SkillVfxType.BURST, target, Vec3.ZERO,
                    0xFFE88A, 0.72F, 24);
        }
        return true;
    }

    private static boolean holyShield(ServerPlayer player, int rank) {
        int duration = SkillScaling.paladinBuffTicks(rank);
        for (LivingEntity target : friendlyTargets(player, SkillScaling.holyRadius(rank), 8)) {
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                    SkillScaling.holyShieldAbsorptionAmplifier(rank), false, true, true), player);
            SkillParticleEffects.attached(player.level(), SkillVfxType.SHIELD, target, Vec3.ZERO,
                    0xFFF2A6, 1.25F, duration);
        }
        return true;
    }

    private static boolean cleanse(ServerPlayer player, int rank) {
        LivingEntity aimed = aimedFriendlyTarget(player, 20.0);
        LivingEntity target = aimed == null ? player : aimed;
        removeHarmfulEffects(target);
        SkillParticleEffects.attached(player.level(), SkillVfxType.BURST, target, Vec3.ZERO,
                0xFFFFFF, 1.0F, 20);
        return true;
    }

    private static boolean holyStorm(ServerPlayer player, int rank) {
        Vec3 target = aimedGroundPoint(player, 32.0);
        SkillRuntimeEffects.startHolyStorm(player, rank, target);
        SkillParticleEffects.areaRing(player.level(), target, 0xFFF1A8,
                (float) SkillScaling.holyRadius(rank), 26);
        return true;
    }

    static void holyStormPulse(ServerPlayer player, int rank, Vec3 center) {
        double radius = SkillScaling.holyRadius(rank);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius, 4.0, radius),
                target -> target != player && target.isAlive() && isHostileTarget(player, target));
        for (LivingEntity target : targets) {
            target.hurtServer(player.level(), player.damageSources().playerAttack(player), SkillScaling.holyStormDamage(rank));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 12, 0, false, false, true), player);
            Vec3 impact = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
            SkillParticleEffects.world(player.level(), SkillVfxType.BURST, impact, impact,
                    0xFFF5BD, 0.65F, 12);
        }
        SkillParticleEffects.holyField(player.level(), center, (float) radius, 14);
    }

    private static boolean paladinHeal(ServerPlayer player, int rank) {
        float amount = SkillScaling.paladinHealAmount(rank) * PvpBalance.healingMultiplier(player);
        for (LivingEntity target : friendlyTargets(player, 4.5, 6)) {
            target.heal(amount);
            SkillParticleEffects.attached(player.level(), SkillVfxType.BURST, target, Vec3.ZERO,
                    0xFFE09A, 0.6F, 16);
        }
        return true;
    }

    private static boolean paladinBlessing(ServerPlayer player, int rank) {
        int duration = SkillScaling.paladinBuffTicks(rank);
        for (LivingEntity target : friendlyTargets(player, 6.0, 8)) {
            target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0, false, true, true), player);
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, rank >= 10 ? 1 : 0, false, true, true), player);
            SkillParticleEffects.attached(player.level(), SkillVfxType.FORTIFY_SHIELDS, target, Vec3.ZERO,
                    0xFFD36A, 1.0F, duration);
        }
        return true;
    }

    private static boolean divineBulwark(ServerPlayer player, int rank) {
        int duration = SkillScaling.paladinBuffTicks(rank);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 1, false, true, true), player);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 2 + rank / 8, false, true, true), player);
        provoke(player, rank);
        SkillParticleEffects.attached(player.level(), SkillVfxType.HOLY_WINGS, player, Vec3.ZERO,
                0xFFFFC64D, 1.35F, duration);
        SkillParticleEffects.bulwarkShields(player.level(), player, 1.15F, duration);
        SkillParticleEffects.attached(player.level(), SkillVfxType.SHIELD, player, Vec3.ZERO,
                0xFFD46A, 1.8F, duration);
        return true;
    }

    private static boolean divineSlash(ServerPlayer player) {
        boolean active = SkillRuntimeEffects.toggleDivineSlash(player);
        player.sendOverlayMessage(Component.translatable(active
                ? "message.basicrpgclasses.divine_slash_on"
                : "message.basicrpgclasses.divine_slash_off"));
        return true;
    }

    private static List<LivingEntity> friendlyTargets(ServerPlayer player, double radius, int maximum) {
        List<LivingEntity> targets = new java.util.ArrayList<>(player.level().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius, 3.0, radius),
                target -> isFriendlyHealingTarget(player, target)
                        && horizontalDistanceSqr(player, target) <= radius * radius));
        targets.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        return targets.stream().limit(maximum).toList();
    }

    private static LivingEntity aimedFriendlyTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, start.add(look.scale(range))).inflate(1.5),
                        target -> target != player && isFriendlyHealingTarget(player, target) && player.hasLineOfSight(target))
                .stream()
                .filter(target -> distanceToRay(start, look, target.getEyePosition(), range) <= Math.max(0.8, target.getBbWidth()))
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static LivingEntity aimedLivingTarget(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(
                        LivingEntity.class, player.getBoundingBox().inflate(range),
                        target -> target != player && target.isAlive() && player.hasLineOfSight(target)
                                && directionDot(player.getEyePosition(), target.getEyePosition(), look) >= 0.975
                ).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static LivingEntity aimedHostileTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, start.add(look.scale(range))).inflate(1.5),
                        target -> target != player && isHostileTarget(player, target) && player.hasLineOfSight(target))
                .stream()
                .filter(target -> distanceToRay(start, look, target.getEyePosition(), range) <= Math.max(0.8, target.getBbWidth()))
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static Vec3 aimedBlockPoint(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        BlockHitResult result = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        return result.getType() == HitResult.Type.MISS ? end : result.getLocation();
    }

    private static Vec3 aimedLivingOrBlockPoint(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 blockPoint = aimedBlockPoint(player, range);
        double blockDistance = start.distanceTo(blockPoint);
        LivingEntity aimedEntity = player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(start, blockPoint).inflate(1.25),
                        target -> target != player && target.isAlive() && player.hasLineOfSight(target)
                ).stream()
                .filter(target -> distanceToRay(start, look, target.getEyePosition(), blockDistance) <= Math.max(0.6, target.getBbWidth() * 0.7))
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        return aimedEntity == null
                ? blockPoint
                : aimedEntity.position().add(0.0, aimedEntity.getBbHeight() * 0.5, 0.0);
    }

    private static Vec3 aimedGroundPoint(ServerPlayer player, double range) {
        Vec3 aimed = aimedBlockPoint(player, range);
        BlockPos start = BlockPos.containing(aimed);
        for (int down = 0; down <= 18; down++) {
            BlockPos candidate = start.below(down);
            if (!player.level().getBlockState(candidate).isAir()) {
                return new Vec3(aimed.x, candidate.getY() + 1.05, aimed.z);
            }
        }
        return aimed;
    }

    private static Vec3 findSurface(ServerLevel level, Vec3 around) {
        BlockPos start = BlockPos.containing(around.x, around.y + 8.0, around.z);
        for (int down = 0; down <= 24; down++) {
            BlockPos position = start.below(down);
            if (!level.getBlockState(position).isAir()) {
                return new Vec3(around.x, position.getY() + 1.0, around.z);
            }
        }
        return around;
    }

    private static Vec3 rotateAroundY(Vec3 direction, double angle) {
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return new Vec3(
                direction.x * cosine - direction.z * sine,
                direction.y,
                direction.x * sine + direction.z * cosine
        ).normalize();
    }

    private static void lungeToward(ServerPlayer player, LivingEntity target, double stoppingDistance) {
        Vec3 offset = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        double distance = offset.length();
        if (distance <= stoppingDistance || distance < 1.0E-4) {
            return;
        }
        double speed = Math.min(1.35, Math.max(0.72, (distance - stoppingDistance) * 0.32));
        Vec3 velocity = offset.normalize().scale(speed).add(0.0, 0.12, 0.0);
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        SkillParticleEffects.travelStreak(
                player.level(),
                player.position().add(0.0, 0.9, 0.0),
                player.position().add(velocity.scale(2.0)).add(0.0, 0.9, 0.0),
                0xDCE5F2, 0.72F, 10
        );
    }

    private static double directionDot(Vec3 start, Vec3 target, Vec3 direction) {
        Vec3 offset = target.subtract(start);
        return offset.lengthSqr() < 1.0E-6 ? 1.0 : offset.normalize().dot(direction);
    }

    private static double distanceToRay(Vec3 start, Vec3 direction, Vec3 point, double maximumRange) {
        Vec3 offset = point.subtract(start);
        double projection = offset.dot(direction);
        if (projection < 0.0 || projection > maximumRange) {
            return Double.MAX_VALUE;
        }
        return offset.subtract(direction.scale(projection)).length();
    }

    private static boolean isHostileTarget(ServerPlayer caster, LivingEntity target) {
        if (target instanceof Monster) {
            return true;
        }
        return target instanceof ServerPlayer other && PvpBalance.canDamagePlayer(caster, other);
    }

    private SkillExecutor() {
    }
}
