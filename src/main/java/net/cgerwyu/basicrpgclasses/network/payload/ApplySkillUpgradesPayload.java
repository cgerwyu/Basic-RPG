package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record ApplySkillUpgradesPayload(List<Integer> desiredRanks) implements CustomPacketPayload {
    public static final Type<ApplySkillUpgradesPayload> TYPE =
            new Type<>(BasicRPGClasses.id("apply_skill_upgrades"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplySkillUpgradesPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ApplySkillUpgradesPayload decode(RegistryFriendlyByteBuf buffer) {
            List<Integer> ranks = new ArrayList<>(SkillId.storageSize());
            for (int index = 0; index < SkillId.storageSize(); index++) {
                ranks.add(buffer.readVarInt());
            }
            return new ApplySkillUpgradesPayload(List.copyOf(ranks));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ApplySkillUpgradesPayload payload) {
            for (int index = 0; index < SkillId.storageSize(); index++) {
                int rank = index < payload.desiredRanks.size() ? payload.desiredRanks.get(index) : 0;
                buffer.writeVarInt(rank);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
