package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SkillUpgradeResultPayload(boolean applied) implements CustomPacketPayload {
    public static final Type<SkillUpgradeResultPayload> TYPE =
            new Type<>(BasicRPGClasses.id("skill_upgrade_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillUpgradeResultPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SkillUpgradeResultPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SkillUpgradeResultPayload(buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SkillUpgradeResultPayload value) {
            buffer.writeBoolean(value.applied());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
