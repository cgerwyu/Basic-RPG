package net.cgerwyu.basicrpgclasses.data;

public final class ClassChangeRules {
    public static final int REFUND_PERCENT = 50;

    public static int totalTrackedCost(PlayerClassData data) {
        if (data.spentMinecraftLevels() > 0 || data.earnedSkillPoints() == 0) {
            return data.spentMinecraftLevels();
        }

        // Compatibility for worlds saved before exact level spending was tracked.
        return SkillPointCosts.totalCostForPoints(data.earnedSkillPoints());
    }

    public static int refundLevels(PlayerClassData data) {
        return totalTrackedCost(data) * REFUND_PERCENT / 100;
    }

    private ClassChangeRules() {
    }
}
