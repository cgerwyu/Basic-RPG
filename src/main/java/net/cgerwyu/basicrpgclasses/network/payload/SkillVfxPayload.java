package net.cgerwyu.basicrpgclasses.network.payload;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Describes a client-only animated VFX primitive. entityId is -1 for a fixed
 * world effect and a live entity id for an effect which follows its owner.
 */
public record SkillVfxPayload(
        int effectId,
        int entityId,
        double x,
        double y,
        double z,
        double endX,
        double endY,
        double endZ,
        int color,
        float scale,
        int durationTicks
) implements CustomPacketPayload {
    public static final Type<SkillVfxPayload> TYPE = new Type<>(BasicRPGClasses.id("skill_vfx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillVfxPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SkillVfxPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new SkillVfxPayload(
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readInt(),
                            buffer.readFloat(),
                            buffer.readVarInt()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SkillVfxPayload payload) {
                    buffer.writeVarInt(payload.effectId());
                    buffer.writeVarInt(payload.entityId());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeDouble(payload.endX());
                    buffer.writeDouble(payload.endY());
                    buffer.writeDouble(payload.endZ());
                    buffer.writeInt(payload.color());
                    buffer.writeFloat(payload.scale());
                    buffer.writeVarInt(payload.durationTicks());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
