package net.cgerwyu.basicrpgclasses.item;

import net.cgerwyu.basicrpgclasses.data.InfiniteResourceManager;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.PlayerCombatData;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillParticleEffects;
import net.cgerwyu.basicrpgclasses.skill.SkillRuntimeEffects;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.cgerwyu.basicrpgclasses.skill.SkillVfxType;
import net.cgerwyu.basicrpgclasses.weapon.WeaponProfile;
import net.cgerwyu.basicrpgclasses.weapon.WeaponFamily;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

public final class HunterBowItem extends BowItem implements ProfiledWeapon {
    private final WeaponProfile profile;
    private final String loreId;

    public HunterBowItem(WeaponProfile profile, Properties properties) {
        this(profile, properties, null);
    }

    public HunterBowItem(WeaponProfile profile, Properties properties, String loreId) {
        super(properties);
        this.profile = profile;
        this.loreId = loreId;
    }

    @Override
    public WeaponProfile profile() {
        return profile;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (loreId != null) {
            ItemLore.append(loreId, tooltip);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (level instanceof ServerLevel
                && entity instanceof net.minecraft.server.level.ServerPlayer player
                && player.tickCount % 4 == 0) {
            if (!SkillRuntimeEffects.powerShotArmed(player)) {
                return;
            }
            int rank = player.getData(ModAttachments.PLAYER_CLASS).skillRank(SkillId.HUNTER_DRAW_SPEED);
            int rawHeld = getUseDuration(stack, entity) - remainingUseDuration;
            float readiness = Math.clamp((float) (rawHeld * SkillScaling.hunterDrawSpeedMultiplier(rank) / 20.0), 0.0F, 1.0F);
            SkillParticleEffects.attached(player.level(), SkillVfxType.BOW_CHARGE, player,
                    player.getLookAngle(), 0x3CCBFF,
                    0.45F + readiness * 0.65F, 6);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack bow, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        ItemStack projectile = player.getProjectile(bow);
        if (projectile.isEmpty()) {
            return false;
        }

        PlayerClassData classData = player.getData(ModAttachments.PLAYER_CLASS);
        boolean hunter = classData.rpgClass() == RpgClass.HUNTER;
        int drawSpeedRank = hunter ? classData.skillRank(SkillId.HUNTER_DRAW_SPEED) : 0;
        int shotPowerRank = hunter ? classData.skillRank(SkillId.HUNTER_SHOT_POWER) : 0;

        int rawTimeHeld = getUseDuration(bow, entity) - remainingTime;
        rawTimeHeld = net.neoforged.neoforge.event.EventHooks.onArrowLoose(bow, level, player, rawTimeHeld, true);
        if (rawTimeHeld < 0) {
            return false;
        }

        float drawMultiplier = switch (profile.family()) {
            case SHORTBOW -> 1.35F;
            case RECURVE_BOW -> 1.0F;
            case LONGBOW -> 0.75F;
            default -> 1.0F;
        };
        float arrowSpeed = switch (profile.family()) {
            case SHORTBOW -> 2.6F;
            case RECURVE_BOW -> 3.0F;
            case LONGBOW -> 3.4F;
            default -> 3.0F;
        };
        float uncertainty = switch (profile.family()) {
            case SHORTBOW -> 1.35F;
            case RECURVE_BOW -> 0.85F;
            case LONGBOW -> 0.45F;
            default -> 1.0F;
        };

        if (shotPowerRank > 0) {
            arrowSpeed *= (float) SkillScaling.hunterShotVelocityMultiplier(shotPowerRank);
        }

        int adjustedTimeHeld = (int) Math.round(rawTimeHeld * drawMultiplier
                * SkillScaling.hunterDrawSpeedMultiplier(drawSpeedRank));
        float power = BowItem.getPowerForTime(adjustedTimeHeld);
        if (power < 0.1F) {
            return false;
        }

        List<ItemStack> projectiles = draw(bow, projectile, player);
        if (level instanceof ServerLevel serverLevel && !projectiles.isEmpty()) {
            int multishotRank = classData.skillRank(SkillId.MULTISHOT);
            int frostRank = classData.skillRank(SkillId.FROST_ARROWS);
            boolean multishot = hunter && multishotRank > 0 && SkillRuntimeEffects.multishotActive((net.minecraft.server.level.ServerPlayer) player);
            boolean frost = hunter && frostRank > 0 && SkillRuntimeEffects.frostArrowsActive((net.minecraft.server.level.ServerPlayer) player);
            int modifierCost = (multishot ? SkillScaling.multishotManaCost(multishotRank) : 0)
                    + (frost ? SkillScaling.frostArrowManaCost(frostRank) : 0);
            net.minecraft.server.level.ServerPlayer serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
            PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
            if (!InfiniteResourceManager.active(serverPlayer) && !combat.canAfford(modifierCost)) {
                if (multishot) {
                    SkillRuntimeEffects.toggleMultishot(serverPlayer);
                }
                if (frost) {
                    SkillRuntimeEffects.toggleFrostArrows(serverPlayer);
                }
                multishot = false;
                frost = false;
                player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.shot_modifiers_no_mana"));
            } else if (!InfiniteResourceManager.active(serverPlayer) && modifierCost > 0) {
                player.setData(
                        ModAttachments.PLAYER_COMBAT,
                        combat.spendResource(modifierCost, level.getGameTime())
                );
            }

            SkillRuntimeEffects.breakCamouflage(serverPlayer);
            SkillRuntimeEffects.beginHunterShot(serverPlayer, frost, multishot);
            try {
                shoot(serverLevel, player, player.getUsedItemHand(), bow, projectiles, power * arrowSpeed, uncertainty, power == 1.0F, null);
                if (multishot) {
                    spawnMultishotExtras(
                            serverLevel,
                            serverPlayer,
                            bow,
                            projectile.copyWithCount(1),
                            power * arrowSpeed,
                            uncertainty,
                            multishotRank
                    );
                }
            } finally {
                SkillRuntimeEffects.endHunterShot(serverPlayer);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    @Override
    public int getDefaultProjectileRange() {
        return switch (profile.family()) {
            case SHORTBOW -> 12;
            case RECURVE_BOW -> 18;
            case LONGBOW -> 25;
            default -> super.getDefaultProjectileRange();
        };
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power,
                                    float uncertainty, float angle, @Nullable LivingEntity targetOverride) {
        super.shootProjectile(shooter, projectile, index, power, uncertainty, angle, targetOverride);
        if (shooter instanceof net.minecraft.server.level.ServerPlayer player && projectile instanceof AbstractArrow arrow) {
            SkillRuntimeEffects.registerHunterProjectile(player, arrow);
        }
    }

    private static void spawnMultishotExtras(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            ItemStack bow,
            ItemStack ammo,
            float speed,
            float uncertainty,
            int rank
    ) {
        int count = SkillScaling.multishotArrowCount(rank);
        Vec3 look = player.getLookAngle().normalize();
        double spread = Math.toRadians(24.0);
        for (int index = 0; index < count; index++) {
            if (index == count / 2) {
                continue; // The vanilla bow shot is the centre projectile.
            }
            double normalized = count == 1 ? 0.0 : index / (double) (count - 1) - 0.5;
            Vec3 direction = rotateAroundY(look, normalized * spread * 2.0);
            Arrow arrow = new Arrow(level, player, ammo.copyWithCount(1), bow.copy());
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            int shotPowerRank = player.getData(ModAttachments.PLAYER_CLASS).skillRank(SkillId.HUNTER_SHOT_POWER);
            arrow.setBaseDamage(SkillScaling.multishotArrowDamage(rank)
                    * SkillScaling.hunterShotDamageMultiplier(shotPowerRank));
            arrow.shoot(direction.x, direction.y, direction.z, speed, Math.min(uncertainty, 0.45F));
            SkillRuntimeEffects.registerHunterProjectile(player, arrow);
            level.addFreshEntity(arrow);
        }
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
}
