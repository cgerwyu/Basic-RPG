package net.cgerwyu.basicrpgclasses.skill;

import net.cgerwyu.basicrpgclasses.data.RpgClass;

public final class SkillScaling {
    public static float whirlwindDamage(int rank) {
        return 5.0F + 2.5F * (rank - 1) / 14.0F;
    }

    public static double whirlwindRadius(int rank) {
        return 3.0 + 0.1 * ((rank - 1) / 2);
    }

    public static int fortifyDurationTicks(int rank) {
        return (int) Math.round((55.0 + 3.0 * (rank - 1) / 14.0) * 20.0);
    }

    public static int fortifyResistanceAmplifier(int rank) {
        return 0;
    }

    public static int fortifyAbsorptionAmplifier(int rank) {
        return Math.min(2, (rank - 1) / 6);
    }

    public static double fortifyRadius(int rank) {
        return 4.0 + 2.0 * (rank - 1) / 14.0;
    }

    public static double provokeRadius(int rank) {
        return 6.0 + 0.5 * (rank - 1);
    }

    public static int provokeDurationTicks(int rank) {
        return (6 + (rank - 1) / 2) * 20;
    }

    public static float fireballDamage(int rank) {
        return 6.0F + 3.0F * (rank - 1) / 14.0F;
    }

    public static float fireballBurnSeconds(int rank) {
        return 2.0F + 1.0F * (rank - 1) / 14.0F;
    }

    public static int fireballAreaRadius(int rank) {
        return rank >= 8 ? 2 : 1;
    }

    public static int fireballVolleyCount(int rank) {
        return 1;
    }

    public static float fireballVisualScale(int rank) {
        return 1.15F + 0.085F * (rank - 1);
    }

    public static float healAmount(int rank) {
        return 5.0F + 3.0F * (rank - 1) / 14.0F;
    }

    public static float holyBoltDamage(int rank) {
        return 7.0F + 5.0F * (rank - 1) / 14.0F;
    }

    public static float solarBeamDamage(int rank) {
        return 1.5F + 1.8F * (rank - 1) / 14.0F;
    }

    public static float solarBeamHeal(int rank) {
        return 1.2F + 1.4F * (rank - 1) / 14.0F;
    }

    public static double healRadius(int rank) {
        return 5.0 + 0.15 * (rank - 1);
    }

    public static double blinkDistance(int rank) {
        return 6.0 + 0.4 * (rank - 1);
    }

    public static int blinkProtectionTicks(int rank) {
        return 6 + rank / 3;
    }

    public static double mageGlideDescentSpeed(int rank) {
        return -0.18 + 0.005 * (rank - 1);
    }

    public static double mageGlideAirControlSpeed(int rank) {
        return 0.12 + 0.008 * (rank - 1);
    }

    public static int mageGlideManaPerSecond(int rank) {
        return rank >= 15 ? 2 : rank >= 8 ? 3 : 4;
    }

    public static double mageManaRegenerationBonusPerSecond(int rank) {
        return 0.1 * rank;
    }

    public static double hunterManaRegenerationBonusPerSecond(int rank) {
        return 0.1 * rank;
    }

    public static double hunterDrawSpeedMultiplier(int rank) {
        double progress = Math.clamp(rank, 0, SkillDefinitions.MAX_SKILL_RANK)
                / (double) SkillDefinitions.MAX_SKILL_RANK;
        return 1.0 + 0.8 * progress;
    }

    public static double hunterShotDamageMultiplier(int rank) {
        return 1.0 + 0.2 * Math.clamp(rank, 0, SkillDefinitions.MAX_SKILL_RANK)
                / SkillDefinitions.MAX_SKILL_RANK;
    }

    public static double hunterShotVelocityMultiplier(int rank) {
        return 1.0 + 0.14 * Math.clamp(rank, 0, SkillDefinitions.MAX_SKILL_RANK)
                / SkillDefinitions.MAX_SKILL_RANK;
    }

    public static double warriorVampirismFraction(int rank) {
        return 0.03 + 0.09 * (rank - 1) / 14.0;
    }

    public static int magicShieldDurationTicks() {
        return 24;
    }

    public static double magicShieldCleanseRadius(int rank) {
        return 5.0 + 2.0 * (rank - 1) / 14.0;
    }

    public static double dashSpeed(int rank) {
        return 1.0 + 0.025 * (rank - 1);
    }

    public static int windrunDurationTicks(int rank) {
        return (int) Math.round((3.0 + 1.5 * (rank - 1) / 14.0) * 20.0);
    }

    public static int windrunSpeedAmplifier(int rank) {
        return 0;
    }

    public static int camouflageDurationTicks(int rank) {
        return (int) Math.round((3.0 + 2.0 * (rank - 1) / 14.0) * 20.0);
    }

    public static double vitalityHealthBonus(RpgClass rpgClass, int rank) {
        return switch (rpgClass) {
            case WARRIOR -> 0.25 * rank;
            case MAGE -> 0.12 * rank;
            case HUNTER -> 0.16 * rank;
            case PRIEST -> 0.14 * rank;
            case PALADIN -> 0.65 * rank;
            case UNASSIGNED -> 0.0;
        };
    }

    public static int vitalityResourceBonus(RpgClass rpgClass, int rank) {
        return switch (rpgClass) {
            case MAGE -> 2 * rank;
            case WARRIOR -> 2 * rank;
            case HUNTER -> rank;
            case PRIEST -> 2 * rank;
            case PALADIN -> 2 * rank;
            case UNASSIGNED -> 0;
        };
    }

    public static double groundStunRadius(int rank) {
        return 3.0 + 1.0 * (rank - 1) / 14.0;
    }

    public static double groundStunRange(int rank) {
        return 6.0 + 2.0 * (rank - 1) / 14.0;
    }

    public static int groundStunDurationTicks(int rank) {
        return (int) Math.round((1.8 + 1.2 * (rank - 1) / 14.0) * 20.0);
    }

    public static float groundStunDamage(int rank) {
        return 3.0F + 1.5F * (rank - 1) / 14.0F;
    }

    public static double hunterFallDamageReduction(int rank) {
        return 0.20 + 0.55 * (rank - 1) / 14.0;
    }

    public static double hunterClimbSpeed(int rank) {
        return 0.14 + 0.006 * (rank - 1);
    }

    public static float frostNovaDamage(int rank) {
        return 3.0F + 2.0F * (rank - 1) / 14.0F;
    }

    public static double frostNovaRadius(int rank) {
        return 3.5 + 1.0 * (rank - 1) / 14.0;
    }

    public static int frostNovaSlowTicks(int rank) {
        return (int) Math.round((2.5 + 1.5 * (rank - 1) / 14.0) * 20.0);
    }

    public static int frostNovaSlowAmplifier(int rank) {
        return rank >= 9 ? 1 : 0;
    }

    public static float chainLightningDamage(int rank) {
        return 3.0F + 1.5F * (rank - 1) / 14.0F;
    }

    public static int chainLightningTargets(int rank) {
        return 5 + (rank - 1) / 3;
    }

    public static double chainLightningJumpRange(int rank) {
        return 6.0 + 2.0 * (rank - 1) / 14.0;
    }

    public static double chainLightningFalloff(int rank) {
        return 0.76 + 0.10 * (rank - 1) / 14.0;
    }

    public static float skyRaysDamage(int rank) {
        return 3.0F + 2.0F * (rank - 1) / 14.0F;
    }

    public static int skyRaysTargets(int rank) {
        return 3 + (rank - 1) / 7;
    }

    public static double skyRaysRadius(int rank) {
        return 12.0 + 4.0 * (rank - 1) / 14.0;
    }

    public static float meteorDamage(int rank) {
        return 20.0F + 12.0F * (rank - 1) / 14.0F;
    }

    public static double meteorRadius(int rank) {
        double progress = (rank - 1) / 14.0;
        return 4.5 + 2.5 * progress;
    }

    public static double meteorCastRange(int rank) {
        return 24.0 + 40.0 * (rank - 1) / 14.0;
    }

    public static double meteorSizeMultiplier(int rank) {
        return 1.0 + (rank - 1) / 14.0;
    }

    public static int multishotArrowCount(int rank) {
        return rank >= 11 ? 7 : rank >= 6 ? 5 : 3;
    }

    public static double multishotArrowDamage(int rank) {
        return 2.2 + 0.8 * (rank - 1) / 14.0;
    }

    public static int multishotManaCost(int rank) {
        return rank >= 12 ? 8 : rank >= 6 ? 10 : 12;
    }

    public static int arrowRainArrowCount(int rank) {
        return 12 + (int) Math.round(20.0 * (rank - 1) / 14.0);
    }

    public static double arrowRainRadius(int rank) {
        return 5.0 + 3.0 * (rank - 1) / 14.0;
    }

    public static double arrowRainArrowDamage(int rank) {
        return 3.0 + 1.5 * (rank - 1) / 14.0;
    }

    public static float powerShotDamage(int rank) {
        return 14.0F + 8.0F * (rank - 1) / 14.0F;
    }

    public static double powerShotRange(int rank) {
        return 25.0 + 10.0 * (rank - 1) / 14.0;
    }

    public static double powerShotWidth(int rank) {
        return 2.4 + 1.6 * (rank - 1) / 14.0;
    }

    public static int frostArrowManaCost(int rank) {
        return rank >= 12 ? 2 : rank >= 6 ? 3 : 4;
    }

    public static int frostArrowSlowTicks(int rank) {
        return (int) Math.round((1.5 + 1.5 * (rank - 1) / 14.0) * 20.0);
    }

    public static int frostArrowSlowAmplifier(int rank) {
        return rank >= 9 ? 1 : 0;
    }

    public static float shieldBashDamage(int rank) {
        return 4.0F + 3.0F * (rank - 1) / 14.0F;
    }

    public static double shieldBashRange(int rank) {
        return 5.0 + 2.0 * (rank - 1) / 14.0;
    }

    public static int shieldBashStunTicks(int rank) {
        return (int) Math.round((1.5 + 1.0 * (rank - 1) / 14.0) * 20.0);
    }

    public static int battleCryDurationTicks(int rank) {
        return (int) Math.round((5.0 + 3.0 * (rank - 1) / 14.0) * 20.0);
    }

    public static int battleCryStrengthAmplifier(int rank) {
        return 0;
    }

    public static int battleCryHasteAmplifier(int rank) {
        return rank >= 10 ? 1 : 0;
    }

    public static float restorationHealPerPulse(int rank) {
        return 1.3F + 1.2F * (rank - 1) / 14.0F;
    }

    public static int restorationCastTicks(int rank) {
        return 60 - Math.min(14, rank - 1);
    }

    public static float healingHaloAmount(int rank) {
        return 7.0F + 5.0F * (rank - 1) / 14.0F;
    }

    public static double holyRadius(int rank) {
        return 5.0 + 2.0 * (rank - 1) / 14.0;
    }

    public static int holyShieldAbsorptionAmplifier(int rank) {
        return rank >= 11 ? 2 : rank >= 5 ? 1 : 0;
    }

    public static int priestBuffTicks(int rank) {
        return (55 + (rank - 1) / 5) * 20;
    }

    public static int holyStormCastTicks(int rank) {
        return 80 - Math.min(20, rank - 1);
    }

    public static float holyStormDamage(int rank) {
        return 2.4F + 1.8F * (rank - 1) / 14.0F;
    }

    public static float paladinHealAmount(int rank) {
        return 3.0F + 3.0F * (rank - 1) / 14.0F;
    }

    public static int paladinBuffTicks(int rank) {
        return (55 + (rank - 1) / 5) * 20;
    }

    public static int berserkDurationTicks(int rank) {
        return (5 + (rank - 1) / 3) * 20;
    }

    public static float executionDamage(int rank) {
        return 12.0F + 10.0F * (rank - 1) / 14.0F;
    }

    public static float ultraThrustDamage(int rank) {
        return 30.0F + 30.0F * (rank - 1) / 14.0F;
    }

    public static float warriorLeapDamage(int rank) {
        return 9.0F + 8.0F * (rank - 1) / 14.0F;
    }

    public static double warriorLeapRange(int rank) {
        return 10.0 + 8.0 * (rank - 1) / 14.0;
    }

    /** Four seconds at the first rank, three seconds at the maximum rank. */
    public static int ultraThrustCastTicks(int rank) {
        return 80 - Math.round(20.0F * (rank - 1) / 14.0F);
    }

    public static int divineSlashManaCost(int rank) {
        return rank >= 11 ? 3 : rank >= 6 ? 4 : 5;
    }

    public static float divineSlashDamage(int rank) {
        return 4.0F + 4.0F * (rank - 1) / 14.0F;
    }

    private SkillScaling() {
    }
}
