package net.cgerwyu.basicrpgclasses.skill.entity;

import net.cgerwyu.basicrpgclasses.registry.ModEntities;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.cgerwyu.basicrpgclasses.skill.SkillParticleEffects;
import net.cgerwyu.basicrpgclasses.combat.PvpBalance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class MageFireball extends SmallFireball {
    private static final double FLIGHT_SPEED = 2.5;
    private static final EntityDataAccessor<Integer> DATA_SKILL_RANK =
            SynchedEntityData.defineId(MageFireball.class, EntityDataSerializers.INT);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MageFireball(EntityType<? extends MageFireball> type, Level level) {
        super((EntityType) type, level);
        this.accelerationPower = 0.0;
    }

    public MageFireball(ServerLevel level, ServerPlayer owner, int rank) {
        this(level, owner, rank, owner.getLookAngle());
    }

    public MageFireball(ServerLevel level, ServerPlayer owner, int rank, Vec3 direction) {
        this(ModEntities.MAGE_FIREBALL.get(), level);
        this.entityData.set(DATA_SKILL_RANK, Math.clamp(rank, 1, 15));
        this.setOwner(owner);
        Vec3 look = direction.normalize();
        Vec3 start = owner.getEyePosition().add(look.scale(0.8));
        this.setPos(start);
        this.setDeltaMovement(look.scale(FLIGHT_SPEED));
        this.setRot(owner.getYRot(), owner.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKILL_RANK, 1);
    }

    public int skillRank() {
        return entityData.get(DATA_SKILL_RANK);
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected float getLiquidInertia() {
        return 1.0F;
    }

    @Override
    public void tick() {
        Vec3 beforeTick = getDeltaMovement();
        if (beforeTick.lengthSqr() > 1.0E-5) {
            setDeltaMovement(beforeTick.normalize().scale(FLIGHT_SPEED));
        }
        super.tick();
        Vec3 afterTick = getDeltaMovement();
        if (!isRemoved() && afterTick.lengthSqr() > 1.0E-5) {
            setDeltaMovement(afterTick.normalize().scale(FLIGHT_SPEED));
        }
        if (!level().isClientSide() || isRemoved()) {
            return;
        }
        Vec3 movement = getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-5) {
            return;
        }
        Vec3 direction = movement.normalize();
        Vec3 side = direction.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-5 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = direction.cross(side).normalize();
        double visualScale = SkillScaling.fireballVisualScale(skillRank());
        for (int index = 0; index < 7; index++) {
            double angle = tickCount * 0.72 + index * 2.399963229728653;
            double radius = visualScale * (0.10 + (index % 3) * 0.045);
            double tail = visualScale * index * 0.13;
            Vec3 point = position().subtract(direction.scale(tail))
                    .add(side.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            level().addParticle(index % 3 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                    point.x, point.y, point.z,
                    -direction.x * 0.035, -direction.y * 0.035 + 0.012, -direction.z * 0.035);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) {
            return false;
        }
        Entity owner = getOwner();
        if (entity instanceof ServerPlayer target && owner instanceof ServerPlayer caster) {
            return target.isAlive() && PvpBalance.canDamagePlayer(caster, target);
        }
        return entity instanceof Monster monster && monster.isAlive();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !(hitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        DamageSource source = damageSources().fireball(this, getOwner());
        if (target.hurtServer(serverLevel, source, SkillScaling.fireballDamage(skillRank()))) {
            target.igniteForSeconds(SkillScaling.fireballBurnSeconds(skillRank()));
        }
        SkillParticleEffects.burst(serverLevel, position(), 0xFF5A18,
                SkillScaling.fireballVisualScale(skillRank()), 12);
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        SkillParticleEffects.burst(serverLevel, hitResult.getLocation(), 0xFF5A18,
                SkillScaling.fireballVisualScale(skillRank()), 12);
        SkillParticleEffects.areaRing(serverLevel, hitResult.getLocation(), 0xFF6B24,
                SkillScaling.fireballAreaRadius(skillRank()) + 0.8F, 18);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("skill_rank", skillRank());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.accelerationPower = 0.0;
        entityData.set(DATA_SKILL_RANK, Math.clamp(input.getIntOr("skill_rank", 1), 1, 15));
    }
}
