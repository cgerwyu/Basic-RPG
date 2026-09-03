package net.cgerwyu.basicrpgclasses.equipment;

public enum RpgEquipmentSlotType {
    MAIN_WEAPON(0),
    OFF_WEAPON(1),
    NECKLACE(2),
    RING_LEFT(3),
    RING_RIGHT(4),
    BELT(5);

    public static final int COUNT = 6;

    private final int index;

    RpgEquipmentSlotType(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
