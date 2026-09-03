package net.cgerwyu.basicrpgclasses.client;

import net.cgerwyu.basicrpgclasses.skill.SkillId;

import java.util.EnumSet;
import java.util.Set;

public final class ClientToggleSkillStates {
    private static final Set<SkillId> ACTIVE = EnumSet.noneOf(SkillId.class);

    public static void set(SkillId skillId, boolean active) {
        if (active) {
            ACTIVE.add(skillId);
        } else {
            ACTIVE.remove(skillId);
        }
    }

    public static boolean active(SkillId skillId) {
        return ACTIVE.contains(skillId);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private ClientToggleSkillStates() {
    }
}
