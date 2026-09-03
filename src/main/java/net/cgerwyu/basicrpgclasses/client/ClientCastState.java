package net.cgerwyu.basicrpgclasses.client;

import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.minecraft.client.Minecraft;

public final class ClientCastState {
    private static SkillId skill = SkillId.NONE;
    private static long startTick;
    private static int durationTicks;

    public static void start(SkillId skillId, int duration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }
        skill = skillId;
        startTick = minecraft.level.getGameTime();
        durationTicks = Math.max(1, duration);
    }

    public static void clear() {
        skill = SkillId.NONE;
        startTick = 0L;
        durationTicks = 0;
    }

    public static boolean active() {
        return skill != SkillId.NONE && remainingTicks() > 0L;
    }

    public static SkillId skill() {
        return skill;
    }

    public static long remainingTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || skill == SkillId.NONE) {
            return 0L;
        }
        return Math.max(0L, startTick + durationTicks - minecraft.level.getGameTime());
    }

    public static float progress() {
        return durationTicks <= 0 ? 0.0F : 1.0F - remainingTicks() / (float) durationTicks;
    }

    private ClientCastState() {
    }
}
