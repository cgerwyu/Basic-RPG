package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CastStatePayload(int skillId, int durationTicks, boolean active) implements CustomPacketPayload {
    public static final Type<CastStatePayload> TYPE = new Type<>(BasicRPGClasses.id("cast_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastStatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CastStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new CastStatePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CastStatePayload value) {
            buffer.writeVarInt(value.skillId());
            buffer.writeVarInt(value.durationTicks());
            buffer.writeBoolean(value.active());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
