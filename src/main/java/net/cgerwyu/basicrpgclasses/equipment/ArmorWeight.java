package net.cgerwyu.basicrpgclasses.equipment;

public enum ArmorWeight {
    LIGHT("tooltip.basicrpgclasses.armor.light"),
    MEDIUM("tooltip.basicrpgclasses.armor.medium"),
    HEAVY("tooltip.basicrpgclasses.armor.heavy");

    private final String translationKey;

    ArmorWeight(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
