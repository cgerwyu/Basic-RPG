package net.cgerwyu.basicrpgclasses.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinition;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record PlayerClassData(
        RpgClass rpgClass,
        int earnedSkillPoints,
        int unspentSkillPoints,
        int spentMinecraftLevels,
        List<Integer> skillRanks,
        List<Integer> actionBarSlots
) {
    public static final MapCodec<PlayerClassData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RpgClass.CODEC.fieldOf("class").forGetter(PlayerClassData::rpgClass),
            com.mojang.serialization.Codec.INT.fieldOf("earned_skill_points").forGetter(PlayerClassData::earnedSkillPoints),
            com.mojang.serialization.Codec.INT.fieldOf("unspent_skill_points").forGetter(PlayerClassData::unspentSkillPoints),
            com.mojang.serialization.Codec.INT.optionalFieldOf("spent_minecraft_levels", 0).forGetter(PlayerClassData::spentMinecraftLevels),
            com.mojang.serialization.Codec.INT.listOf().optionalFieldOf("skill_ranks", List.of()).forGetter(PlayerClassData::skillRanks),
            com.mojang.serialization.Codec.INT.listOf().optionalFieldOf("action_bar_slots", List.of()).forGetter(PlayerClassData::actionBarSlots)
    ).apply(instance, PlayerClassData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerClassData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerClassData decode(RegistryFriendlyByteBuf buffer) {
            return new PlayerClassData(
                    RpgClass.byId(buffer.readVarInt()),
                    Math.max(0, buffer.readVarInt()),
                    Math.max(0, buffer.readVarInt()),
                    Math.max(0, buffer.readVarInt()),
                    readIntList(buffer, SkillId.storageSize()),
                    readIntList(buffer, ACTION_BAR_SLOT_COUNT)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PlayerClassData value) {
            buffer.writeVarInt(value.rpgClass.numericId());
            buffer.writeVarInt(value.earnedSkillPoints);
            buffer.writeVarInt(value.unspentSkillPoints);
            buffer.writeVarInt(value.spentMinecraftLevels);
            writeIntList(buffer, value.skillRanks);
            writeIntList(buffer, value.actionBarSlots);
        }
    };

    public static final int ACTION_BAR_SLOT_COUNT = 9;

    public PlayerClassData {
        rpgClass = rpgClass == null ? RpgClass.UNASSIGNED : rpgClass;
        earnedSkillPoints = Math.max(0, earnedSkillPoints);
        spentMinecraftLevels = Math.max(0, spentMinecraftLevels);
        skillRanks = normalizeSkillRanks(skillRanks, rpgClass);
        int spentSkillPoints = skillRanks.stream().mapToInt(Integer::intValue).sum();
        // Keep the point economy conserved when skills move between classes in a mod update.
        // Invalid old ranks are removed by normalization and immediately become spendable again.
        unspentSkillPoints = Math.max(0, earnedSkillPoints - spentSkillPoints);
        actionBarSlots = normalizeActionBar(actionBarSlots, rpgClass, skillRanks);
    }

    public static PlayerClassData unassigned() {
        return new PlayerClassData(RpgClass.UNASSIGNED, 0, 0, 0, List.of(), List.of());
    }

    public int classLevel() {
        return earnedSkillPoints;
    }

    public boolean hasClass() {
        return rpgClass.playable();
    }

    public PlayerClassData selectClass(RpgClass selectedClass) {
        if (hasClass() || selectedClass == null || !selectedClass.playable()) {
            return this;
        }
        return new PlayerClassData(
                selectedClass,
                earnedSkillPoints,
                unspentSkillPoints,
                spentMinecraftLevels,
                skillRanks,
                actionBarSlots
        );
    }

    public PlayerClassData purchaseSkillPoint(int minecraftLevelCost) {
        return new PlayerClassData(
                rpgClass,
                earnedSkillPoints + 1,
                unspentSkillPoints + 1,
                spentMinecraftLevels + Math.max(0, minecraftLevelCost),
                skillRanks,
                actionBarSlots
        );
    }

    public int skillRank(SkillId skillId) {
        if (skillId == null || skillId.numericId() < 0 || skillId.numericId() >= skillRanks.size()) {
            return 0;
        }
        return skillRanks.get(skillId.numericId());
    }

    public SkillId skillAtSlot(int slot) {
        if (slot < 0 || slot >= actionBarSlots.size()) {
            return SkillId.NONE;
        }
        return SkillId.byId(actionBarSlots.get(slot));
    }

    public PlayerClassData applySkillRanks(List<Integer> requestedRanks) {
        if (requestedRanks == null || requestedRanks.size() != SkillId.storageSize()) {
            return this;
        }

        List<Integer> updatedRanks = new ArrayList<>(skillRanks);
        int pointsToSpend = 0;
        for (int index = 0; index < SkillId.storageSize(); index++) {
            SkillId skillId = SkillId.byId(index);
            SkillDefinition definition = SkillDefinitions.get(skillId);
            int currentRank = skillRanks.get(index);
            int desiredRank = requestedRanks.get(index);
            if (desiredRank < currentRank) {
                return this;
            }
            if (definition == null) {
                if (desiredRank != 0) {
                    return this;
                }
                continue;
            }
            if (definition.ownerClass() != rpgClass) {
                if (desiredRank != 0) {
                    return this;
                }
                continue;
            }
            if (desiredRank > definition.maxRank()) {
                return this;
            }
            if (desiredRank > currentRank
                    && earnedSkillPoints < definition.requiredClassLevelForRank(desiredRank)) {
                return this;
            }
            pointsToSpend += desiredRank - currentRank;
            updatedRanks.set(index, desiredRank);
        }

        if (pointsToSpend <= 0 || pointsToSpend > unspentSkillPoints) {
            return this;
        }
        for (SkillDefinition definition : SkillDefinitions.forClass(rpgClass)) {
            int desiredRank = updatedRanks.get(definition.id().numericId());
            if (desiredRank > 0
                    && definition.hasPrerequisite()
                    && updatedRanks.get(definition.prerequisite().numericId()) < definition.prerequisiteRank()) {
                return this;
            }
        }

        return new PlayerClassData(
                rpgClass,
                earnedSkillPoints,
                unspentSkillPoints - pointsToSpend,
                spentMinecraftLevels,
                updatedRanks,
                actionBarSlots
        );
    }

    public PlayerClassData setActionBarSlot(int slot, SkillId skillId) {
        if (slot < 0 || slot >= ACTION_BAR_SLOT_COUNT || skillId == null) {
            return this;
        }
        if (skillId != SkillId.NONE) {
            SkillDefinition definition = SkillDefinitions.get(skillId);
            if (definition == null || skillId.isPassive() || definition.ownerClass() != rpgClass || skillRank(skillId) <= 0) {
                return this;
            }
        }

        List<Integer> updatedSlots = new ArrayList<>(actionBarSlots);
        if (skillId != SkillId.NONE) {
            for (int index = 0; index < updatedSlots.size(); index++) {
                if (updatedSlots.get(index) == skillId.numericId()) {
                    updatedSlots.set(index, SkillId.NONE.numericId());
                }
            }
        }
        updatedSlots.set(slot, skillId.numericId());

        return new PlayerClassData(
                rpgClass,
                earnedSkillPoints,
                unspentSkillPoints,
                spentMinecraftLevels,
                skillRanks,
                updatedSlots
        );
    }

    public PlayerClassData resetClass() {
        return unassigned();
    }

    private static List<Integer> normalizeSkillRanks(List<Integer> source, RpgClass rpgClass) {
        List<Integer> normalized = new ArrayList<>(SkillId.storageSize());
        for (int index = 0; index < SkillId.storageSize(); index++) {
            SkillId skillId = SkillId.byId(index);
            SkillDefinition definition = SkillDefinitions.get(skillId);
            int value = source != null && index < source.size() ? source.get(index) : 0;
            int maxRank = definition == null ? 0 : definition.maxRank();
            if (definition == null || definition.ownerClass() != rpgClass) {
                value = 0;
            }
            normalized.add(Math.clamp(value, 0, maxRank));
        }
        return List.copyOf(normalized);
    }

    private static List<Integer> normalizeActionBar(List<Integer> source, RpgClass rpgClass, List<Integer> ranks) {
        List<Integer> normalized = new ArrayList<>(ACTION_BAR_SLOT_COUNT);
        Set<SkillId> assigned = new HashSet<>();
        for (int slot = 0; slot < ACTION_BAR_SLOT_COUNT; slot++) {
            int storedId = source != null && slot < source.size() ? source.get(slot) : SkillId.NONE.numericId();
            SkillId skillId = SkillId.byId(storedId);
            SkillDefinition definition = SkillDefinitions.get(skillId);
            boolean valid = skillId != SkillId.NONE
                    && !skillId.isPassive()
                    && definition != null
                    && definition.ownerClass() == rpgClass
                    && ranks.get(skillId.numericId()) > 0
                    && assigned.add(skillId);
            normalized.add(valid ? skillId.numericId() : SkillId.NONE.numericId());
        }
        return List.copyOf(normalized);
    }

    private static List<Integer> readIntList(RegistryFriendlyByteBuf buffer, int expectedSize) {
        List<Integer> values = new ArrayList<>(expectedSize);
        for (int index = 0; index < expectedSize; index++) {
            values.add(buffer.readVarInt());
        }
        return values;
    }

    private static void writeIntList(RegistryFriendlyByteBuf buffer, List<Integer> values) {
        for (int value : values) {
            buffer.writeVarInt(value);
        }
    }
}
