package net.cgerwyu.basicrpgclasses.data;

import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;

public final class ClassResourceRules {
    public static final int REGEN_STEP_TICKS = 10;
    public static final int REGEN_DELAY_TICKS = 60;

    public static boolean usesFury(RpgClass rpgClass) {
        return rpgClass == RpgClass.WARRIOR;
    }

    public static int maxResource(PlayerClassData data) {
        RpgClass rpgClass = data.rpgClass();
        int base = switch (rpgClass) {
            case MAGE -> 150;
            case PRIEST -> 165;
            case PALADIN -> 120;
            case WARRIOR, HUNTER, UNASSIGNED -> 100;
        };
        SkillId vitality = SkillDefinitions.vitalitySkillForClass(rpgClass);
        return base + SkillScaling.vitalityResourceBonus(rpgClass, data.skillRank(vitality));
    }

    public static int baseMaxResource(RpgClass rpgClass) {
        return switch (rpgClass) {
            case MAGE -> 150;
            case PRIEST -> 165;
            case PALADIN -> 120;
            default -> 100;
        };
    }

    public static int regenerationTenthsPerStep(PlayerClassData data) {
        return switch (data.rpgClass()) {
            case MAGE -> 25 + Math.round(data.skillRank(SkillId.MAGE_MANA_REGEN) * 0.5F); // base 5 + up to 1.4/s
            case HUNTER -> 50 + Math.round(data.skillRank(SkillId.HUNTER_MANA_REGEN) * 0.5F); // stamina: base 10 + up to 1.4/s
            case PRIEST -> 30 + Math.round(data.skillRank(SkillId.PRIEST_MANA_REGEN) * 0.75F);
            case PALADIN -> 20;
            case WARRIOR, UNASSIGNED -> 0;
        };
    }

    public static String nameTranslationKey(RpgClass rpgClass) {
        return switch (rpgClass) {
            case WARRIOR -> "resource.basicrpgclasses.fury";
            case HUNTER -> "resource.basicrpgclasses.stamina";
            case MAGE, PRIEST, PALADIN, UNASSIGNED -> "resource.basicrpgclasses.mana";
        };
    }

    public static int barColor(RpgClass rpgClass) {
        return usesFury(rpgClass) ? 0xFFB52A2A
                : rpgClass == RpgClass.HUNTER ? 0xFF297F9E
                : rpgClass == RpgClass.PRIEST ? 0xFFD3AD3F
                : rpgClass == RpgClass.PALADIN ? 0xFFB88A2D
                : 0xFF235A9E;
    }

    public static int barBackground(RpgClass rpgClass) {
        return usesFury(rpgClass) ? 0xFF351010 : 0xFF101A2D;
    }

    private ClassResourceRules() {
    }
}
