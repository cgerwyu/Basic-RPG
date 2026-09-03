package net.cgerwyu.basicrpgclasses.party;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistent, server-authoritative party roster. */
public final class PartySavedData extends SavedData {
    private static final Codec<PartyState> PARTY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("leader").forGetter(PartyState::leaderId),
            UUIDUtil.CODEC_SET.fieldOf("members").forGetter(PartyState::memberIds)
    ).apply(instance, PartyState::new));

    public static final Codec<PartySavedData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, PARTY_CODEC)
            .xmap(PartySavedData::new, data -> data.parties);
    public static final SavedDataType<PartySavedData> TYPE = new SavedDataType<>(
            BasicRPGClasses.id("parties"), PartySavedData::new, CODEC
    );

    private final Map<UUID, PartyState> parties;

    public PartySavedData() {
        this(Map.of());
    }

    private PartySavedData(Map<UUID, PartyState> parties) {
        this.parties = new HashMap<>(parties);
    }

    public static PartySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<PartyLookup> findByMember(UUID playerId) {
        return parties.entrySet().stream()
                .filter(entry -> entry.getValue().memberIds.contains(playerId))
                .findFirst()
                .map(entry -> new PartyLookup(entry.getKey(), entry.getValue()));
    }

    public PartyState createParty(UUID leaderId) {
        Set<UUID> members = new HashSet<>();
        members.add(leaderId);
        return new PartyState(leaderId, members);
    }

    public UUID newPartyId() {
        UUID partyId;
        do {
            partyId = UUID.randomUUID();
        } while (parties.containsKey(partyId));
        return partyId;
    }

    public void put(UUID partyId, PartyState party) {
        parties.put(partyId, party);
        setDirty();
    }

    public void remove(UUID partyId) {
        if (parties.remove(partyId) != null) {
            setDirty();
        }
    }

    public record PartyLookup(UUID partyId, PartyState party) {
    }

    public record PartyState(UUID leaderId, Set<UUID> memberIds) {
        public PartyState {
            memberIds = Set.copyOf(memberIds);
        }

        public PartyState withMember(UUID memberId) {
            Set<UUID> members = new HashSet<>(memberIds);
            members.add(memberId);
            return new PartyState(leaderId, members);
        }

        public PartyState withoutMember(UUID memberId) {
            Set<UUID> members = new HashSet<>(memberIds);
            members.remove(memberId);
            return new PartyState(leaderId, members);
        }

        public PartyState withLeader(UUID nextLeaderId) {
            return new PartyState(nextLeaderId, memberIds);
        }
    }
}
