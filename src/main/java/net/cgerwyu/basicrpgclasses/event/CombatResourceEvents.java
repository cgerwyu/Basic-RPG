package net.cgerwyu.basicrpgclasses.event;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.ClassResourceRules;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.PlayerCombatData;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.data.InfiniteResourceManager;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.cgerwyu.basicrpgclasses.skill.SkillRuntimeEffects;
import net.cgerwyu.basicrpgclasses.skill.SkillParticleEffects;
import net.cgerwyu.basicrpgclasses.skill.SkillVfxType;
import net.cgerwyu.basicrpgclasses.combat.PvpBalance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BasicRPGClasses.MODID)
public final class CombatResourceEvents {
    private static final Identifier VITALITY_MODIFIER = BasicRPGClasses.id("class_vitality");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        PlayerClassData classData = player.getData(ModAttachments.PLAYER_CLASS);
        PlayerCombatData current = player.getData(ModAttachments.PLAYER_COMBAT);
        PlayerCombatData updated = current;

        if (InfiniteResourceManager.active(player)) {
            int maximum = ClassResourceRules.maxResource(classData);
            updated = updated.gainResource(maximum, maximum);
        }

        SkillRuntimeEffects.tick(player);
        if (SkillRuntimeEffects.camouflageActive(player) && player.tickCount % 5 == 0) {
            player.level().getEntitiesOfClass(
                    Monster.class,
                    player.getBoundingBox().inflate(48.0),
                    monster -> monster.getTarget() == player
            ).forEach(monster -> monster.setTarget(null));
        }
        boolean stunned = SkillRuntimeEffects.groundStunActive(player);
        if (stunned) {
            SkillRuntimeEffects.stopGliding(player);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        } else {
            boolean mobilityLocked = SkillRuntimeEffects.mobilityLocked(player);
            if (mobilityLocked) {
                SkillRuntimeEffects.stopGliding(player);
            } else {
                updated = applyMageGlide(player, classData, updated, gameTime);
                applyHunterClimbing(player, classData);
            }
        }

        if (updated.blinkSlowFallEndTick() > 0L) {
            player.removeEffect(MobEffects.SLOW_FALLING);
            updated = updated.clearBlinkSlowFall();
        }

        if (player.tickCount % ClassResourceRules.REGEN_STEP_TICKS == 0) {
            updated = updated.reconcileResourceClass(classData);
            updated = updated.regenerate(gameTime, classData);
            updated = updated.clampCooldowns(classData, gameTime);
            applyVitality(player, classData);
        }
        if (updated != current) {
            player.setData(ModAttachments.PLAYER_COMBAT, updated);
        }
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() > 0.0F) {
            if (event.getSource().getEntity() instanceof ServerPlayer attackingPlayer) {
                SkillRuntimeEffects.breakCamouflage(attackingPlayer);
            }
            if (event.getEntity() instanceof ServerPlayer damagedPlayer) {
                SkillRuntimeEffects.breakCamouflage(damagedPlayer);
                SkillRuntimeEffects.interruptMajorCasts(damagedPlayer);
            }
        }
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow
                && SkillRuntimeEffects.isFrostProjectile(arrow)
                && arrow.getOwner() instanceof ServerPlayer hunter) {
            int frostRank = hunter.getData(ModAttachments.PLAYER_CLASS).skillRank(SkillId.FROST_ARROWS);
            if (frostRank > 0) {
                event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        MobEffects.SLOWNESS,
                        PvpBalance.softControlDuration(
                                event.getEntity(), SkillScaling.frostArrowSlowTicks(frostRank), 30
                        ),
                        PvpBalance.softControlAmplifier(
                                event.getEntity(), SkillScaling.frostArrowSlowAmplifier(frostRank)
                        ),
                        false,
                        true,
                        true
                ), hunter);
                Vec3 impact = event.getEntity().position().add(0.0, event.getEntity().getBbHeight() * 0.55, 0.0);
                SkillParticleEffects.world(hunter.level(), SkillVfxType.KINETIC_BURST,
                        impact, hunter.getEyePosition(), 0x75DFFF, 0.7F, 10);
            }
        }
        if (event.getHealthDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        PlayerClassData classData = attacker.getData(ModAttachments.PLAYER_CLASS);
        if (classData.rpgClass() == RpgClass.PALADIN
                && event.getSource().getDirectEntity() == attacker
                && !SkillRuntimeEffects.divineSlashDamageInProgress(attacker)) {
            int manaStrikeRank = classData.skillRank(SkillId.PALADIN_MANA_STRIKE);
            if (manaStrikeRank > 0) {
                PlayerCombatData combat = attacker.getData(ModAttachments.PLAYER_COMBAT);
                PlayerCombatData updated = combat.gainResource(
                        Math.max(1, manaStrikeRank / 5), ClassResourceRules.maxResource(classData)
                );
                if (updated != combat) {
                    attacker.setData(ModAttachments.PLAYER_COMBAT, updated);
                }
            }
            repairPaladinWeapon(attacker, classData);
        }
        if (classData.rpgClass() != RpgClass.WARRIOR) {
            return;
        }

        int vampirismRank = classData.skillRank(SkillId.WARRIOR_VAMPIRISM);
        if (vampirismRank > 0 && event.getEntity() != attacker && attacker.isAlive()) {
            float requestedHeal = (float) (event.getHealthDamage()
                    * SkillScaling.warriorVampirismFraction(vampirismRank)
                    * PvpBalance.warriorVampirismMultiplier(event.getEntity()));
            attacker.heal(PvpBalance.capWarriorVampirismHeal(attacker, event.getEntity(), requestedHeal));
            SkillParticleEffects.bloodDrain(
                    attacker.level(),
                    event.getEntity().position().add(0.0, event.getEntity().getBbHeight() * 0.55, 0.0),
                    attacker,
                    0.48F + vampirismRank * 0.025F
            );
        }

        if (event.getSource().getDirectEntity() != attacker) {
            return;
        }

        int furyGain = 8 + Math.min(12, (int) Math.ceil(event.getHealthDamage() * 1.5F));
        PlayerCombatData current = attacker.getData(ModAttachments.PLAYER_COMBAT);
        PlayerCombatData updated = current.gainResource(furyGain, ClassResourceRules.maxResource(classData));
        if (updated != current) {
            attacker.setData(ModAttachments.PLAYER_COMBAT, updated);
        }
    }

    /** A toggled Divine Slash is fired from every right-click with the Paladin's sword. */
    @SubscribeEvent
    public static void onPaladinSwordRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ItemTags.SWORDS)) {
            return;
        }
        PlayerClassData classData = player.getData(ModAttachments.PLAYER_CLASS);
        if (classData.rpgClass() != RpgClass.PALADIN) {
            return;
        }
        int rank = classData.skillRank(SkillId.DIVINE_SLASH);
        if (rank > 0 && SkillRuntimeEffects.divineSlashActive(player)) {
            castDivineSlash(player, rank);
        }
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerClassData data = player.getData(ModAttachments.PLAYER_CLASS);
        int rank = data.rpgClass() == RpgClass.PALADIN ? data.skillRank(SkillId.PALADIN_ARMOR_TRAINING) : 0;
        if (rank <= 0) {
            return;
        }
        float retainedDamage = 1.0F - (0.20F + 0.03F * rank);
        event.getArmorMap().keySet().forEach(slot -> event.setNewDamage(
                slot, event.getNewDamage(slot) * Math.max(0.25F, retainedDamage)
        ));
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerClassData data = player.getData(ModAttachments.PLAYER_CLASS);
        int rank = data.rpgClass() == RpgClass.PALADIN ? data.skillRank(SkillId.PALADIN_ARMOR_TRAINING) : 0;
        if (rank > 0 && event.getOriginalBlock()) {
            int vanillaDamage = event.shieldDamage() >= 0
                    ? event.shieldDamage()
                    : Math.max(1, (int) Math.ceil(event.getBlockedDamage()));
            event.setShieldDamage((int) Math.floor(vanillaDamage * Math.max(0.25, 0.80 - rank * 0.03)));
        }
    }

    private static void repairPaladinWeapon(ServerPlayer player, PlayerClassData data) {
        int rank = data.skillRank(SkillId.PALADIN_ARMOR_TRAINING);
        if (rank <= 0 || player.getRandom().nextInt(Math.max(3, 20 - rank)) != 0) {
            return;
        }
        var weapon = player.getMainHandItem();
        if (weapon.isDamageableItem() && weapon.getDamageValue() > 0) {
            weapon.setDamageValue(weapon.getDamageValue() - 1);
        }
    }

    private static void castDivineSlash(ServerPlayer player, int rank) {
        PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
        int manaCost = SkillScaling.divineSlashManaCost(rank);
        if (!InfiniteResourceManager.active(player) && !combat.canAfford(manaCost)) {
            SkillRuntimeEffects.toggleDivineSlash(player);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.basicrpgclasses.divine_slash_no_mana"));
            return;
        }
        if (!InfiniteResourceManager.active(player)) {
            player.setData(ModAttachments.PLAYER_COMBAT, combat.spendResource(manaCost, player.level().getGameTime()));
        }
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.65));
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(12.0));
        SkillRuntimeEffects.beginDivineSlashDamage(player);
        try {
            for (LivingEntity target : player.level().getEntitiesOfClass(
                    LivingEntity.class, new net.minecraft.world.phys.AABB(start, end).inflate(1.15),
                    target -> target != player && target.isAlive()
                            && (target instanceof Monster || target instanceof ServerPlayer other && PvpBalance.canDamagePlayer(player, other))
            )) {
                Vec3 offset = target.getEyePosition().subtract(start);
                double projection = offset.dot(direction);
                if (projection >= 0.0 && projection <= 12.0
                        && offset.subtract(direction.scale(projection)).length() <= 1.15) {
                    target.hurtServer(player.level(), player.damageSources().playerAttack(player), SkillScaling.divineSlashDamage(rank));
                }
            }
        } finally {
            SkillRuntimeEffects.finishDivineSlashDamage(player);
        }
        SkillParticleEffects.divineSlashWave(player.level(), start, end, 1.05F);
    }

    @SubscribeEvent
    public static void onArrowJoined(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        SkillRuntimeEffects.registerHunterProjectile(player, arrow);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            boolean powerProjectile = SkillRuntimeEffects.isPowerProjectile(arrow);
            if (powerProjectile && event.getRayTraceResult().getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                event.setCanceled(true);
                return;
            }
            if (event.getRayTraceResult().getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                SkillRuntimeEffects.triggerPowerShotImpact(arrow, event.getRayTraceResult().getLocation());
            }
            SkillRuntimeEffects.triggerArrowRainImpact(arrow, event.getRayTraceResult().getLocation());
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer target
                && target.getData(ModAttachments.PLAYER_CLASS).rpgClass() == RpgClass.HUNTER
                && SkillRuntimeEffects.camouflageActive(target)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Monster monster && monster.getTarget() == target) {
                monster.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && SkillRuntimeEffects.groundStunActive(attacker)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow
                && SkillRuntimeEffects.rejectDuplicateHunterHit(arrow, event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerPlayer pvpAttacker = event.getSource().getEntity() instanceof ServerPlayer attacker
                ? attacker : null;
        if (pvpAttacker != null && PvpBalance.denyAttack(pvpAttacker, player)) {
            event.setCanceled(true);
            return;
        }

        if (SkillRuntimeEffects.windrunActive(player)
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker) {
            if (pvpAttacker == null) {
                event.setCanceled(true);
            } else {
                event.setAmount(event.getAmount() * 0.80F);
            }
            SkillParticleEffects.attached(player.level(), SkillVfxType.HUNTER_AFTERIMAGE, player,
                    player.getDeltaMovement(),
                    0xD9FFFF, 0.65F, 8);
            if (event.isCanceled()) {
                return;
            }
        }
        PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
        if (combat.magicShieldActive(player.level().getGameTime())) {
            event.setCanceled(true);

            SkillParticleEffects.attached(player.level(), SkillVfxType.BURST, player, Vec3.ZERO,
                    0x5EBBFF, 0.72F, 8);
            if (player.tickCount % 4 == 0) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.75F, 1.5F);
            }
            return;
        }

        if (pvpAttacker != null) {
            float challengedAmount = event.getAmount()
                    * SkillRuntimeEffects.challengeDamageMultiplier(pvpAttacker, player);
            event.setAmount(PvpBalance.adjustDamage(pvpAttacker, player, challengedAmount));
            PvpBalance.markCombat(pvpAttacker, player);
            SkillRuntimeEffects.breakCamouflage(pvpAttacker);
            SkillRuntimeEffects.breakCamouflage(player);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (SkillRuntimeEffects.consumeDashFallProtection(player)) {
            event.setCanceled(true);
            player.resetFallDistance();
            return;
        }

        PlayerClassData classData = player.getData(ModAttachments.PLAYER_CLASS);
        int rank = classData.skillRank(SkillId.HUNTER_FALL_TRAINING);
        if (classData.rpgClass() == RpgClass.HUNTER && rank > 0) {
            event.setDamageMultiplier((float) (
                    event.getDamageMultiplier() * (1.0 - SkillScaling.hunterFallDamageReduction(rank))
            ));
        }
    }

    private static void applyVitality(ServerPlayer player, PlayerClassData classData) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        SkillId vitalitySkill = SkillDefinitions.vitalitySkillForClass(classData.rpgClass());
        double bonus = SkillScaling.vitalityHealthBonus(classData.rpgClass(), classData.skillRank(vitalitySkill));
        if (bonus <= 0.0) {
            maxHealth.removeModifier(VITALITY_MODIFIER);
        } else {
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                    VITALITY_MODIFIER,
                    bonus,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static PlayerCombatData applyMageGlide(
            ServerPlayer player,
            PlayerClassData classData,
            PlayerCombatData combat,
            long gameTime
    ) {
        int rank = classData.skillRank(SkillId.MAGE_GLIDE);
        int manaPerSecond = SkillScaling.mageGlideManaPerSecond(rank);
        var input = player.getLastClientInput();
        boolean hasGlide = classData.rpgClass() == RpgClass.MAGE && rank > 0;
        boolean glideInputActive = hasGlide && SkillRuntimeEffects.updateGlideInput(player, input.jump());
        boolean correctState = hasGlide
                && !input.shift()
                && !player.onGround()
                && !player.getAbilities().flying
                && !player.isInWater()
                && !player.isPassenger();
        boolean wantsToGlide = correctState && glideInputActive;
        boolean infinite = InfiniteResourceManager.active(player);
        if (!wantsToGlide || !infinite && !combat.canAfford(manaPerSecond)) {
            SkillRuntimeEffects.stopGliding(player);
            return combat;
        }

        PlayerCombatData updated = combat;
        if (!infinite && SkillRuntimeEffects.shouldDrainGlideMana(player, gameTime)) {
            updated = updated.spendResource(manaPerSecond, gameTime);
        }

        Vec3 movement = player.getDeltaMovement();
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() > 1.0E-5) {
            forward = forward.normalize();
        }
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 desired = Vec3.ZERO;
        if (input.forward()) {
            desired = desired.add(forward);
        }
        if (input.backward()) {
            desired = desired.subtract(forward);
        }
        if (input.right()) {
            desired = desired.add(right);
        }
        if (input.left()) {
            desired = desired.subtract(right);
        }
        Vec3 horizontal = SkillRuntimeEffects.steerGlide(
                player,
                desired,
                SkillScaling.mageGlideAirControlSpeed(rank)
        );
        player.setDeltaMovement(
                horizontal.x,
                Math.max(movement.y, SkillScaling.mageGlideDescentSpeed(rank)),
                horizontal.z
        );
        player.hurtMarked = true;
        player.resetFallDistance();
        if (player.tickCount % 3 == 0) {
            SkillParticleEffects.mageGlideVapor(player, gameTime);
        }
        return updated;
    }

    private static void applyHunterClimbing(ServerPlayer player, PlayerClassData classData) {
        int rank = classData.skillRank(SkillId.HUNTER_CLIMBING);
        var input = player.getLastClientInput();
        if (classData.rpgClass() != RpgClass.HUNTER
                || rank <= 0
                || PvpBalance.inCombat(player)
                || !hasClimbableWall(player)
                || !input.jump()
                || input.shift()) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.max(movement.y, SkillScaling.hunterClimbSpeed(rank)), movement.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        if (player.tickCount % 20 == 0) {
            SkillParticleEffects.attached(player.level(), SkillVfxType.WIND_TRAIL, player,
                    new Vec3(0.0, 1.0, 0.0), 0x7BDFFF, 0.46F, 24);
        }
    }

    private static boolean hasClimbableWall(ServerPlayer player) {
        double reach = player.getBbWidth() * 0.5 + 0.30;
        double[][] offsets = {
                {reach, 0.0}, {-reach, 0.0}, {0.0, reach}, {0.0, -reach}
        };
        double[] heights = {0.25, Math.min(1.25, player.getBbHeight() * 0.7)};
        for (double height : heights) {
            for (double[] offset : offsets) {
                BlockPos position = BlockPos.containing(
                        player.getX() + offset[0],
                        player.getY() + height,
                        player.getZ() + offset[1]
                );
                var state = player.level().getBlockState(position);
                if (!state.getCollisionShape(player.level(), position).isEmpty()) {
                    return true;
                }
            }
        }
        return player.horizontalCollision;
    }

    private CombatResourceEvents() {
    }
}
