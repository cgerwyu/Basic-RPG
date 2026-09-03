package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CastHoldPayload(boolean held) implements CustomPacketPayload {
    public static final Type<CastHoldPayload> TYPE = new Type<>(BasicRPGClasses.id("cast_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastHoldPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CastHoldPayload decode(RegistryFriendlyByteBuf buffer) {
            return new CastHoldPayload(buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CastHoldPayload value) {
            buffer.writeBoolean(value.held());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
