package net.cgerwyu.basicrpgclasses.client.hud;

import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.Map;

public final class SkillHudAnimations {
    private static final long READY_FLASH_DURATION_MS = 700L;
    private static final Map<SkillId, Observation> OBSERVATIONS = new EnumMap<>(SkillId.class);

    public static float readyFlash(SkillId skillId, int availableCharges) {
        Observation previous = OBSERVATIONS.get(skillId);
        if (previous == null) {
            OBSERVATIONS.put(skillId, new Observation(availableCharges, 0L));
            return 0.0F;
        }

        Observation current = previous;
        if (availableCharges > previous.availableCharges) {
            current = new Observation(availableCharges, Util.getMillis());
            OBSERVATIONS.put(skillId, current);
        } else if (availableCharges != previous.availableCharges) {
            current = new Observation(availableCharges, 0L);
            OBSERVATIONS.put(skillId, current);
        }
        if (current.flashStartedAt == 0L) {
            return 0.0F;
        }

        long elapsed = Util.getMillis() - current.flashStartedAt;
        if (elapsed >= READY_FLASH_DURATION_MS) {
            OBSERVATIONS.put(skillId, new Observation(availableCharges, 0L));
            return 0.0F;
        }
        return 1.0F - elapsed / (float) READY_FLASH_DURATION_MS;
    }

    public static void reset() {
        OBSERVATIONS.clear();
    }

    private record Observation(int availableCharges, long flashStartedAt) {
    }

    private SkillHudAnimations() {
    }
}
