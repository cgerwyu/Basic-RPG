package net.cgerwyu.basicrpgclasses.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinition;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record PlayerCombatData(
        int manaTenths,
        long lastManaSpendTick,
        int resourceClassId,
        long blinkSlowFallEndTick,
        long magicShieldEndTick,
        List<Integer> chargeCounts,
        List<Long> cooldownEndTicks
) {
    public static final int MAX_STORED_RESOURCE_TENTHS = 500 * 10;
    private static final int DEFAULT_RESOURCE_TENTHS = 100 * 10;

    public static final MapCodec<PlayerCombatData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.optionalFieldOf("mana_tenths", DEFAULT_RESOURCE_TENTHS).forGetter(PlayerCombatData::manaTenths),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("last_mana_spend_tick", 0L).forGetter(PlayerCombatData::lastManaSpendTick),
            com.mojang.serialization.Codec.INT.optionalFieldOf("resource_class", 0).forGetter(PlayerCombatData::resourceClassId),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("blink_slow_fall_end_tick", 0L).forGetter(PlayerCombatData::blinkSlowFallEndTick),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("magic_shield_end_tick", 0L).forGetter(PlayerCombatData::magicShieldEndTick),
            com.mojang.serialization.Codec.INT.listOf().optionalFieldOf("charge_counts", List.of()).forGetter(PlayerCombatData::chargeCounts),
            com.mojang.serialization.Codec.LONG.listOf().optionalFieldOf("cooldown_end_ticks", List.of()).forGetter(PlayerCombatData::cooldownEndTicks)
    ).apply(instance, PlayerCombatData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCombatData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerCombatData decode(RegistryFriendlyByteBuf buffer) {
            int manaTenths = buffer.readVarInt();
            long lastManaSpendTick = buffer.readVarLong();
            int resourceClassId = buffer.readVarInt();
            long blinkSlowFallEndTick = buffer.readVarLong();
            long magicShieldEndTick = buffer.readVarLong();
            List<Integer> charges = new ArrayList<>(SkillId.storageSize());
            for (int index = 0; index < SkillId.storageSize(); index++) {
                charges.add(buffer.readVarInt());
            }
            List<Long> cooldowns = new ArrayList<>(SkillId.storageSize());
            for (int index = 0; index < SkillId.storageSize(); index++) {
                cooldowns.add(buffer.readVarLong());
            }
            return new PlayerCombatData(manaTenths, lastManaSpendTick, resourceClassId, blinkSlowFallEndTick, magicShieldEndTick, charges, cooldowns);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PlayerCombatData value) {
            buffer.writeVarInt(value.manaTenths);
            buffer.writeVarLong(value.lastManaSpendTick);
            buffer.writeVarInt(value.resourceClassId);
            buffer.writeVarLong(value.blinkSlowFallEndTick);
            buffer.writeVarLong(value.magicShieldEndTick);
            for (int chargeCount : value.chargeCounts) {
                buffer.writeVarInt(chargeCount);
            }
            for (long cooldownEndTick : value.cooldownEndTicks) {
                buffer.writeVarLong(cooldownEndTick);
            }
        }
    };

    public PlayerCombatData {
        manaTenths = Math.clamp(manaTenths, 0, MAX_STORED_RESOURCE_TENTHS);
        lastManaSpendTick = Math.max(0L, lastManaSpendTick);
        resourceClassId = RpgClass.byId(resourceClassId).numericId();
        blinkSlowFallEndTick = Math.max(0L, blinkSlowFallEndTick);
        magicShieldEndTick = Math.max(0L, magicShieldEndTick);
        chargeCounts = normalizeCharges(chargeCounts);
        cooldownEndTicks = normalizeCooldowns(cooldownEndTicks);
    }

    public static PlayerCombatData fresh() {
        return new PlayerCombatData(DEFAULT_RESOURCE_TENTHS, 0L, 0, 0L, 0L, List.of(), List.of());
    }

    public static PlayerCombatData fresh(RpgClass rpgClass) {
        int initialResource = ClassResourceRules.usesFury(rpgClass)
                ? 0
                : ClassResourceRules.baseMaxResource(rpgClass) * 10;
        return new PlayerCombatData(initialResource, 0L, rpgClass.numericId(), 0L, 0L, List.of(), List.of());
    }

    public int resource() {
        return manaTenths / 10;
    }

    public int mana() {
        return resource();
    }

    public boolean canAfford(int manaCost) {
        return manaTenths >= Math.max(0, manaCost) * 10;
    }

    public long cooldownEndTick(SkillId skillId) {
        if (skillId == null || skillId.numericId() < 0 || skillId.numericId() >= cooldownEndTicks.size()) {
            return 0L;
        }
        return cooldownEndTicks.get(skillId.numericId());
    }

    public long remainingCooldownTicks(SkillId skillId, long gameTime) {
        return Math.max(0L, cooldownEndTick(skillId) - gameTime);
    }

    public int availableCharges(SkillDefinition definition, int rank, long gameTime) {
        return chargeState(definition, rank, gameTime).available();
    }

    public long nextChargeReadyTick(SkillDefinition definition, int rank, long gameTime) {
        return chargeState(definition, rank, gameTime).nextReadyTick();
    }

    public long remainingRechargeTicks(SkillDefinition definition, int rank, long gameTime) {
        return Math.max(0L, nextChargeReadyTick(definition, rank, gameTime) - gameTime);
    }

    public PlayerCombatData useSkill(SkillDefinition definition, int rank, long gameTime) {
        return useSkill(definition, rank, gameTime, true);
    }

    public PlayerCombatData useSkill(SkillDefinition definition, int rank, long gameTime, boolean consumeResource) {
        ChargeState currentState = chargeState(definition, rank, gameTime);
        int manaCost = definition.manaCost(rank);
        if (currentState.available() <= 0 || consumeResource && !canAfford(manaCost)) {
            return this;
        }

        int chargesAfterUse = currentState.available() - 1;
        long nextReadyTick = currentState.nextReadyTick();
        if (nextReadyTick <= gameTime) {
            nextReadyTick = gameTime + definition.cooldownTicks(rank);
        }

        List<Integer> updatedCharges = new ArrayList<>(chargeCounts);
        updatedCharges.set(definition.id().numericId(), chargesAfterUse);
        List<Long> updatedCooldowns = new ArrayList<>(cooldownEndTicks);
        updatedCooldowns.set(definition.id().numericId(), nextReadyTick);
        return new PlayerCombatData(
                consumeResource ? manaTenths - manaCost * 10 : manaTenths,
                consumeResource ? gameTime : lastManaSpendTick,
                resourceClassId,
                blinkSlowFallEndTick,
                magicShieldEndTick,
                updatedCharges,
                updatedCooldowns
        );
    }

    public boolean blinkSlowFallActive(long gameTime) {
        return blinkSlowFallEndTick > gameTime;
    }

    public PlayerCombatData withBlinkSlowFall(long endTick) {
        return new PlayerCombatData(
                manaTenths,
                lastManaSpendTick,
                resourceClassId,
                Math.max(blinkSlowFallEndTick, endTick),
                magicShieldEndTick,
                chargeCounts,
                cooldownEndTicks
        );
    }

    public PlayerCombatData clearBlinkSlowFall() {
        if (blinkSlowFallEndTick == 0L) {
            return this;
        }
        return new PlayerCombatData(
                manaTenths,
                lastManaSpendTick,
                resourceClassId,
                0L,
                magicShieldEndTick,
                chargeCounts,
                cooldownEndTicks
        );
    }

    public boolean magicShieldActive(long gameTime) {
        return magicShieldEndTick > gameTime;
    }

    public PlayerCombatData withMagicShield(long endTick) {
        return new PlayerCombatData(
                manaTenths,
                lastManaSpendTick,
                resourceClassId,
                blinkSlowFallEndTick,
                Math.max(magicShieldEndTick, endTick),
                chargeCounts,
                cooldownEndTicks
        );
    }

    public PlayerCombatData regenerate(long gameTime, PlayerClassData classData) {
        int maximum = ClassResourceRules.maxResource(classData) * 10;
        int current = Math.min(manaTenths, maximum);
        int regeneration = ClassResourceRules.regenerationTenthsPerStep(classData);
        if (regeneration <= 0
                || current >= maximum
                || gameTime - lastManaSpendTick < ClassResourceRules.REGEN_DELAY_TICKS) {
            return current == manaTenths ? this : withResourceTenths(current);
        }
        return withResourceTenths(Math.min(maximum, current + regeneration));
    }

    public PlayerCombatData reconcileResourceClass(PlayerClassData classData) {
        if (resourceClassId == classData.rpgClass().numericId()) {
            return this;
        }
        int initialResource = ClassResourceRules.usesFury(classData.rpgClass())
                ? 0
                : ClassResourceRules.maxResource(classData) * 10;
        return new PlayerCombatData(
                initialResource,
                lastManaSpendTick,
                classData.rpgClass().numericId(),
                blinkSlowFallEndTick,
                magicShieldEndTick,
                chargeCounts,
                cooldownEndTicks
        );
    }

    public PlayerCombatData gainResource(int amount, int maximum) {
        int updated = Math.min(Math.max(0, maximum) * 10, manaTenths + Math.max(0, amount) * 10);
        if (updated == manaTenths) {
            return this;
        }
        return withResourceTenths(updated);
    }

    public PlayerCombatData spendResource(int amount, long gameTime) {
        int costTenths = Math.max(0, amount) * 10;
        if (costTenths == 0 || manaTenths < costTenths) {
            return this;
        }
        return new PlayerCombatData(
                manaTenths - costTenths,
                gameTime,
                resourceClassId,
                blinkSlowFallEndTick,
                magicShieldEndTick,
                chargeCounts,
                cooldownEndTicks
        );
    }

    public PlayerCombatData clampCooldowns(PlayerClassData classData, long gameTime) {
        List<Long> updatedCooldowns = new ArrayList<>(cooldownEndTicks);
        boolean changed = false;
        for (SkillDefinition definition : SkillDefinitions.forClass(classData.rpgClass())) {
            if (definition.id().isPassive()) {
                continue;
            }
            int rank = classData.skillRank(definition.id());
            if (rank <= 0) {
                continue;
            }
            int index = definition.id().numericId();
            long maximumEndTick = gameTime + definition.cooldownTicks(rank);
            if (updatedCooldowns.get(index) > maximumEndTick) {
                updatedCooldowns.set(index, maximumEndTick);
                changed = true;
            }
        }
        return changed
                ? new PlayerCombatData(manaTenths, lastManaSpendTick, resourceClassId, blinkSlowFallEndTick, magicShieldEndTick, chargeCounts, updatedCooldowns)
                : this;
    }

    private PlayerCombatData withResourceTenths(int resourceTenths) {
        return new PlayerCombatData(
                resourceTenths,
                lastManaSpendTick,
                resourceClassId,
                blinkSlowFallEndTick,
                magicShieldEndTick,
                chargeCounts,
                cooldownEndTicks
        );
    }

    public PlayerCombatData reconcileSkillRanks(
            PlayerClassData before,
            PlayerClassData after,
            long gameTime
    ) {
        List<Integer> updatedCharges = new ArrayList<>(chargeCounts);
        List<Long> updatedCooldowns = new ArrayList<>(cooldownEndTicks);
        boolean changed = false;
        for (SkillDefinition definition : SkillDefinitions.forClass(after.rpgClass())) {
            int oldRank = before.skillRank(definition.id());
            int newRank = after.skillRank(definition.id());
            if (newRank <= oldRank) {
                continue;
            }

            int index = definition.id().numericId();
            if (oldRank <= 0) {
                updatedCharges.set(index, definition.maxCharges(newRank));
                updatedCooldowns.set(index, 0L);
                changed = true;
                continue;
            }

            ChargeState oldState = chargeState(definition, oldRank, gameTime);
            int available = Math.min(oldState.available(), definition.maxCharges(newRank));
            long nextReady = oldState.nextReadyTick();
            if (oldState.available() >= definition.maxCharges(oldRank) && nextReady == 0L) {
                available = definition.maxCharges(newRank);
            }
            updatedCharges.set(index, available);
            updatedCooldowns.set(index, available >= definition.maxCharges(newRank) ? 0L : nextReady);
            changed = true;
        }
        return changed
                ? new PlayerCombatData(manaTenths, lastManaSpendTick, resourceClassId, blinkSlowFallEndTick, magicShieldEndTick, updatedCharges, updatedCooldowns)
                : this;
    }

    private ChargeState chargeState(SkillDefinition definition, int rank, long gameTime) {
        int skillIndex = definition.id().numericId();
        int maxCharges = definition.maxCharges(rank);
        int storedCharges = chargeCounts.get(skillIndex);
        long storedReadyTick = cooldownEndTicks.get(skillIndex);

        // Worlds from the previous format have no charge counter. A running old cooldown
        // maps to zero charges; otherwise the skill starts fully charged.
        if (storedCharges < 0) {
            return storedReadyTick > gameTime
                    ? new ChargeState(0, storedReadyTick)
                    : new ChargeState(maxCharges, 0L);
        }

        int available = Math.min(storedCharges, maxCharges);
        if (available >= maxCharges) {
            return new ChargeState(maxCharges, 0L);
        }
        if (storedReadyTick <= 0L) {
            return new ChargeState(available, gameTime + definition.cooldownTicks(rank));
        }
        if (gameTime < storedReadyTick) {
            return new ChargeState(available, storedReadyTick);
        }

        long rechargeTicks = definition.cooldownTicks(rank);
        long gained = 1L + (gameTime - storedReadyTick) / rechargeTicks;
        int recharged = Math.min(maxCharges, available + (int) gained);
        long nextReady = recharged >= maxCharges ? 0L : storedReadyTick + gained * rechargeTicks;
        return new ChargeState(recharged, nextReady);
    }

    private static List<Integer> normalizeCharges(List<Integer> source) {
        List<Integer> normalized = new ArrayList<>(SkillId.storageSize());
        for (int index = 0; index < SkillId.storageSize(); index++) {
            int value = source != null && index < source.size() ? source.get(index) : -1;
            normalized.add(Math.max(-1, value));
        }
        return List.copyOf(normalized);
    }

    private static List<Long> normalizeCooldowns(List<Long> source) {
        List<Long> normalized = new ArrayList<>(SkillId.storageSize());
        for (int index = 0; index < SkillId.storageSize(); index++) {
            long value = source != null && index < source.size() ? source.get(index) : 0L;
            normalized.add(Math.max(0L, value));
        }
        return List.copyOf(normalized);
    }

    private record ChargeState(int available, long nextReadyTick) {
    }
}
