package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WhirlwindAnimationPayload(int entityId, int durationTicks) implements CustomPacketPayload {
    public static final Type<WhirlwindAnimationPayload> TYPE =
            new Type<>(BasicRPGClasses.id("whirlwind_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WhirlwindAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WhirlwindAnimationPayload::entityId,
            ByteBufCodecs.VAR_INT,
            WhirlwindAnimationPayload::durationTicks,
            WhirlwindAnimationPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
