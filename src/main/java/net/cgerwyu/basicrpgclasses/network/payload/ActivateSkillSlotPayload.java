package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ActivateSkillSlotPayload(int slot) implements CustomPacketPayload {
    public static final Type<ActivateSkillSlotPayload> TYPE = new Type<>(BasicRPGClasses.id("activate_skill_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateSkillSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ActivateSkillSlotPayload::slot,
            ActivateSkillSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
