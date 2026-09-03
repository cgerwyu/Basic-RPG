package net.cgerwyu.basicrpgclasses.skill;

import net.cgerwyu.basicrpgclasses.data.RpgClass;

public record SkillDefinition(
        SkillId id,
        RpgClass ownerClass,
        int maxRank,
        int[] manaCostsByRank,
        int[] cooldownTicksByRank,
        int[] chargesByRank,
        SkillId prerequisite,
        int prerequisiteRank,
        int[] requiredClassLevelsByRank,
        int color
) {
    public SkillDefinition {
        maxRank = Math.max(1, maxRank);
        manaCostsByRank = manaCostsByRank.clone();
        cooldownTicksByRank = cooldownTicksByRank.clone();
        chargesByRank = chargesByRank.clone();
        requiredClassLevelsByRank = requiredClassLevelsByRank.clone();
        if (manaCostsByRank.length != maxRank
                || cooldownTicksByRank.length != maxRank
                || chargesByRank.length != maxRank
                || requiredClassLevelsByRank.length != maxRank) {
            throw new IllegalArgumentException("Every skill scaling array must contain maxRank entries");
        }
        prerequisite = prerequisite == null ? SkillId.NONE : prerequisite;
        prerequisiteRank = prerequisite == SkillId.NONE ? 0 : Math.max(1, prerequisiteRank);
        for (int index = 0; index < requiredClassLevelsByRank.length; index++) {
            requiredClassLevelsByRank[index] = Math.max(
                    index == 0 ? 0 : requiredClassLevelsByRank[index - 1],
                    requiredClassLevelsByRank[index]
            );
        }
    }

    @Override
    public int[] manaCostsByRank() {
        return manaCostsByRank.clone();
    }

    @Override
    public int[] cooldownTicksByRank() {
        return cooldownTicksByRank.clone();
    }

    @Override
    public int[] chargesByRank() {
        return chargesByRank.clone();
    }

    @Override
    public int[] requiredClassLevelsByRank() {
        return requiredClassLevelsByRank.clone();
    }

    public int requiredClassLevel() {
        return requiredClassLevelForRank(1);
    }

    public int requiredClassLevelForRank(int rank) {
        return requiredClassLevelsByRank[index(rank)];
    }

    public int manaCost(int rank) {
        return Math.max(0, manaCostsByRank[index(rank)]);
    }

    public int cooldownTicks(int rank) {
        return Math.max(1, cooldownTicksByRank[index(rank)]);
    }

    public int maxCharges(int rank) {
        return Math.max(1, chargesByRank[index(rank)]);
    }

    public boolean hasPrerequisite() {
        return prerequisite != SkillId.NONE;
    }

    private int index(int rank) {
        return Math.clamp(rank, 1, maxRank) - 1;
    }
}
