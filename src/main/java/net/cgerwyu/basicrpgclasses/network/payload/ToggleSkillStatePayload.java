package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ToggleSkillStatePayload(int skillId, boolean active) implements CustomPacketPayload {
    public static final Type<ToggleSkillStatePayload> TYPE =
            new Type<>(BasicRPGClasses.id("toggle_skill_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSkillStatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ToggleSkillStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new ToggleSkillStatePayload(buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ToggleSkillStatePayload value) {
            buffer.writeVarInt(value.skillId());
            buffer.writeBoolean(value.active());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
