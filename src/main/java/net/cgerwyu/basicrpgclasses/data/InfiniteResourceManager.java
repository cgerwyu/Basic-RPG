package net.cgerwyu.basicrpgclasses.data;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Session-only testing override. Creative players always receive the same benefit. */
public final class InfiniteResourceManager {
    private static final Set<UUID> ENABLED = new HashSet<>();

    public static boolean active(ServerPlayer player) {
        return player.getAbilities().instabuild || ENABLED.contains(player.getUUID());
    }

    public static boolean toggle(ServerPlayer player) {
        if (!ENABLED.add(player.getUUID())) {
            ENABLED.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void set(ServerPlayer player, boolean active) {
        if (active) {
            ENABLED.add(player.getUUID());
        } else {
            ENABLED.remove(player.getUUID());
        }
    }

    public static void clear(ServerPlayer player) {
        ENABLED.remove(player.getUUID());
    }

    private InfiniteResourceManager() {
    }
}
