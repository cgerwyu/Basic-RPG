package net.cgerwyu.basicrpgclasses.skill;

import java.util.Arrays;

public enum SkillId {
    NONE(0, "none"),
    WHIRLWIND(1, "whirlwind"),
    FORTIFY(2, "fortify"),
    PROVOKE(3, "provoke"),
    FIREBALL(4, "fireball"),
    HEAL(5, "heal"),
    BLINK(6, "blink"),
    DASH(7, "dash"),
    WINDRUN(8, "windrun"),
    CAMOUFLAGE(9, "camouflage"),
    MAGIC_SHIELD(10, "magic_shield"),
    WARRIOR_VITALITY(11, "warrior_vitality"),
    MAGE_VITALITY(12, "mage_vitality"),
    HUNTER_VITALITY(13, "hunter_vitality"),
    MAGE_GLIDE(14, "mage_glide"),
    MAGE_MANA_REGEN(15, "mage_mana_regen"),
    GROUND_STUN(16, "ground_stun"),
    HUNTER_FALL_TRAINING(17, "hunter_fall_training"),
    HUNTER_CLIMBING(18, "hunter_climbing"),
    FROST_NOVA(19, "frost_nova"),
    METEOR(20, "meteor"),
    SKY_RAYS(21, "sky_rays"),
    CHAIN_LIGHTNING(22, "chain_lightning"),
    MULTISHOT(23, "multishot"),
    ARROW_RAIN(24, "arrow_rain"),
    POWER_SHOT(25, "power_shot"),
    FROST_ARROWS(26, "frost_arrows"),
    SHIELD_BASH(27, "shield_bash"),
    BATTLE_CRY(28, "battle_cry"),
    HUNTER_MANA_REGEN(29, "hunter_mana_regen"),
    WARRIOR_VAMPIRISM(30, "warrior_vampirism"),
    HUNTER_DRAW_SPEED(31, "hunter_draw_speed"),
    HUNTER_SHOT_POWER(32, "hunter_shot_power"),
    RESTORATION(33, "restoration"),
    HEALING_HALO(34, "healing_halo"),
    RESURRECTION(35, "resurrection"),
    BLESSING(36, "blessing"),
    HOLY_SHIELD(37, "holy_shield"),
    CLEANSE(38, "cleanse"),
    HOLY_STORM(39, "holy_storm"),
    PRIEST_VITALITY(40, "priest_vitality"),
    PRIEST_MANA_REGEN(41, "priest_mana_regen"),
    PALADIN_HEAL(42, "paladin_heal"),
    PALADIN_BLESSING(43, "paladin_blessing"),
    DIVINE_BULWARK(44, "divine_bulwark"),
    PALADIN_VITALITY(45, "paladin_vitality"),
    BERSERK(46, "berserk"),
    EXECUTION(47, "execution"),
    ULTRA_THRUST(48, "ultra_thrust"),
    HOLY_BOLT(49, "holy_bolt"),
    SOLAR_BEAM(50, "solar_beam"),
    DIVINE_SLASH(51, "divine_slash"),
    PALADIN_ARMOR_TRAINING(52, "paladin_armor_training"),
    PALADIN_MANA_STRIKE(53, "paladin_mana_strike"),
    WARRIOR_LEAP(54, "warrior_leap"),
    WARRIOR_WHIRLWIND(55, "warrior_whirlwind");

    private final int numericId;
    private final String serializedName;

    SkillId(int numericId, String serializedName) {
        this.numericId = numericId;
        this.serializedName = serializedName;
    }

    public int numericId() {
        return numericId;
    }

    public String serializedName() {
        return serializedName;
    }

    public String translationKey() {
        return "skill.basicrpgclasses." + serializedName;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    public String shortTranslationKey() {
        return translationKey() + ".short";
    }

    public boolean isPassive() {
        return this == WARRIOR_VITALITY
                || this == MAGE_VITALITY
                || this == HUNTER_VITALITY
                || this == MAGE_GLIDE
                || this == MAGE_MANA_REGEN
                || this == HUNTER_FALL_TRAINING
                || this == HUNTER_CLIMBING
                || this == HUNTER_MANA_REGEN
                || this == HUNTER_DRAW_SPEED
                || this == HUNTER_SHOT_POWER
                || this == WARRIOR_VAMPIRISM
                || this == PRIEST_VITALITY
                || this == PRIEST_MANA_REGEN
                || this == PALADIN_VITALITY
                || this == PALADIN_ARMOR_TRAINING
                || this == PALADIN_MANA_STRIKE;
    }

    public static int storageSize() {
        return values().length;
    }

    public static SkillId byId(int id) {
        return Arrays.stream(values())
                .filter(skill -> skill.numericId == id)
                .findFirst()
                .orElse(NONE);
    }
}
