package net.cgerwyu.basicrpgclasses.data;

public final class SkillPointCosts {
    public static int nextPointCost(PlayerClassData data) {
        return 1 + Math.max(0, data.earnedSkillPoints());
    }

    public static int totalCostForPoints(int pointCount) {
        int points = Math.max(0, pointCount);
        return points * (points + 1) / 2;
    }

    private SkillPointCosts() {
    }
}
