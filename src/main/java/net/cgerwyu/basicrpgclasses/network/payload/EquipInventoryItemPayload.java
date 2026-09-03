package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EquipInventoryItemPayload(int sourceSlot) implements CustomPacketPayload {
    public static final Type<EquipInventoryItemPayload> TYPE = new Type<>(BasicRPGClasses.id("equip_inventory_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipInventoryItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EquipInventoryItemPayload::sourceSlot,
            EquipInventoryItemPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
