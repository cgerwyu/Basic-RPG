package net.cgerwyu.basicrpgclasses.data;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/** Minimal replicated party state used by the client HUD. The server keeps the member list. */
public record PlayerPartyData(Optional<UUID> partyId) {
    public static final MapCodec<PlayerPartyData> CODEC = UUIDUtil.CODEC.optionalFieldOf("party_id")
            .xmap(PlayerPartyData::new, PlayerPartyData::partyId);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerPartyData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerPartyData decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readBoolean()
                    ? joined(buffer.readUUID())
                    : empty();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PlayerPartyData value) {
            buffer.writeBoolean(value.inParty());
            value.partyId.ifPresent(buffer::writeUUID);
        }
    };

    public PlayerPartyData {
        partyId = partyId == null ? Optional.empty() : partyId;
    }

    public static PlayerPartyData empty() {
        return new PlayerPartyData(Optional.empty());
    }

    public static PlayerPartyData joined(UUID partyId) {
        return new PlayerPartyData(Optional.of(partyId));
    }

    public boolean inParty() {
        return partyId.isPresent();
    }
}
