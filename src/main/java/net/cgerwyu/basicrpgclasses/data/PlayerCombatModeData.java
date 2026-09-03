package net.cgerwyu.basicrpgclasses.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlayerCombatModeData(boolean active) {
    public static final MapCodec<PlayerCombatModeData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("active", false).forGetter(PlayerCombatModeData::active)
    ).apply(instance, PlayerCombatModeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerCombatModeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PlayerCombatModeData::active,
            PlayerCombatModeData::new
    );

    public static PlayerCombatModeData inactive() {
        return new PlayerCombatModeData(false);
    }
}
