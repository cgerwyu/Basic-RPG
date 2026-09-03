package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenClassSelectionPayload() implements CustomPacketPayload {
    public static final OpenClassSelectionPayload INSTANCE = new OpenClassSelectionPayload();
    public static final Type<OpenClassSelectionPayload> TYPE = new Type<>(BasicRPGClasses.id("open_class_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenClassSelectionPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
