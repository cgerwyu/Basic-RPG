package net.cgerwyu.basicrpgclasses.skill;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerCombatData;
import net.cgerwyu.basicrpgclasses.data.InfiniteResourceManager;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.combat.PvpBalance;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.cgerwyu.basicrpgclasses.network.payload.ToggleSkillStatePayload;
import net.cgerwyu.basicrpgclasses.network.payload.CastStatePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

public final class SkillRuntimeEffects {
    private static final int FIREBALL_VOLLEY_INTERVAL_TICKS = 4;
    private static final Map<UUID, PendingFireballVolley> FIREBALL_VOLLEYS = new HashMap<>();
    private static final Map<UUID, TimedEffect> WHIRLWINDS = new HashMap<>();
    private static final Map<UUID, Long> WINDRUN_END_TICKS = new HashMap<>();
    private static final Map<UUID, DashFallProtection> DASH_FALL_PROTECTION = new HashMap<>();
    private static final Map<UUID, Long> GLIDE_NEXT_DRAIN_TICKS = new HashMap<>();
    private static final Map<UUID, GlideControl> GLIDE_CONTROLS = new HashMap<>();
    private static final Map<UUID, GroundStunEffect> GROUND_STUNS = new HashMap<>();
    private static final Map<UUID, PendingMeteor> METEORS = new HashMap<>();
    private static final Map<UUID, SkyRaysEffect> SKY_RAYS = new HashMap<>();
    private static final Map<UUID, ChanneledHeal> RESTORATIONS = new HashMap<>();
    private static final Map<UUID, HolyStormEffect> HOLY_STORMS = new HashMap<>();
    private static final Map<UUID, SolarBeamEffect> SOLAR_BEAMS = new HashMap<>();
    private static final Map<UUID, PendingUltraThrust> ULTRA_THRUST_CASTS = new HashMap<>();
    private static final Map<UUID, Boolean> CAST_HELD = new HashMap<>();
    private static final Map<UUID, PendingArrowRain> ARROW_RAINS = new HashMap<>();
    private static final Map<UUID, Long> CAMOUFLAGE_END_TICKS = new HashMap<>();
    private static final Map<UUID, Boolean> FROST_ARROW_TOGGLES = new HashMap<>();
    private static final Map<UUID, Boolean> MULTISHOT_TOGGLES = new HashMap<>();
    private static final Map<UUID, Boolean> DIVINE_SLASH_TOGGLES = new HashMap<>();
    private static final Set<UUID> DIVINE_SLASH_DAMAGE = new HashSet<>();
    private static final Map<UUID, Integer> ARMED_ARROW_RAIN = new HashMap<>();
    private static final Map<UUID, Integer> ARMED_POWER_SHOT = new HashMap<>();
    private static final Map<UUID, HunterShotContext> ACTIVE_HUNTER_SHOTS = new HashMap<>();
    private static final Map<UUID, TrackedProjectile> FROST_PROJECTILES = new HashMap<>();
    private static final Map<UUID, PowerProjectile> POWER_PROJECTILES = new HashMap<>();
    private static final Map<UUID, ArrowRainProjectile> ARROW_RAIN_PROJECTILES = new HashMap<>();
    private static final Map<UUID, MultishotProjectile> MULTISHOT_PROJECTILES = new HashMap<>();
    private static final Map<UUID, Set<UUID>> MULTISHOT_HITS = new HashMap<>();
    private static final Map<UUID, Long> MULTISHOT_VOLLEY_END_TICKS = new HashMap<>();
    private static final Map<UUID, RainDamageProjectile> RAIN_DAMAGE_PROJECTILES = new HashMap<>();
    private static final Map<UUID, Set<UUID>> RAIN_PLAYER_HITS = new HashMap<>();
    private static final Map<UUID, Long> RAIN_CAST_END_TICKS = new HashMap<>();
    private static final Map<UUID, Long> MOBILITY_LOCK_END_TICKS = new HashMap<>();
    private static final Map<UUID, Long> GLOBAL_COOLDOWN_END_TICKS = new HashMap<>();
    private static final Map<UUID, ChallengeEffect> CHALLENGES = new HashMap<>();

    public static void scheduleFireballVolley(ServerPlayer player, int rank, int remaining, Vec3 direction) {
        if (remaining <= 0) {
            return;
        }
        FIREBALL_VOLLEYS.put(player.getUUID(), new PendingFireballVolley(
                Math.clamp(rank, 1, SkillDefinitions.MAX_SKILL_RANK),
                remaining,
                player.level().getGameTime() + FIREBALL_VOLLEY_INTERVAL_TICKS,
                direction.normalize()
        ));
    }

    public static void startWhirlwind(ServerPlayer player) {
        int durationTicks = 12;
        long startTick = player.level().getGameTime();
        WHIRLWINDS.put(player.getUUID(), new TimedEffect(startTick, startTick + durationTicks));
    }

    public static void startWindrun(ServerPlayer player, int durationTicks) {
        WINDRUN_END_TICKS.put(player.getUUID(), player.level().getGameTime() + durationTicks);
    }

    public static boolean windrunActive(ServerPlayer player) {
        return WINDRUN_END_TICKS.getOrDefault(player.getUUID(), 0L) > player.level().getGameTime();
    }

    public static boolean ultraThrustCasting(ServerPlayer player) {
        return ULTRA_THRUST_CASTS.containsKey(player.getUUID());
    }

    public static boolean toggleDivineSlash(ServerPlayer player) {
        boolean active = !DIVINE_SLASH_TOGGLES.getOrDefault(player.getUUID(), false);
        DIVINE_SLASH_TOGGLES.put(player.getUUID(), active);
        syncToggle(player, SkillId.DIVINE_SLASH, active);
        return active;
    }

    public static boolean divineSlashActive(ServerPlayer player) {
        return DIVINE_SLASH_TOGGLES.getOrDefault(player.getUUID(), false);
    }

    public static boolean divineSlashDamageInProgress(ServerPlayer player) {
        return DIVINE_SLASH_DAMAGE.contains(player.getUUID());
    }

    public static void beginDivineSlashDamage(ServerPlayer player) {
        DIVINE_SLASH_DAMAGE.add(player.getUUID());
    }

    public static void finishDivineSlashDamage(ServerPlayer player) {
        DIVINE_SLASH_DAMAGE.remove(player.getUUID());
    }

    public static void startSolarBeam(ServerPlayer player, int rank) {
        clearActiveCast(player, false);
        SOLAR_BEAMS.put(player.getUUID(), new SolarBeamEffect(rank, player.position(), player.level().getGameTime()));
        syncCast(player, SkillId.SOLAR_BEAM, 0, true);
    }

    public static void startUltraThrustCast(ServerPlayer player, int rank, LivingEntity target) {
        long now = player.level().getGameTime();
        clearActiveCast(player, false);
        int castTicks = SkillScaling.ultraThrustCastTicks(rank);
        ULTRA_THRUST_CASTS.put(player.getUUID(), new PendingUltraThrust(
                rank, target.getUUID(), player.position(), now + castTicks
        ));
        syncCast(player, SkillId.ULTRA_THRUST, castTicks, true);
        SkillParticleEffects.attached(player.level(), SkillVfxType.WARRIOR_AURA, player, Vec3.ZERO,
                0xC4122D, 0.82F, castTicks);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS, 0.72F, 0.55F);
    }

    public static void startMobilityLock(LivingEntity target, int durationTicks) {
        long endTick = target.level().getGameTime() + Math.max(1, durationTicks);
        MOBILITY_LOCK_END_TICKS.merge(target.getUUID(), endTick, Math::max);
        if (target instanceof ServerPlayer player) {
            stopGliding(player);
        }
    }

    public static boolean mobilityLocked(LivingEntity target) {
        long endTick = MOBILITY_LOCK_END_TICKS.getOrDefault(target.getUUID(), 0L);
        if (endTick <= target.level().getGameTime() || !target.isAlive()) {
            MOBILITY_LOCK_END_TICKS.remove(target.getUUID());
            return false;
        }
        return true;
    }

    public static boolean globalCooldownActive(ServerPlayer player) {
        return GLOBAL_COOLDOWN_END_TICKS.getOrDefault(player.getUUID(), 0L) > player.level().getGameTime();
    }

    public static void startGlobalCooldown(ServerPlayer player) {
        GLOBAL_COOLDOWN_END_TICKS.put(player.getUUID(), player.level().getGameTime() + 7L);
    }

    public static void startChallenge(ServerPlayer challenged, ServerPlayer warrior, int durationTicks) {
        CHALLENGES.put(challenged.getUUID(), new ChallengeEffect(
                warrior.getUUID(),
                challenged.level().getGameTime() + Math.max(1, durationTicks)
        ));
    }

    public static float challengeDamageMultiplier(ServerPlayer attacker, ServerPlayer victim) {
        ChallengeEffect effect = CHALLENGES.get(attacker.getUUID());
        if (effect == null) {
            return 1.0F;
        }
        if (effect.endTick <= attacker.level().getGameTime() || !attacker.isAlive()) {
            CHALLENGES.remove(attacker.getUUID());
            return 1.0F;
        }
        return effect.warriorId.equals(victim.getUUID()) ? 1.0F : 0.72F;
    }

    public static void startDashFallProtection(ServerPlayer player) {
        DASH_FALL_PROTECTION.put(
                player.getUUID(),
                new DashFallProtection(player.level().getGameTime() + 200L, false)
        );
        player.resetFallDistance();
    }

    public static boolean consumeDashFallProtection(ServerPlayer player) {
        return DASH_FALL_PROTECTION.remove(player.getUUID()) != null;
    }

    public static boolean shouldDrainGlideMana(ServerPlayer player, long gameTime) {
        Long nextDrain = GLIDE_NEXT_DRAIN_TICKS.get(player.getUUID());
        if (nextDrain == null || gameTime >= nextDrain) {
            GLIDE_NEXT_DRAIN_TICKS.put(player.getUUID(), gameTime + 20L);
            return true;
        }
        return false;
    }

    public static void stopGliding(ServerPlayer player) {
        GLIDE_NEXT_DRAIN_TICKS.remove(player.getUUID());
        GlideControl control = GLIDE_CONTROLS.get(player.getUUID());
        if (control != null) {
            control.active = false;
            control.horizontalMomentum = Vec3.ZERO;
        }
    }

    /** A normal held jump never starts gliding. The player must release Space and press it again while airborne. */
    public static boolean updateGlideInput(ServerPlayer player, boolean jumpPressed) {
        GlideControl control = GLIDE_CONTROLS.computeIfAbsent(player.getUUID(), ignored -> new GlideControl());
        if (player.onGround()) {
            control.active = false;
            control.previousJump = jumpPressed;
            control.releasedAfterTakeoff = false;
            control.horizontalMomentum = Vec3.ZERO;
            return false;
        }
        boolean risingEdge = jumpPressed && !control.previousJump;
        if (risingEdge && control.releasedAfterTakeoff) {
            control.active = true;
            control.horizontalMomentum = player.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        }
        if (!jumpPressed) {
            control.active = false;
            control.releasedAfterTakeoff = true;
        }
        control.previousJump = jumpPressed;
        return control.active;
    }

    /** Keeps the speed captured when gliding started and turns that momentum gradually toward player input. */
    public static Vec3 steerGlide(ServerPlayer player, Vec3 desiredDirection, double minimumControlSpeed) {
        GlideControl control = GLIDE_CONTROLS.computeIfAbsent(player.getUUID(), ignored -> new GlideControl());
        Vec3 momentum = control.horizontalMomentum.multiply(1.0, 0.0, 1.0);
        if (momentum.lengthSqr() < 1.0E-5) {
            momentum = player.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        }
        double speed = momentum.length();
        Vec3 desired = desiredDirection.multiply(1.0, 0.0, 1.0);
        if (desired.lengthSqr() > 1.0E-5) {
            if (speed < minimumControlSpeed) {
                speed = minimumControlSpeed;
                momentum = desired.normalize().scale(speed);
            }
            Vec3 steered = momentum.normalize().scale(0.72).add(desired.normalize().scale(0.28));
            if (steered.lengthSqr() > 1.0E-5) {
                momentum = steered.normalize().scale(speed);
            }
        } else if (speed < 1.0E-4) {
            control.horizontalMomentum = Vec3.ZERO;
            return Vec3.ZERO;
        }
        control.horizontalMomentum = momentum;
        return momentum;
    }

    public static void startGroundStun(LivingEntity target, int durationTicks) {
        long endTick = target.level().getGameTime() + Math.max(1, durationTicks);
        GroundStunEffect existing = GROUND_STUNS.get(target.getUUID());
        if (existing == null || existing.endTick < endTick) {
            GROUND_STUNS.put(target.getUUID(), new GroundStunEffect(target, endTick));
            if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
                SkillParticleEffects.attached(level, SkillVfxType.KINETIC_BURST, target, Vec3.ZERO,
                        0xDCE5F2, 0.9F, 12);
            }
        }
    }

    public static boolean groundStunActive(LivingEntity target) {
        GroundStunEffect effect = GROUND_STUNS.get(target.getUUID());
        if (effect == null || effect.endTick <= target.level().getGameTime() || !target.isAlive()) {
            GROUND_STUNS.remove(target.getUUID());
            return false;
        }
        return true;
    }

    public static void scheduleMeteor(ServerPlayer player, int rank, Vec3 target) {
        long now = player.level().getGameTime();
        clearActiveCast(player, false);
        METEORS.put(player.getUUID(), new PendingMeteor(rank, target, player.position(), now, now + 50L));
        syncCast(player, SkillId.METEOR, 50, true);
        SkillParticleEffects.meteorFlightPath(
                player.level(), target.add(-4.0, 20.0, -2.0), target, rank, 50
        );
    }

    public static void scheduleArrowRain(ServerPlayer player, int rank, Vec3 center) {
        double radius = SkillScaling.arrowRainRadius(rank);
        UUID castId = UUID.randomUUID();
        int initialTargetCount = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(radius, 5.0, radius),
                target -> target != player && target.isAlive()
                        && (target instanceof net.minecraft.world.entity.monster.Monster
                        || target instanceof ServerPlayer other && PvpBalance.canDamagePlayer(player, other))
        ).size();
        ARROW_RAINS.put(player.getUUID(), new PendingArrowRain(
                castId,
                rank,
                center,
                Math.max(SkillScaling.arrowRainArrowCount(rank), initialTargetCount),
                player.level().getGameTime(),
                0
        ));
        RAIN_CAST_END_TICKS.put(castId, player.level().getGameTime() + 120L);
        RAIN_PLAYER_HITS.put(castId, new HashSet<>());
        SkillParticleEffects.hunterTargetField(player.level(), center, (float) radius, 34);
    }

    public static void startCamouflage(ServerPlayer player, int durationTicks) {
        CAMOUFLAGE_END_TICKS.put(player.getUUID(), player.level().getGameTime() + durationTicks);
    }

    public static boolean camouflageActive(ServerPlayer player) {
        long endTick = CAMOUFLAGE_END_TICKS.getOrDefault(player.getUUID(), 0L);
        if (endTick <= player.level().getGameTime()
                || !player.isAlive()
                || !player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
            CAMOUFLAGE_END_TICKS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void breakCamouflage(ServerPlayer player) {
        if (CAMOUFLAGE_END_TICKS.remove(player.getUUID()) == null) {
            return;
        }
        player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        SkillParticleEffects.attached(player.level(), SkillVfxType.HUNTER_CLOAK, player, Vec3.ZERO,
                0xBDEFFF, 0.8F, 8);
    }

    public static boolean toggleFrostArrows(ServerPlayer player) {
        boolean active = !FROST_ARROW_TOGGLES.getOrDefault(player.getUUID(), false);
        if (active) {
            FROST_ARROW_TOGGLES.put(player.getUUID(), true);
        } else {
            FROST_ARROW_TOGGLES.remove(player.getUUID());
        }
        syncToggle(player, SkillId.FROST_ARROWS, active);
        return active;
    }

    public static boolean frostArrowsActive(ServerPlayer player) {
        return FROST_ARROW_TOGGLES.getOrDefault(player.getUUID(), false);
    }

    public static boolean toggleMultishot(ServerPlayer player) {
        boolean active = !MULTISHOT_TOGGLES.getOrDefault(player.getUUID(), false);
        if (active) {
            MULTISHOT_TOGGLES.put(player.getUUID(), true);
        } else {
            MULTISHOT_TOGGLES.remove(player.getUUID());
        }
        syncToggle(player, SkillId.MULTISHOT, active);
        return active;
    }

    public static boolean multishotActive(ServerPlayer player) {
        return MULTISHOT_TOGGLES.getOrDefault(player.getUUID(), false);
    }

    public static void armArrowRain(ServerPlayer player, int rank) {
        ARMED_ARROW_RAIN.put(player.getUUID(), rank);
        syncToggle(player, SkillId.ARROW_RAIN, true);
    }

    public static boolean arrowRainArmed(ServerPlayer player) {
        return ARMED_ARROW_RAIN.containsKey(player.getUUID());
    }

    public static void armPowerShot(ServerPlayer player, int rank) {
        ARMED_POWER_SHOT.put(player.getUUID(), rank);
        syncToggle(player, SkillId.POWER_SHOT, true);
    }

    public static boolean powerShotArmed(ServerPlayer player) {
        return ARMED_POWER_SHOT.containsKey(player.getUUID());
    }

    public static void beginHunterShot(ServerPlayer player, boolean frostEnabled, boolean multishotEnabled) {
        int powerRank = ARMED_POWER_SHOT.remove(player.getUUID()) == null
                ? 0 : player.getData(ModAttachments.PLAYER_CLASS).skillRank(SkillId.POWER_SHOT);
        int rainRank = ARMED_ARROW_RAIN.remove(player.getUUID()) == null
                ? 0 : player.getData(ModAttachments.PLAYER_CLASS).skillRank(SkillId.ARROW_RAIN);
        if (powerRank > 0) {
            syncToggle(player, SkillId.POWER_SHOT, false);
        }
        if (rainRank > 0) {
            syncToggle(player, SkillId.ARROW_RAIN, false);
        }
        ACTIVE_HUNTER_SHOTS.put(player.getUUID(), new HunterShotContext(
                UUID.randomUUID(),
                frostEnabled,
                multishotEnabled,
                powerRank,
                rainRank
        ));
    }

    public static void endHunterShot(ServerPlayer player) {
        ACTIVE_HUNTER_SHOTS.remove(player.getUUID());
    }

    public static void registerHunterProjectile(ServerPlayer player, AbstractArrow arrow) {
        HunterShotContext context = ACTIVE_HUNTER_SHOTS.get(player.getUUID());
        if (context == null || !context.projectileIds.add(arrow.getUUID())) {
            return;
        }
        boolean primary = context.projectileCount++ == 0;
        if (context.multishot) {
            long endTick = arrow.level().getGameTime() + 100L;
            MULTISHOT_PROJECTILES.put(arrow.getUUID(), new MultishotProjectile(
                    arrow, context.volleyId, player.getUUID(), endTick
            ));
            MULTISHOT_HITS.computeIfAbsent(context.volleyId, ignored -> new HashSet<>());
            MULTISHOT_VOLLEY_END_TICKS.put(context.volleyId, endTick);
        }
        if (primary) {
            int shotPowerRank = player.getData(ModAttachments.PLAYER_CLASS).skillRank(SkillId.HUNTER_SHOT_POWER);
            arrow.setBaseDamage(2.0 * SkillScaling.hunterShotDamageMultiplier(shotPowerRank));
        }
        if (context.frost) {
            markFrostProjectile(arrow);
            SkillParticleEffects.projectileTrail(player.level(), arrow, 0x75DFFF, 0.62F, 100);
        }
        if (primary && context.powerRank > 0) {
            arrow.setBaseDamage(0.1);
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.45));
            arrow.setNoGravity(true);
            POWER_PROJECTILES.put(arrow.getUUID(), new PowerProjectile(
                    arrow,
                    player,
                    context.powerRank,
                    player.getEyePosition(),
                    arrow.level().getGameTime() + 1200L
            ));
            SkillParticleEffects.projectileTrail(player.level(), arrow, 0x3CCBFF, 1.05F, 100);
        }
        if (primary && context.rainRank > 0) {
            ARROW_RAIN_PROJECTILES.put(arrow.getUUID(), new ArrowRainProjectile(player, context.rainRank));
        }
    }

    public static boolean triggerArrowRainImpact(AbstractArrow arrow, Vec3 location) {
        ArrowRainProjectile projectile = ARROW_RAIN_PROJECTILES.remove(arrow.getUUID());
        if (projectile == null || !projectile.owner.isAlive()) {
            return false;
        }
        scheduleArrowRain(projectile.owner, projectile.rank, location);
        return true;
    }

    public static boolean triggerPowerShotImpact(AbstractArrow arrow, Vec3 location) {
        PowerProjectile projectile = POWER_PROJECTILES.remove(arrow.getUUID());
        if (projectile == null || !projectile.owner.isAlive()) {
            return false;
        }
        SkillExecutor.impactPowerShot(projectile.owner, projectile.rank, projectile.start, location);
        return true;
    }

    public static boolean isPowerProjectile(AbstractArrow arrow) {
        return POWER_PROJECTILES.containsKey(arrow.getUUID());
    }

    public static void markFrostProjectile(AbstractArrow arrow) {
        FROST_PROJECTILES.put(arrow.getUUID(), new TrackedProjectile(arrow, arrow.level().getGameTime() + 1200L));
    }

    public static boolean isFrostProjectile(AbstractArrow arrow) {
        TrackedProjectile projectile = FROST_PROJECTILES.get(arrow.getUUID());
        return projectile != null && projectile.endTick > arrow.level().getGameTime();
    }

    public static void markArrowRainDamageProjectile(AbstractArrow arrow, UUID castId, ServerPlayer owner) {
        RAIN_DAMAGE_PROJECTILES.put(arrow.getUUID(), new RainDamageProjectile(
                arrow,
                castId,
                owner.getUUID(),
                arrow.level().getGameTime() + 120L
        ));
    }

    /**
     * A volley is an area-clear tool, not a point-blank shotgun. Each Multishot
     * volley may damage a living target once, and each Arrow Rain cast may damage
     * a player once. PvE rain keeps its multi-arrow boss behaviour until boss
     * profiles provide a dedicated per-cast budget.
     */
    public static boolean rejectDuplicateHunterHit(AbstractArrow arrow, LivingEntity target) {
        MultishotProjectile multishot = MULTISHOT_PROJECTILES.get(arrow.getUUID());
        if (multishot != null) {
            Set<UUID> hitTargets = MULTISHOT_HITS.computeIfAbsent(multishot.volleyId, ignored -> new HashSet<>());
            if (!hitTargets.add(target.getUUID())) {
                return true;
            }
        }

        if (target instanceof ServerPlayer) {
            RainDamageProjectile rain = RAIN_DAMAGE_PROJECTILES.get(arrow.getUUID());
            if (rain != null) {
                Set<UUID> hitPlayers = RAIN_PLAYER_HITS.computeIfAbsent(rain.castId, ignored -> new HashSet<>());
                return !hitPlayers.add(target.getUUID());
            }
        }
        return false;
    }

    public static void startSkyRays(ServerPlayer player, int rank, Vec3 center) {
        long now = player.level().getGameTime();
        clearActiveCast(player, false);
        SKY_RAYS.put(player.getUUID(), new SkyRaysEffect(rank, center, player.position(), now + 60L, now + 10L));
        syncCast(player, SkillId.SKY_RAYS, 60, true);
    }

    public static void startRestoration(ServerPlayer player, LivingEntity target, int rank) {
        long now = player.level().getGameTime();
        int duration = SkillScaling.restorationCastTicks(rank);
        clearActiveCast(player, false);
        RESTORATIONS.put(player.getUUID(), new ChanneledHeal(target, rank, player.position(), now + duration, now));
        syncCast(player, SkillId.RESTORATION, duration, true);
    }

    public static void startHolyStorm(ServerPlayer player, int rank, Vec3 center) {
        long now = player.level().getGameTime();
        int duration = SkillScaling.holyStormCastTicks(rank);
        clearActiveCast(player, false);
        HOLY_STORMS.put(player.getUUID(), new HolyStormEffect(rank, center, player.position(), now + duration, now));
        syncCast(player, SkillId.HOLY_STORM, duration, true);
    }

    public static void setCastHeld(ServerPlayer player, boolean held) {
        if (held) {
            CAST_HELD.put(player.getUUID(), true);
        } else {
            CAST_HELD.remove(player.getUUID());
            interruptMajorCasts(player);
        }
    }

    public static void interruptMajorCasts(ServerPlayer player) {
        clearActiveCast(player, true);
    }

    public static void tick(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        tickGroundStuns(gameTime);
        tickTrackedProjectiles(player, gameTime);
        tickFireballVolley(player, gameTime);
        tickWhirlwind(player, gameTime);
        tickWindrun(player, gameTime);
        tickDashProtection(player, gameTime);
        tickUltraThrust(player, gameTime);
        tickMeteor(player, gameTime);
        tickSkyRays(player, gameTime);
        tickRestoration(player, gameTime);
        tickHolyStorm(player, gameTime);
        tickSolarBeam(player, gameTime);
        tickArrowRain(player, gameTime);
    }

    public static void clearPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        FIREBALL_VOLLEYS.remove(playerId);
        WHIRLWINDS.remove(playerId);
        WINDRUN_END_TICKS.remove(playerId);
        DASH_FALL_PROTECTION.remove(playerId);
        GLIDE_NEXT_DRAIN_TICKS.remove(playerId);
        GLIDE_CONTROLS.remove(playerId);
        GROUND_STUNS.remove(playerId);
        METEORS.remove(playerId);
        SKY_RAYS.remove(playerId);
        RESTORATIONS.remove(playerId);
        HOLY_STORMS.remove(playerId);
        SOLAR_BEAMS.remove(playerId);
        ULTRA_THRUST_CASTS.remove(playerId);
        CAST_HELD.remove(playerId);
        ARROW_RAINS.remove(playerId);
        CAMOUFLAGE_END_TICKS.remove(playerId);
        FROST_ARROW_TOGGLES.remove(playerId);
        MULTISHOT_TOGGLES.remove(playerId);
        DIVINE_SLASH_TOGGLES.remove(playerId);
        DIVINE_SLASH_DAMAGE.remove(playerId);
        ARMED_ARROW_RAIN.remove(playerId);
        ARMED_POWER_SHOT.remove(playerId);
        ACTIVE_HUNTER_SHOTS.remove(playerId);
        FROST_PROJECTILES.entrySet().removeIf(entry ->
                entry.getValue().arrow.getOwner() instanceof ServerPlayer owner
                        && owner.getUUID().equals(playerId));
        POWER_PROJECTILES.entrySet().removeIf(entry -> entry.getValue().owner.getUUID().equals(playerId));
        ARROW_RAIN_PROJECTILES.entrySet().removeIf(entry -> entry.getValue().owner.getUUID().equals(playerId));
        MULTISHOT_PROJECTILES.entrySet().removeIf(entry -> entry.getValue().ownerId.equals(playerId));
        RAIN_DAMAGE_PROJECTILES.entrySet().removeIf(entry -> entry.getValue().ownerId.equals(playerId));
        MOBILITY_LOCK_END_TICKS.remove(playerId);
        GLOBAL_COOLDOWN_END_TICKS.remove(playerId);
        CHALLENGES.remove(playerId);
        CHALLENGES.values().removeIf(effect -> effect.warriorId.equals(playerId));
        syncToggle(player, SkillId.FROST_ARROWS, false);
        syncToggle(player, SkillId.MULTISHOT, false);
        syncToggle(player, SkillId.DIVINE_SLASH, false);
        syncToggle(player, SkillId.ARROW_RAIN, false);
        syncToggle(player, SkillId.POWER_SHOT, false);
    }

    private static void tickFireballVolley(ServerPlayer player, long gameTime) {
        PendingFireballVolley volley = FIREBALL_VOLLEYS.get(player.getUUID());
        if (volley == null || gameTime < volley.nextShotTick()) {
            return;
        }
        if (!player.isAlive()
                || player.getData(ModAttachments.PLAYER_CLASS).rpgClass() != RpgClass.MAGE) {
            FIREBALL_VOLLEYS.remove(player.getUUID());
            return;
        }

        SkillExecutor.spawnFireball(player, volley.rank(), volley.direction());
        int remaining = volley.remaining() - 1;
        if (remaining <= 0) {
            FIREBALL_VOLLEYS.remove(player.getUUID());
        } else {
            FIREBALL_VOLLEYS.put(player.getUUID(), new PendingFireballVolley(
                    volley.rank(), remaining, gameTime + FIREBALL_VOLLEY_INTERVAL_TICKS, volley.direction()
            ));
        }
    }

    private static void tickWhirlwind(ServerPlayer player, long gameTime) {
        TimedEffect effect = WHIRLWINDS.get(player.getUUID());
        if (effect == null) {
            return;
        }
        if (gameTime >= effect.endTick() || !player.isAlive()) {
            WHIRLWINDS.remove(player.getUUID());
            return;
        }

        // Geometry and its rotation are rendered client-side by SLASH_ORBIT.
    }

    private static void tickWindrun(ServerPlayer player, long gameTime) {
        long endTick = WINDRUN_END_TICKS.getOrDefault(player.getUUID(), 0L);
        if (endTick <= gameTime || !player.isAlive()) {
            WINDRUN_END_TICKS.remove(player.getUUID());
            return;
        }
        // The persistent client-side WIND_TRAIL follows the player.
    }

    private static void tickDashProtection(ServerPlayer player, long gameTime) {
        DashFallProtection protection = DASH_FALL_PROTECTION.get(player.getUUID());
        if (protection == null) {
            return;
        }
        if (gameTime >= protection.endTick()) {
            DASH_FALL_PROTECTION.remove(player.getUUID());
        } else if (!player.onGround()) {
            protection.leftGround = true;
        } else if (protection.leftGround) {
            DASH_FALL_PROTECTION.remove(player.getUUID());
            player.resetFallDistance();
        }
    }

    private static void tickUltraThrust(ServerPlayer player, long gameTime) {
        PendingUltraThrust cast = ULTRA_THRUST_CASTS.get(player.getUUID());
        if (cast == null) {
            return;
        }
        if (!player.isAlive() || player.position().distanceToSqr(cast.startPosition()) > 0.16) {
            ULTRA_THRUST_CASTS.remove(player.getUUID());
            syncCast(player, SkillId.ULTRA_THRUST, 0, false);
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        if (gameTime >= cast.endTick()) {
            ULTRA_THRUST_CASTS.remove(player.getUUID());
            syncCast(player, SkillId.ULTRA_THRUST, 0, false);
            SkillExecutor.finishUltraThrust(player, cast.rank(), cast.targetId());
        }
    }

    private static void tickGroundStuns(long gameTime) {
        var iterator = GROUND_STUNS.values().iterator();
        while (iterator.hasNext()) {
            GroundStunEffect effect = iterator.next();
            if (effect.endTick <= gameTime || !effect.target.isAlive()) {
                iterator.remove();
                continue;
            }
            effect.target.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void tickMeteor(ServerPlayer player, long gameTime) {
        PendingMeteor meteor = METEORS.get(player.getUUID());
        if (meteor == null || gameTime < meteor.startTick()) {
            return;
        }
        if (!player.isAlive() || !castHeld(player)) {
            clearActiveCast(player, player.isAlive());
            return;
        }
        if (player.position().distanceToSqr(meteor.startPosition()) > 0.16) {
            clearActiveCast(player, true);
            return;
        }
        if (gameTime >= meteor.impactTick()) {
            METEORS.remove(player.getUUID());
            syncCast(player, SkillId.METEOR, 0, false);
            SkillExecutor.impactMeteor(player, meteor.rank(), meteor.target());
            return;
        }

        // The entire flight path was sent once when the meteor was scheduled.
    }

    private static void tickSkyRays(ServerPlayer player, long gameTime) {
        SkyRaysEffect effect = SKY_RAYS.get(player.getUUID());
        if (effect == null) {
            return;
        }
        if (!player.isAlive()
                || player.getData(ModAttachments.PLAYER_CLASS).rpgClass() != RpgClass.PRIEST
                || !castHeld(player)
                || player.position().distanceToSqr(effect.startPosition) > 0.16) {
            clearActiveCast(player, player.isAlive());
            return;
        }
        if (gameTime >= effect.endTick) {
            SKY_RAYS.remove(player.getUUID());
            syncCast(player, SkillId.SKY_RAYS, 0, false);
            return;
        }
        if (gameTime >= effect.nextStrikeTick) {
            SkillExecutor.skyRaysPulse(player, effect.rank, effect.center);
            effect.nextStrikeTick = gameTime + 10L;
        }
    }

    private static void tickRestoration(ServerPlayer player, long gameTime) {
        ChanneledHeal effect = RESTORATIONS.get(player.getUUID());
        if (effect == null) {
            return;
        }
        if (!player.isAlive() || !castHeld(player) || !effect.target.isAlive()
                || player.position().distanceToSqr(effect.startPosition) > 0.16
                || player.distanceToSqr(effect.target) > 24.0 * 24.0) {
            clearActiveCast(player, player.isAlive());
            return;
        }
        if (gameTime >= effect.endTick) {
            RESTORATIONS.remove(player.getUUID());
            syncCast(player, SkillId.RESTORATION, 0, false);
            return;
        }
        if (gameTime >= effect.nextPulseTick) {
            effect.target.heal(SkillScaling.restorationHealPerPulse(effect.rank));
            SkillParticleEffects.attached(player.level(), SkillVfxType.BURST, effect.target, Vec3.ZERO,
                    0xFFF2A6, 0.62F, 10);
            effect.nextPulseTick = gameTime + 5L;
        }
    }

    private static void tickHolyStorm(ServerPlayer player, long gameTime) {
        HolyStormEffect effect = HOLY_STORMS.get(player.getUUID());
        if (effect == null) {
            return;
        }
        if (!player.isAlive() || !castHeld(player)
                || player.position().distanceToSqr(effect.startPosition) > 0.16) {
            clearActiveCast(player, player.isAlive());
            return;
        }
        if (gameTime >= effect.endTick) {
            HOLY_STORMS.remove(player.getUUID());
            syncCast(player, SkillId.HOLY_STORM, 0, false);
            return;
        }
        if (gameTime >= effect.nextPulseTick) {
            SkillExecutor.holyStormPulse(player, effect.rank, effect.center);
            effect.nextPulseTick = gameTime + 6L;
        }
    }

    private static void tickSolarBeam(ServerPlayer player, long gameTime) {
        SolarBeamEffect effect = SOLAR_BEAMS.get(player.getUUID());
        if (effect == null) {
            return;
        }
        if (!player.isAlive() || !castHeld(player)
                || player.position().distanceToSqr(effect.startPosition) > 0.16) {
            SOLAR_BEAMS.remove(player.getUUID());
            syncCast(player, SkillId.SOLAR_BEAM, 0, false);
            return;
        }
        if (gameTime >= effect.nextDrainTick) {
            if (!InfiniteResourceManager.active(player)) {
                PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
                if (!combat.canAfford(3)) {
                    SOLAR_BEAMS.remove(player.getUUID());
                    syncCast(player, SkillId.SOLAR_BEAM, 0, false);
                    player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.solar_beam_no_mana"));
                    return;
                }
                player.setData(ModAttachments.PLAYER_COMBAT, combat.spendResource(3, gameTime));
            }
            effect.nextDrainTick = gameTime + 4L;
        }
        if (gameTime >= effect.nextPulseTick) {
            SkillExecutor.solarBeamPulse(player, effect.rank);
            effect.nextPulseTick = gameTime + 4L;
        }
    }

    private static boolean castHeld(ServerPlayer player) {
        return CAST_HELD.getOrDefault(player.getUUID(), false)
                && PlayerEquipmentManager.hasMainWeapon(player);
    }

    private static void clearActiveCast(ServerPlayer player, boolean notify) {
        UUID id = player.getUUID();
        boolean interrupted = METEORS.remove(id) != null;
        interrupted |= SKY_RAYS.remove(id) != null;
        interrupted |= RESTORATIONS.remove(id) != null;
        interrupted |= HOLY_STORMS.remove(id) != null;
        interrupted |= SOLAR_BEAMS.remove(id) != null;
        if (interrupted) {
            syncCast(player, SkillId.NONE, 0, false);
            if (notify) {
                player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.cast_interrupted"));
            }
        }
    }

    private static void syncCast(ServerPlayer player, SkillId skillId, int durationTicks, boolean active) {
        PacketDistributor.sendToPlayer(player, new CastStatePayload(skillId.numericId(), durationTicks, active));
    }

    private static void tickTrackedProjectiles(ServerPlayer player, long gameTime) {
        tickProjectileMap(player, gameTime, FROST_PROJECTILES, true);
        tickPowerProjectiles(player, gameTime);
        MULTISHOT_PROJECTILES.entrySet().removeIf(entry -> {
            MultishotProjectile projectile = entry.getValue();
            return projectile.endTick <= gameTime || !projectile.arrow.isAlive() || projectile.arrow.isRemoved();
        });
        RAIN_DAMAGE_PROJECTILES.entrySet().removeIf(entry -> {
            RainDamageProjectile projectile = entry.getValue();
            return projectile.endTick <= gameTime || !projectile.arrow.isAlive() || projectile.arrow.isRemoved();
        });
        MULTISHOT_VOLLEY_END_TICKS.entrySet().removeIf(entry -> {
            if (entry.getValue() > gameTime) {
                return false;
            }
            MULTISHOT_HITS.remove(entry.getKey());
            return true;
        });
        RAIN_CAST_END_TICKS.entrySet().removeIf(entry -> {
            if (entry.getValue() > gameTime) {
                return false;
            }
            RAIN_PLAYER_HITS.remove(entry.getKey());
            return true;
        });
        ARROW_RAIN_PROJECTILES.entrySet().removeIf(entry -> {
            ArrowRainProjectile projectile = entry.getValue();
            return !projectile.owner.isAlive()
                    || player.getUUID().equals(projectile.owner.getUUID())
                    && gameTime - projectile.createdTick > 1200L;
        });
    }

    private static void tickProjectileMap(
            ServerPlayer tickingPlayer,
            long gameTime,
            Map<UUID, TrackedProjectile> projectiles,
            boolean frost
    ) {
        var iterator = projectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            TrackedProjectile tracked = iterator.next().getValue();
            AbstractArrow arrow = tracked.arrow;
            if (tracked.endTick <= gameTime || !arrow.isAlive() || arrow.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!(arrow.getOwner() instanceof ServerPlayer owner)
                    || owner != tickingPlayer
                    || arrow.tickCount % 2 != 0) {
                continue;
            }
            // Persistent projectile VFX follows the entity; no per-tick packet spam.
        }
    }

    private static void tickPowerProjectiles(ServerPlayer player, long gameTime) {
        var iterator = POWER_PROJECTILES.values().iterator();
        while (iterator.hasNext()) {
            PowerProjectile projectile = iterator.next();
            AbstractArrow arrow = projectile.arrow;
            if (projectile.endTick <= gameTime || !arrow.isAlive() || arrow.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (projectile.owner != player || arrow.tickCount % 2 != 0) {
                continue;
            }
            double maximumRange = SkillScaling.powerShotRange(projectile.rank);
            if (arrow.position().distanceToSqr(projectile.start) >= maximumRange * maximumRange) {
                Vec3 impact = projectile.start.add(
                        arrow.position().subtract(projectile.start).normalize().scale(maximumRange)
                );
                iterator.remove();
                SkillExecutor.impactPowerShot(projectile.owner, projectile.rank, projectile.start, impact);
                arrow.discard();
            }
        }
    }

    private static void tickArrowRain(ServerPlayer player, long gameTime) {
        PendingArrowRain rain = ARROW_RAINS.get(player.getUUID());
        if (rain == null || gameTime < rain.nextArrowTick()) {
            return;
        }
        if (!player.isAlive() || rain.remaining() <= 0) {
            ARROW_RAINS.remove(player.getUUID());
            return;
        }

        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(rain.center(), rain.center()).inflate(SkillScaling.arrowRainRadius(rain.rank()), 5.0, SkillScaling.arrowRainRadius(rain.rank())),
                target -> target != player && target.isAlive()
                        && (target instanceof net.minecraft.world.entity.monster.Monster
                        || target instanceof ServerPlayer other && PvpBalance.canDamagePlayer(player, other))
        );
        targets.sort(java.util.Comparator.comparingInt(LivingEntity::getId));
        int arrowsThisTick = Math.min(3, rain.remaining());
        for (int index = 0; index < arrowsThisTick; index++) {
            LivingEntity target = targets.isEmpty()
                    ? null
                    : targets.get((rain.targetCursor() + index) % targets.size());
            SkillExecutor.spawnRainArrow(player, rain.rank(), rain.center(), target, rain.castId());
        }
        int remaining = rain.remaining() - arrowsThisTick;
        if (remaining <= 0) {
            ARROW_RAINS.remove(player.getUUID());
        } else {
            ARROW_RAINS.put(player.getUUID(), new PendingArrowRain(
                    rain.castId(), rain.rank(), rain.center(), remaining, gameTime + 1L, rain.targetCursor() + arrowsThisTick
            ));
        }
    }

    private record PendingFireballVolley(int rank, int remaining, long nextShotTick, Vec3 direction) {
    }

    private record TimedEffect(long startTick, long endTick) {
    }

    private record PendingMeteor(int rank, Vec3 target, Vec3 startPosition, long startTick, long impactTick) {
    }

    private record PendingUltraThrust(int rank, UUID targetId, Vec3 startPosition, long endTick) {
    }

    private record PendingArrowRain(UUID castId, int rank, Vec3 center, int remaining, long nextArrowTick, int targetCursor) {
    }

    private static final class GlideControl {
        private boolean previousJump;
        private boolean active;
        private boolean releasedAfterTakeoff;
        private Vec3 horizontalMomentum = Vec3.ZERO;
    }

    private static final class SkyRaysEffect {
        private final int rank;
        private final Vec3 center;
        private final Vec3 startPosition;
        private final long endTick;
        private long nextStrikeTick;

        private SkyRaysEffect(int rank, Vec3 center, Vec3 startPosition, long endTick, long nextStrikeTick) {
            this.rank = rank;
            this.center = center;
            this.startPosition = startPosition;
            this.endTick = endTick;
            this.nextStrikeTick = nextStrikeTick;
        }
    }

    private static final class ChanneledHeal {
        private final LivingEntity target;
        private final int rank;
        private final Vec3 startPosition;
        private final long endTick;
        private long nextPulseTick;

        private ChanneledHeal(LivingEntity target, int rank, Vec3 startPosition, long endTick, long nextPulseTick) {
            this.target = target;
            this.rank = rank;
            this.startPosition = startPosition;
            this.endTick = endTick;
            this.nextPulseTick = nextPulseTick;
        }
    }

    private static final class HolyStormEffect {
        private final int rank;
        private final Vec3 center;
        private final Vec3 startPosition;
        private final long endTick;
        private long nextPulseTick;

        private HolyStormEffect(int rank, Vec3 center, Vec3 startPosition, long endTick, long nextPulseTick) {
            this.rank = rank;
            this.center = center;
            this.startPosition = startPosition;
            this.endTick = endTick;
            this.nextPulseTick = nextPulseTick;
        }
    }

    private static final class SolarBeamEffect {
        private final int rank;
        private final Vec3 startPosition;
        private long nextPulseTick;
        private long nextDrainTick;

        private SolarBeamEffect(int rank, Vec3 startPosition, long now) {
            this.rank = rank;
            this.startPosition = startPosition;
            this.nextPulseTick = now;
            this.nextDrainTick = now;
        }
    }

    private static final class HunterShotContext {
        private final UUID volleyId;
        private final boolean frost;
        private final boolean multishot;
        private final int powerRank;
        private final int rainRank;
        private final Set<UUID> projectileIds = new HashSet<>();
        private int projectileCount;

        private HunterShotContext(UUID volleyId, boolean frost, boolean multishot, int powerRank, int rainRank) {
            this.volleyId = volleyId;
            this.frost = frost;
            this.multishot = multishot;
            this.powerRank = powerRank;
            this.rainRank = rainRank;
        }
    }

    private static final class TrackedProjectile {
        private final AbstractArrow arrow;
        private final long endTick;

        private TrackedProjectile(AbstractArrow arrow, long endTick) {
            this.arrow = arrow;
            this.endTick = endTick;
        }
    }

    private static final class MultishotProjectile {
        private final AbstractArrow arrow;
        private final UUID volleyId;
        private final UUID ownerId;
        private final long endTick;

        private MultishotProjectile(AbstractArrow arrow, UUID volleyId, UUID ownerId, long endTick) {
            this.arrow = arrow;
            this.volleyId = volleyId;
            this.ownerId = ownerId;
            this.endTick = endTick;
        }
    }

    private static final class RainDamageProjectile {
        private final AbstractArrow arrow;
        private final UUID castId;
        private final UUID ownerId;
        private final long endTick;

        private RainDamageProjectile(AbstractArrow arrow, UUID castId, UUID ownerId, long endTick) {
            this.arrow = arrow;
            this.castId = castId;
            this.ownerId = ownerId;
            this.endTick = endTick;
        }
    }

    private static final class PowerProjectile {
        private final AbstractArrow arrow;
        private final ServerPlayer owner;
        private final int rank;
        private final Vec3 start;
        private final long endTick;

        private PowerProjectile(AbstractArrow arrow, ServerPlayer owner, int rank, Vec3 start, long endTick) {
            this.arrow = arrow;
            this.owner = owner;
            this.rank = rank;
            this.start = start;
            this.endTick = endTick;
        }
    }

    private static final class ArrowRainProjectile {
        private final ServerPlayer owner;
        private final int rank;
        private final long createdTick;

        private ArrowRainProjectile(ServerPlayer owner, int rank) {
            this.owner = owner;
            this.rank = rank;
            this.createdTick = owner.level().getGameTime();
        }
    }

    private static final class GroundStunEffect {
        private final LivingEntity target;
        private final long endTick;

        private GroundStunEffect(LivingEntity target, long endTick) {
            this.target = target;
            this.endTick = endTick;
        }
    }

    private record ChallengeEffect(UUID warriorId, long endTick) {
    }

    private static final class DashFallProtection {
        private final long endTick;
        private boolean leftGround;

        private DashFallProtection(long endTick, boolean leftGround) {
            this.endTick = endTick;
            this.leftGround = leftGround;
        }

        private long endTick() {
            return endTick;
        }
    }

    private static void syncToggle(ServerPlayer player, SkillId skillId, boolean active) {
        PacketDistributor.sendToPlayer(player, new ToggleSkillStatePayload(skillId.numericId(), active));
    }

    private SkillRuntimeEffects() {
    }
}
