package net.cgerwyu.basicrpgclasses.skill;

import net.cgerwyu.basicrpgclasses.data.RpgClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

public final class SkillDefinitions {
    public static final int MAX_SKILL_RANK = 15;
    private static final Map<SkillId, SkillDefinition> DEFINITIONS = new EnumMap<>(SkillId.class);

    static {
        register(skill(SkillId.WHIRLWIND, RpgClass.PALADIN, 24, 18, 10.0, 7.0,
                rank -> 1, SkillId.NONE, 0, 0, 0xFFF2F5F8));
        register(skill(SkillId.GROUND_STUN, RpgClass.PALADIN, 34, 26, 18.0, 13.0,
                rank -> 1, SkillId.WHIRLWIND, 3, 8, 0xFFFFD36A));
        register(skill(SkillId.BATTLE_CRY, RpgClass.WARRIOR, 52, 40, 25.0, 18.0,
                rank -> 1, SkillId.NONE, 0, 0, 0xFFE05242));
        register(skill(SkillId.BERSERK, RpgClass.WARRIOR, 48, 38, 24.0, 17.0,
                rank -> 1, SkillId.BATTLE_CRY, 4, 7, 0xFFFF4A32));
        register(skill(SkillId.EXECUTION, RpgClass.WARRIOR, 72, 56, 20.0, 14.0,
                rank -> 1, SkillId.BERSERK, 6, 12, 0xFFB7192D));
        register(skill(SkillId.ULTRA_THRUST, RpgClass.WARRIOR, 100, 100, 60.0, 60.0,
                rank -> 1, SkillId.BERSERK, 8, 18, 0xFFB00020));
        register(skill(SkillId.WARRIOR_LEAP, RpgClass.WARRIOR, 58, 44, 18.0, 12.0,
                rank -> 1, SkillId.BATTLE_CRY, 3, 7, 0xFFDA5B4B));
        register(skill(SkillId.WARRIOR_WHIRLWIND, RpgClass.WARRIOR, 30, 22, 10.0, 7.0,
                rank -> 1, SkillId.BATTLE_CRY, 2, 5, 0xFFFF6A52));
        register(passive(SkillId.WARRIOR_VITALITY, RpgClass.WARRIOR, 0xFFD65C45));
        register(passive(SkillId.WARRIOR_VAMPIRISM, RpgClass.WARRIOR, 6, 0xFFB93B52));

        register(skill(SkillId.FIREBALL, RpgClass.MAGE, 30, 24, 5.0, 4.0,
                rank -> 1, SkillId.NONE, 0, 0, 0xFFE85A3F));
        register(skill(SkillId.BLINK, RpgClass.MAGE, 38, 32, 11.0, 9.0,
                rank -> 1, SkillId.NONE, 0, 4, 0xFF7E78E8));
        register(skill(SkillId.MAGIC_SHIELD, RpgClass.MAGE, 60, 48, 30.0, 24.0,
                rank -> 1, SkillId.NONE, 0, 7, 0xFF6F8CFF));
        register(skill(SkillId.FROST_NOVA, RpgClass.MAGE, 40, 32, 14.0, 10.0,
                rank -> 1, SkillId.NONE, 0, 5, 0xFF76C8FF));
        register(skill(SkillId.CHAIN_LIGHTNING, RpgClass.MAGE, 34, 26, 9.0, 6.5,
                rank -> 1, SkillId.FROST_NOVA, 3, 7, 0xFF58B8FF));
        register(skill(SkillId.METEOR, RpgClass.MAGE, 90, 75, 50.0, 40.0,
                rank -> 1, SkillId.FIREBALL, 10, 14, 0xFFFF784F));
        register(passive(SkillId.MAGE_VITALITY, RpgClass.MAGE, 0xFF6B9DFF));
        register(passive(SkillId.MAGE_GLIDE, RpgClass.MAGE, 2, 0xFFA78CFF));
        register(passive(SkillId.MAGE_MANA_REGEN, RpgClass.MAGE, 0xFF4EBCFF));

        register(skill(SkillId.DASH, RpgClass.HUNTER, 24, 20, 10.0, 8.0,
                rank -> 1, SkillId.NONE, 0, 0, 0xFF58B8D8));
        register(skill(SkillId.WINDRUN, RpgClass.HUNTER, 30, 24, 24.0, 18.0,
                rank -> 1, SkillId.DASH, 3, 4, 0xFF49CFFF));
        register(skill(SkillId.CAMOUFLAGE, RpgClass.HUNTER, 36, 30, 30.0, 24.0,
                rank -> 1, SkillId.WINDRUN, 5, 10, 0xFF55BFE8));
        register(skill(SkillId.MULTISHOT, RpgClass.HUNTER, 0, 0, 0.5, 0.5,
                rank -> 1, SkillId.NONE, 0, 2, 0xFF48CFFF));
        register(skill(SkillId.FROST_ARROWS, RpgClass.HUNTER, 0, 0, 0.5, 0.5,
                rank -> 1, SkillId.NONE, 0, 4, 0xFF70D8FF));
        register(skill(SkillId.ARROW_RAIN, RpgClass.HUNTER, 44, 36, 22.0, 16.0,
                rank -> 1, SkillId.MULTISHOT, 5, 8, 0xFF8FE8FF));
        register(skill(SkillId.POWER_SHOT, RpgClass.HUNTER, 40, 32, 18.0, 14.0,
                rank -> 1, SkillId.MULTISHOT, 8, 12, 0xFF5BBCE8));
        register(passive(SkillId.HUNTER_VITALITY, RpgClass.HUNTER, 0xFF78B86A));
        register(passive(SkillId.HUNTER_FALL_TRAINING, RpgClass.HUNTER, 0xFFD4C16A));
        register(passive(SkillId.HUNTER_CLIMBING, RpgClass.HUNTER, 3, 0xFF8BA56A));
        register(passive(SkillId.HUNTER_MANA_REGEN, RpgClass.HUNTER, 2, 0xFF55C8B6));
        register(passive(SkillId.HUNTER_DRAW_SPEED, RpgClass.HUNTER, 2, 0xFF68D8FF));
        register(passive(SkillId.HUNTER_SHOT_POWER, RpgClass.HUNTER, 4, 0xFF4DB9F2));

        register(skill(SkillId.HEAL, RpgClass.PRIEST, 16, 10, 4.0, 2.8,
                rank -> 1, SkillId.NONE, 0, 0, 0xFFFFF2A8));
        register(skill(SkillId.RESTORATION, RpgClass.PRIEST, 32, 24, 12.0, 8.0,
                rank -> 1, SkillId.HEAL, 3, 3, 0xFFFFD966));
        register(skill(SkillId.HEALING_HALO, RpgClass.PRIEST, 42, 32, 18.0, 12.0,
                rank -> 1, SkillId.HEAL, 5, 6, 0xFFFFFFCC));
        register(skill(SkillId.CLEANSE, RpgClass.PRIEST, 28, 20, 14.0, 9.0,
                rank -> 1, SkillId.HEAL, 3, 5, 0xFFFFF8D8));
        register(skill(SkillId.BLESSING, RpgClass.PRIEST, 46, 34, 60.0, 55.0,
                rank -> 1, SkillId.HEALING_HALO, 3, 9, 0xFFFFE38A));
        register(skill(SkillId.HOLY_BOLT, RpgClass.PRIEST, 22, 16, 4.5, 3.2,
                rank -> 1, SkillId.NONE, 0, 0, 0xFFFFD36A));
        register(skill(SkillId.SOLAR_BEAM, RpgClass.PRIEST, 10, 8, 1.0, 1.0,
                rank -> 1, SkillId.HOLY_BOLT, 3, 7, 0xFFFFE58A));
        register(skill(SkillId.SKY_RAYS, RpgClass.PRIEST, 70, 55, 45.0, 36.0,
                rank -> 1, SkillId.HOLY_BOLT, 5, 11, 0xFFFFD36A));
        register(skill(SkillId.RESURRECTION, RpgClass.PRIEST, 85, 65, 60.0, 42.0,
                rank -> 1, SkillId.RESTORATION, 8, 13, 0xFFFFFFFF));
        register(skill(SkillId.HOLY_STORM, RpgClass.PRIEST, 80, 62, 48.0, 34.0,
                rank -> 1, SkillId.SKY_RAYS, 6, 14, 0xFFFFF4B8));
        register(passive(SkillId.PRIEST_VITALITY, RpgClass.PRIEST, 0xFFFFE6A0));
        register(passive(SkillId.PRIEST_MANA_REGEN, RpgClass.PRIEST, 2, 0xFFFFFFB8));

        register(skill(SkillId.FORTIFY, RpgClass.PALADIN, 24, 18, 60.0, 55.0,
                rank -> 1, SkillId.NONE, 0, 0, 0xFFD9B56D));
        register(skill(SkillId.PROVOKE, RpgClass.PALADIN, 18, 12, 16.0, 11.0,
                rank -> 1, SkillId.FORTIFY, 4, 4, 0xFFE97C52));
        register(skill(SkillId.SHIELD_BASH, RpgClass.WARRIOR, 42, 32, 13.0, 9.0,
                rank -> 1, SkillId.BATTLE_CRY, 2, 5, 0xFFD64A52));
        register(skill(SkillId.PALADIN_HEAL, RpgClass.PALADIN, 30, 22, 14.0, 9.0,
                rank -> 1, SkillId.FORTIFY, 3, 5, 0xFFFFE28A));
        register(skill(SkillId.PALADIN_BLESSING, RpgClass.PALADIN, 48, 36, 60.0, 55.0,
                rank -> 1, SkillId.FORTIFY, 6, 8, 0xFFFFD369));
        register(skill(SkillId.DIVINE_BULWARK, RpgClass.PALADIN, 65, 50, 60.0, 55.0,
                rank -> 1, SkillId.PROVOKE, 6, 12, 0xFFFFEDB3));
        register(skill(SkillId.HOLY_SHIELD, RpgClass.PALADIN, 50, 38, 60.0, 55.0,
                rank -> 1, SkillId.FORTIFY, 5, 9, 0xFFFFFFA8));
        register(skill(SkillId.DIVINE_SLASH, RpgClass.PALADIN, 0, 0, 0.5, 0.5,
                rank -> 1, SkillId.WHIRLWIND, 5, 12, 0xFFFFCC4D));
        register(passive(SkillId.PALADIN_VITALITY, RpgClass.PALADIN, 0xFFE2BD73));
        register(passive(SkillId.PALADIN_ARMOR_TRAINING, RpgClass.PALADIN, 5, 0xFFFFD36A));
        register(passive(SkillId.PALADIN_MANA_STRIKE, RpgClass.PALADIN, 3, 0xFFFFE6A0));
    }

    public static SkillDefinition get(SkillId id) {
        return DEFINITIONS.get(id);
    }

    public static List<SkillDefinition> forClass(RpgClass rpgClass) {
        return DEFINITIONS.values().stream()
                .filter(definition -> definition.ownerClass() == rpgClass)
                .sorted(java.util.Comparator
                        .comparingInt(SkillDefinitions::rootSkillId)
                        .thenComparingInt(SkillDefinitions::dependencyDepth)
                        .thenComparingInt(SkillDefinition::requiredClassLevel)
                        .thenComparingInt(definition -> definition.id().numericId()))
                .toList();
    }

    private static int rootSkillId(SkillDefinition definition) {
        SkillDefinition current = definition;
        int guard = 0;
        while (current.hasPrerequisite() && guard++ < SkillId.storageSize()) {
            SkillDefinition parent = DEFINITIONS.get(current.prerequisite());
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return current.id().numericId();
    }

    private static int dependencyDepth(SkillDefinition definition) {
        int depth = 0;
        SkillDefinition current = definition;
        while (current.hasPrerequisite() && depth < SkillId.storageSize()) {
            SkillDefinition parent = DEFINITIONS.get(current.prerequisite());
            if (parent == null) {
                break;
            }
            depth++;
            current = parent;
        }
        return depth;
    }

    public static int maxSpendForClass(RpgClass rpgClass) {
        return forClass(rpgClass).stream().mapToInt(SkillDefinition::maxRank).sum();
    }

    public static SkillId vitalitySkillForClass(RpgClass rpgClass) {
        return switch (rpgClass) {
            case WARRIOR -> SkillId.WARRIOR_VITALITY;
            case MAGE -> SkillId.MAGE_VITALITY;
            case HUNTER -> SkillId.HUNTER_VITALITY;
            case PRIEST -> SkillId.PRIEST_VITALITY;
            case PALADIN -> SkillId.PALADIN_VITALITY;
            case UNASSIGNED -> SkillId.NONE;
        };
    }

    private static SkillDefinition skill(
            SkillId id,
            RpgClass ownerClass,
            int startMana,
            int endMana,
            double startCooldownSeconds,
            double endCooldownSeconds,
            IntUnaryOperator charges,
            SkillId prerequisite,
            int prerequisiteRank,
            int requiredClassLevel,
            int color
    ) {
        return new SkillDefinition(
                id,
                ownerClass,
                MAX_SKILL_RANK,
                intCurve(startMana, endMana),
                tickCurve(startCooldownSeconds, endCooldownSeconds),
                values(charges),
                prerequisite,
                prerequisiteRank,
                rankRequirements(id, requiredClassLevel),
                color
        );
    }

    private static int[] rankRequirements(SkillId id, int firstRankLevel) {
        int[] offsets = switch (progressionProfile(id)) {
            case CORE -> new int[]{0, 2, 4, 7, 10, 14, 18, 22, 27, 32, 38, 43, 49, 55, 60};
            case ADVANCED -> new int[]{0, 3, 6, 10, 14, 19, 24, 30, 36, 43, 50, 58, 66, 71, 75};
            case ULTIMATE -> new int[]{0, 5, 12, 19, 27, 35, 43, 51, 60, 69, 78, 87, 93, 97, 100};
        };
        return values(rank -> firstRankLevel + offsets[rank - 1]);
    }

    private static ProgressionProfile progressionProfile(SkillId id) {
        return switch (id) {
            case METEOR, SKY_RAYS, ARROW_RAIN, BATTLE_CRY, HOLY_STORM, RESURRECTION,
                 DIVINE_BULWARK, EXECUTION, ULTRA_THRUST -> ProgressionProfile.ULTIMATE;
            case MAGIC_SHIELD, CHAIN_LIGHTNING, POWER_SHOT, CAMOUFLAGE,
                 GROUND_STUN, SHIELD_BASH, WINDRUN, RESTORATION, HEALING_HALO,
                 BLESSING, HOLY_SHIELD, PALADIN_BLESSING, BERSERK, SOLAR_BEAM,
                 DIVINE_SLASH, WARRIOR_LEAP, WARRIOR_WHIRLWIND -> ProgressionProfile.ADVANCED;
            default -> ProgressionProfile.CORE;
        };
    }

    private enum ProgressionProfile {
        CORE,
        ADVANCED,
        ULTIMATE
    }

    private static SkillDefinition passive(SkillId id, RpgClass ownerClass, int color) {
        return passive(id, ownerClass, 0, color);
    }

    private static SkillDefinition passive(SkillId id, RpgClass ownerClass, int requiredClassLevel, int color) {
        return skill(
                id, ownerClass,
                0, 0,
                0.05, 0.05,
                rank -> 1,
                SkillId.NONE, 0,
                requiredClassLevel,
                color
        );
    }

    private static int[] intCurve(int start, int end) {
        return values(rank -> (int) Math.round(start + (end - start) * ((rank - 1) / 14.0)));
    }

    private static int[] tickCurve(double startSeconds, double endSeconds) {
        return values(rank -> (int) Math.round(
                (startSeconds + (endSeconds - startSeconds) * ((rank - 1) / 14.0)) * 20.0
        ));
    }

    private static int[] values(IntUnaryOperator value) {
        int[] result = new int[MAX_SKILL_RANK];
        for (int rank = 1; rank <= MAX_SKILL_RANK; rank++) {
            result[rank - 1] = value.applyAsInt(rank);
        }
        return result;
    }

    private static void register(SkillDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    private SkillDefinitions() {
    }
}
