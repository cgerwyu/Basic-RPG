package net.cgerwyu.basicrpgclasses.client;

import net.cgerwyu.basicrpgclasses.network.payload.ActivateSkillSlotPayload;
import net.cgerwyu.basicrpgclasses.network.payload.CastHoldPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SetCombatModePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class CombatModeController {
    public static final int SKILL_SLOT_COUNT = 9;

    private static boolean active;
    private static int selectedSkillSlot;
    private static boolean castHeld;

    public static boolean active() {
        return active;
    }

    public static void toggle() {
        active = !active;
        ClientPacketDistributor.sendToServer(new SetCombatModePayload(active));
    }

    public static void deactivate() {
        boolean wasActive = active;
        active = false;
        castHeld = false;
        ClientCastState.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (wasActive && minecraft.player != null && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new SetCombatModePayload(false));
        }
    }

    public static int selectedSkillSlot() {
        return selectedSkillSlot;
    }

    public static void selectSkillSlot(int slot) {
        if (slot >= 0 && slot < SKILL_SLOT_COUNT) {
            selectedSkillSlot = slot;
        }
    }

    public static void activateSelectedSkill() {
        if (!active) {
            return;
        }
        ClientPacketDistributor.sendToServer(new ActivateSkillSlotPayload(selectedSkillSlot));
    }

    public static void setCastHeld(boolean held) {
        if (castHeld == held) {
            return;
        }
        castHeld = held;
        ClientPacketDistributor.sendToServer(new CastHoldPayload(held));
    }

    public static boolean castHeld() {
        return castHeld;
    }

    private CombatModeController() {
    }
}
