package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetCombatModePayload(boolean active) implements CustomPacketPayload {
    public static final Type<SetCombatModePayload> TYPE = new Type<>(BasicRPGClasses.id("set_combat_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCombatModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SetCombatModePayload::active,
            SetCombatModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
