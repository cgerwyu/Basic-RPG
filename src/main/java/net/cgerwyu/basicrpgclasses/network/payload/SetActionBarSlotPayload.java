package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetActionBarSlotPayload(int slot, int skillId) implements CustomPacketPayload {
    public static final Type<SetActionBarSlotPayload> TYPE = new Type<>(BasicRPGClasses.id("set_action_bar_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetActionBarSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetActionBarSlotPayload::slot,
            ByteBufCodecs.VAR_INT,
            SetActionBarSlotPayload::skillId,
            SetActionBarSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
