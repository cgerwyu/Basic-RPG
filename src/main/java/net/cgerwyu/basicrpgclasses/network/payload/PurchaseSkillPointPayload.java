package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PurchaseSkillPointPayload() implements CustomPacketPayload {
    public static final PurchaseSkillPointPayload INSTANCE = new PurchaseSkillPointPayload();
    public static final Type<PurchaseSkillPointPayload> TYPE = new Type<>(BasicRPGClasses.id("purchase_skill_point"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseSkillPointPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
