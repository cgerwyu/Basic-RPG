package net.cgerwyu.basicrpgclasses.data;

import com.mojang.serialization.Codec;

import java.util.Arrays;

public enum RpgClass {
    UNASSIGNED(0, "unassigned", false),
    WARRIOR(1, "warrior", true),
    MAGE(2, "mage", true),
    HUNTER(3, "hunter", true),
    PRIEST(4, "priest", true),
    PALADIN(5, "paladin", true);

    public static final Codec<RpgClass> CODEC = Codec.INT.xmap(RpgClass::byId, RpgClass::numericId);

    private final int numericId;
    private final String serializedName;
    private final boolean playable;

    RpgClass(int numericId, String serializedName, boolean playable) {
        this.numericId = numericId;
        this.serializedName = serializedName;
        this.playable = playable;
    }

    public int numericId() {
        return numericId;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean playable() {
        return playable;
    }

    public String translationKey() {
        return "class.basicrpgclasses." + serializedName;
    }

    public static RpgClass byId(int id) {
        return Arrays.stream(values())
                .filter(value -> value.numericId == id)
                .findFirst()
                .orElse(UNASSIGNED);
    }
}
