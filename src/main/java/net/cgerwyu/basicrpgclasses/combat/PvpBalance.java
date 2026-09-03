package net.cgerwyu.basicrpgclasses.combat;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.party.PartyService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central PvP rules. PvE skills keep their spectacle, while player-versus-player
 * combat gets longer time-to-kill, control diminishing returns and anti-gank rules.
 * Scoreboard team prefixes are deliberately used as integration hooks until native duel and
 * guild-war managers are added.
 */
public final class PvpBalance {
    public static final String DUEL_TEAM_PREFIX = "brc_duel_";
    public static final String WAR_TEAM_PREFIX = "brc_war_";

    private static final int NOVICE_MAX_CLASS_LEVEL = 15;
    private static final int NOVICE_PROTECTED_LEVEL_GAP = 10;
    private static final long RETALIATION_WINDOW_TICKS = 30L * 20L;
    private static final long PVP_COMBAT_TICKS = 10L * 20L;
    private static final long RESPAWN_PROTECTION_TICKS = 20L * 20L;
    private static final long LOGIN_PROTECTION_TICKS = 8L * 20L;
    private static final float MAX_RAW_DAMAGE_PER_HIT = 7.0F;
    private static final float PVP_VAMPIRISM_HEAL_PER_TICK = 1.5F;

    private static final Map<PlayerPair, Long> AGGRESSION_END_TICKS = new HashMap<>();
    private static final Map<UUID, Long> COMBAT_END_TICKS = new HashMap<>();
    private static final Map<UUID, Long> SPAWN_PROTECTION_END_TICKS = new HashMap<>();
    private static final Map<UUID, Long> CONTROL_RESISTANCE_END_TICKS = new HashMap<>();
    private static final Map<UUID, Long> WARNING_COOLDOWN_END_TICKS = new HashMap<>();
    private static final Map<UUID, VampirismBudget> VAMPIRISM_BUDGETS = new HashMap<>();

    public static void grantLoginProtection(ServerPlayer player) {
        grantSpawnProtection(player, LOGIN_PROTECTION_TICKS);
    }

    public static void grantRespawnProtection(ServerPlayer player) {
        grantSpawnProtection(player, RESPAWN_PROTECTION_TICKS);
    }

    private static void grantSpawnProtection(ServerPlayer player, long durationTicks) {
        SPAWN_PROTECTION_END_TICKS.put(player.getUUID(), player.level().getGameTime() + durationTicks);
    }

    /**
     * Records voluntary aggression and returns true when the attack must be denied.
     * A novice loses protection against a player they attacked, so the system cannot
     * be used to deal risk-free damage.
     */
    public static boolean denyAttack(ServerPlayer attacker, ServerPlayer target) {
        if (attacker == target) {
            return false;
        }
        if (PartyService.sameParty(attacker, target)) {
            return true;
        }

        long now = attacker.level().getGameTime();
        cleanup(now);
        SPAWN_PROTECTION_END_TICKS.remove(attacker.getUUID());
        AGGRESSION_END_TICKS.put(
                new PlayerPair(attacker.getUUID(), target.getUUID()),
                now + RETALIATION_WINDOW_TICKS
        );

        if (structuredCombat(attacker, target)) {
            return false;
        }
        if (SPAWN_PROTECTION_END_TICKS.getOrDefault(target.getUUID(), 0L) > now) {
            warnProtected(attacker, target, now);
            return true;
        }

        PlayerClassData attackerData = attacker.getData(ModAttachments.PLAYER_CLASS);
        PlayerClassData targetData = target.getData(ModAttachments.PLAYER_CLASS);
        boolean novice = targetData.classLevel() <= NOVICE_MAX_CLASS_LEVEL;
        boolean unfairGap = attackerData.classLevel() - targetData.classLevel() >= NOVICE_PROTECTED_LEVEL_GAP;
        boolean targetStartedFight = AGGRESSION_END_TICKS.getOrDefault(
                new PlayerPair(target.getUUID(), attacker.getUUID()), 0L
        ) > now;
        if (novice && unfairGap && !targetStartedFight) {
            warnProtected(attacker, target, now);
            return true;
        }
        return false;
    }

    public static void markCombat(ServerPlayer first, ServerPlayer second) {
        long endTick = first.level().getGameTime() + PVP_COMBAT_TICKS;
        COMBAT_END_TICKS.put(first.getUUID(), endTick);
        COMBAT_END_TICKS.put(second.getUUID(), endTick);
    }

    public static boolean inCombat(ServerPlayer player) {
        long now = player.level().getGameTime();
        return COMBAT_END_TICKS.getOrDefault(player.getUUID(), 0L) > now;
    }

    /**
     * PvP has a global damage reduction, a modest underdog adjustment and a raw
     * single-hit cap. Progress still matters, but it cannot create one-button kills.
     */
    public static float adjustDamage(ServerPlayer attacker, ServerPlayer target, float amount) {
        PlayerClassData attackerData = attacker.getData(ModAttachments.PLAYER_CLASS);
        PlayerClassData targetData = target.getData(ModAttachments.PLAYER_CLASS);
        int levelGap = attackerData.classLevel() - targetData.classLevel();

        double levelFactor;
        if (levelGap > 0) {
            levelFactor = Math.max(0.62, 1.0 / (1.0 + Math.min(40, levelGap) * 0.018));
        } else {
            levelFactor = 1.0 + Math.min(30, -levelGap) * 0.008;
        }
        double modeFactor = bothInModeTeams(attacker, target, DUEL_TEAM_PREFIX) ? 0.68 : 0.72;
        return Math.min(MAX_RAW_DAMAGE_PER_HIT, (float) (Math.max(0.0F, amount) * modeFactor * levelFactor));
    }

    public static float healingMultiplier(ServerPlayer healer) {
        return inCombat(healer) ? 0.55F : 1.0F;
    }

    public static float warriorVampirismMultiplier(LivingEntity damaged) {
        return damaged instanceof ServerPlayer ? 0.5F : 1.0F;
    }

    public static float capWarriorVampirismHeal(ServerPlayer warrior, LivingEntity damaged, float requested) {
        float safeRequested = Math.max(0.0F, requested);
        if (!(damaged instanceof ServerPlayer)) {
            return safeRequested;
        }
        long now = warrior.level().getGameTime();
        VampirismBudget budget = VAMPIRISM_BUDGETS.get(warrior.getUUID());
        float alreadyHealed = budget != null && budget.tick == now ? budget.healed : 0.0F;
        float allowed = Math.min(safeRequested, Math.max(0.0F, PVP_VAMPIRISM_HEAL_PER_TICK - alreadyHealed));
        VAMPIRISM_BUDGETS.put(warrior.getUUID(), new VampirismBudget(now, alreadyHealed + allowed));
        return allowed;
    }

    /**
     * Hard control on players is short and followed by a resistance window. Missing
     * the control therefore matters, while chaining two stuns cannot remove all input.
     */
    public static int claimHardControl(LivingEntity target, int requestedTicks, int pvpCapTicks) {
        if (!(target instanceof ServerPlayer player)) {
            return Math.max(1, requestedTicks);
        }
        long now = player.level().getGameTime();
        if (CONTROL_RESISTANCE_END_TICKS.getOrDefault(player.getUUID(), 0L) > now) {
            return 0;
        }
        int duration = Math.max(1, Math.min(requestedTicks, pvpCapTicks));
        CONTROL_RESISTANCE_END_TICKS.put(player.getUUID(), now + duration + 60L);
        return duration;
    }

    public static int softControlDuration(LivingEntity target, int pveTicks, int pvpCapTicks) {
        return target instanceof ServerPlayer ? Math.min(pveTicks, pvpCapTicks) : pveTicks;
    }

    public static int softControlAmplifier(LivingEntity target, int pveAmplifier) {
        return target instanceof ServerPlayer ? 0 : pveAmplifier;
    }

    public static boolean friendlyPlayer(ServerPlayer caster, ServerPlayer target) {
        return target == caster || PartyService.sameParty(caster, target);
    }

    /** Keeps party protection independent from vanilla scoreboard teams used by duel and war modes. */
    public static boolean canDamagePlayer(ServerPlayer attacker, ServerPlayer target) {
        return attacker != target
                && !PartyService.sameParty(attacker, target)
                && attacker.canHarmPlayer(target);
    }

    public static void clearPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        COMBAT_END_TICKS.remove(playerId);
        SPAWN_PROTECTION_END_TICKS.remove(playerId);
        CONTROL_RESISTANCE_END_TICKS.remove(playerId);
        WARNING_COOLDOWN_END_TICKS.remove(playerId);
        VAMPIRISM_BUDGETS.remove(playerId);
        AGGRESSION_END_TICKS.keySet().removeIf(pair -> pair.attacker.equals(playerId) || pair.target.equals(playerId));
    }

    private static boolean structuredCombat(ServerPlayer first, ServerPlayer second) {
        return bothInModeTeams(first, second, DUEL_TEAM_PREFIX)
                || bothInModeTeams(first, second, WAR_TEAM_PREFIX);
    }

    private static boolean bothInModeTeams(ServerPlayer first, ServerPlayer second, String prefix) {
        return first.getTeam() != null
                && second.getTeam() != null
                && first.getTeam().getName().startsWith(prefix)
                && second.getTeam().getName().startsWith(prefix);
    }

    private static void warnProtected(ServerPlayer attacker, ServerPlayer target, long now) {
        if (WARNING_COOLDOWN_END_TICKS.getOrDefault(attacker.getUUID(), 0L) > now) {
            return;
        }
        WARNING_COOLDOWN_END_TICKS.put(attacker.getUUID(), now + 40L);
        attacker.sendOverlayMessage(Component.translatable(
                "message.basicrpgclasses.pvp_target_protected",
                target.getDisplayName()
        ));
    }

    private static void cleanup(long now) {
        AGGRESSION_END_TICKS.values().removeIf(endTick -> endTick <= now);
        COMBAT_END_TICKS.values().removeIf(endTick -> endTick <= now);
        SPAWN_PROTECTION_END_TICKS.values().removeIf(endTick -> endTick <= now);
        CONTROL_RESISTANCE_END_TICKS.values().removeIf(endTick -> endTick <= now);
        WARNING_COOLDOWN_END_TICKS.values().removeIf(endTick -> endTick <= now);
    }

    private record PlayerPair(UUID attacker, UUID target) {
    }

    private record VampirismBudget(long tick, float healed) {
    }

    private PvpBalance() {
    }
}
