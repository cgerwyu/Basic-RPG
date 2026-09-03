package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SkyRayVfxPayload(double x, double y, double z, int durationTicks) implements CustomPacketPayload {
    public static final Type<SkyRayVfxPayload> TYPE = new Type<>(BasicRPGClasses.id("sky_ray_vfx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkyRayVfxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            SkyRayVfxPayload::x,
            ByteBufCodecs.DOUBLE,
            SkyRayVfxPayload::y,
            ByteBufCodecs.DOUBLE,
            SkyRayVfxPayload::z,
            ByteBufCodecs.VAR_INT,
            SkyRayVfxPayload::durationTicks,
            SkyRayVfxPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
