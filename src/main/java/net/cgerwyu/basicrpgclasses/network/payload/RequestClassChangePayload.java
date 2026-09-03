package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestClassChangePayload() implements CustomPacketPayload {
    public static final RequestClassChangePayload INSTANCE = new RequestClassChangePayload();
    public static final Type<RequestClassChangePayload> TYPE = new Type<>(BasicRPGClasses.id("request_class_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestClassChangePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
