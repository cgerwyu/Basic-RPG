package net.cgerwyu.basicrpgclasses.equipment;

public enum AccessoryType {
    NECKLACE("tooltip.basicrpgclasses.type.necklace"),
    RING("tooltip.basicrpgclasses.type.ring"),
    BELT("tooltip.basicrpgclasses.type.belt");

    private final String translationKey;

    AccessoryType(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
