package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChooseClassPayload(int classId) implements CustomPacketPayload {
    public static final Type<ChooseClassPayload> TYPE = new Type<>(BasicRPGClasses.id("choose_class"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseClassPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ChooseClassPayload::classId,
            ChooseClassPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
