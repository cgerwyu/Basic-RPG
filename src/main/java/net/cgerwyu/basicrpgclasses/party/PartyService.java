package net.cgerwyu.basicrpgclasses.party;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerPartyData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Commands and combat rules use this single server-authoritative party service. */
public final class PartyService {
    public static final int MAX_MEMBERS = 5;
    private static final long INVITE_DURATION_TICKS = 60L * 20L;
    private static final Map<UUID, PendingInvite> PENDING_INVITES = new HashMap<>();

    public static boolean inParty(Player player) {
        return player.getData(ModAttachments.PLAYER_PARTY).inParty();
    }

    /** Works on both sides because the party id is replicated through PLAYER_PARTY. */
    public static boolean sameParty(Player first, Player second) {
        if (first == second) {
            return true;
        }
        Optional<UUID> firstParty = first.getData(ModAttachments.PLAYER_PARTY).partyId();
        Optional<UUID> secondParty = second.getData(ModAttachments.PLAYER_PARTY).partyId();
        return firstParty.isPresent() && firstParty.equals(secondParty);
    }

    /** Uses saved data instead of a potentially stale replicated attachment during server decisions. */
    public static boolean sameParty(ServerPlayer first, ServerPlayer second) {
        if (first == second) {
            return true;
        }
        PartySavedData data = PartySavedData.get(first.level().getServer());
        Optional<PartySavedData.PartyLookup> party = data.findByMember(first.getUUID());
        return party.isPresent() && party.get().party().memberIds().contains(second.getUUID());
    }

    public static void reconcileMembership(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.level().getServer());
        Optional<PartySavedData.PartyLookup> party = data.findByMember(player.getUUID());
        syncMembership(player, party.map(PartySavedData.PartyLookup::partyId));
    }

    public static boolean invite(ServerPlayer inviter, ServerPlayer invited) {
        MinecraftServer server = inviter.level().getServer();
        removeExpiredInvites(server);
        if (inviter == invited) {
            inviter.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.cannot_invite_self"));
            return false;
        }
        if (findParty(invited).isPresent()) {
            inviter.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.target_already_in_party", invited.getDisplayName()));
            return false;
        }

        Optional<PartySavedData.PartyLookup> inviterParty = findParty(inviter);
        if (inviterParty.isPresent()) {
            PartySavedData.PartyState party = inviterParty.get().party();
            if (!party.leaderId().equals(inviter.getUUID())) {
                inviter.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.not_leader"));
                return false;
            }
            if (party.memberIds().size() >= MAX_MEMBERS) {
                inviter.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.full"));
                return false;
            }
        }

        PENDING_INVITES.put(invited.getUUID(), new PendingInvite(inviter.getUUID(), now(server) + INVITE_DURATION_TICKS));
        inviter.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.invite_sent", invited.getDisplayName()));
        invited.sendSystemMessage(Component.translatable(
                "message.basicrpgclasses.party.invite_received",
                inviter.getDisplayName(), inviter.getName(), inviter.getName()
        ));
        return true;
    }

    public static boolean accept(ServerPlayer invited, ServerPlayer inviter) {
        MinecraftServer server = invited.level().getServer();
        PendingInvite invite = PENDING_INVITES.remove(invited.getUUID());
        if (invite == null || !invite.inviterId().equals(inviter.getUUID()) || invite.expiresAt() <= now(server)) {
            invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.invite_missing"));
            return false;
        }
        if (findParty(invited).isPresent()) {
            invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.already_in_party"));
            return false;
        }
        if (!inviter.isAlive()) {
            invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.inviter_unavailable"));
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyLookup partyLookup = findParty(inviter).orElse(null);
        UUID partyId;
        PartySavedData.PartyState party;
        if (partyLookup == null) {
            partyId = data.newPartyId();
            party = data.createParty(inviter.getUUID());
        } else {
            partyId = partyLookup.partyId();
            party = partyLookup.party();
            if (!party.leaderId().equals(inviter.getUUID())) {
                invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.inviter_unavailable"));
                return false;
            }
        }
        if (party.memberIds().size() >= MAX_MEMBERS) {
            invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.full"));
            return false;
        }

        party = party.withMember(invited.getUUID());
        data.put(partyId, party);
        syncParty(server, partyId, party);
        inviter.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.member_joined", invited.getDisplayName()));
        invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.joined", inviter.getDisplayName()));
        return true;
    }

    public static boolean deny(ServerPlayer invited, ServerPlayer inviter) {
        MinecraftServer server = invited.level().getServer();
        PendingInvite invite = PENDING_INVITES.get(invited.getUUID());
        if (invite == null || !invite.inviterId().equals(inviter.getUUID()) || invite.expiresAt() <= now(server)) {
            invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.invite_missing"));
            return false;
        }
        PENDING_INVITES.remove(invited.getUUID());
        invited.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.invite_denied"));
        inviter.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.invite_declined", invited.getDisplayName()));
        return true;
    }

    public static boolean leave(ServerPlayer player) {
        PartySavedData.PartyLookup party = findParty(player).orElse(null);
        if (party == null) {
            player.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.not_in_party"));
            return false;
        }
        removeMember(player.level().getServer(), party, player.getUUID());
        discardInvitesFrom(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.left"));
        return true;
    }

    public static boolean kick(ServerPlayer leader, ServerPlayer target) {
        if (leader == target) {
            leader.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.cannot_kick_self"));
            return false;
        }
        PartySavedData.PartyLookup party = findParty(leader).orElse(null);
        if (party == null) {
            leader.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.not_in_party"));
            return false;
        }
        if (!party.party().leaderId().equals(leader.getUUID())) {
            leader.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.not_leader"));
            return false;
        }
        if (!party.party().memberIds().contains(target.getUUID())) {
            leader.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.target_not_in_party", target.getDisplayName()));
            return false;
        }

        removeMember(leader.level().getServer(), party, target.getUUID());
        discardInvitesFrom(target.getUUID());
        leader.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.kicked", target.getDisplayName()));
        target.sendSystemMessage(Component.translatable("message.basicrpgclasses.party.you_were_kicked", leader.getDisplayName()));
        return true;
    }

    private static Optional<PartySavedData.PartyLookup> findParty(ServerPlayer player) {
        return PartySavedData.get(player.level().getServer()).findByMember(player.getUUID());
    }

    private static void removeMember(MinecraftServer server, PartySavedData.PartyLookup partyLookup, UUID memberId) {
        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyState updated = partyLookup.party().withoutMember(memberId);
        if (updated.memberIds().isEmpty()) {
            data.remove(partyLookup.partyId());
        } else {
            if (updated.leaderId().equals(memberId)) {
                UUID nextLeader = updated.memberIds().stream().min(Comparator.comparing(UUID::toString)).orElseThrow();
                updated = updated.withLeader(nextLeader);
            }
            data.put(partyLookup.partyId(), updated);
            syncParty(server, partyLookup.partyId(), updated);
        }
        ServerPlayer removedPlayer = server.getPlayerList().getPlayer(memberId);
        if (removedPlayer != null) {
            syncMembership(removedPlayer, Optional.empty());
        }
    }

    private static void syncParty(MinecraftServer server, UUID partyId, PartySavedData.PartyState party) {
        for (UUID memberId : party.memberIds()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                syncMembership(member, Optional.of(partyId));
            }
        }
    }

    private static void syncMembership(ServerPlayer player, Optional<UUID> partyId) {
        PlayerPartyData next = partyId.map(PlayerPartyData::joined).orElseGet(PlayerPartyData::empty);
        if (!next.equals(player.getData(ModAttachments.PLAYER_PARTY))) {
            player.setData(ModAttachments.PLAYER_PARTY, next);
        }
        player.syncData(ModAttachments.PLAYER_PARTY);
    }

    private static void discardInvitesFrom(UUID inviterId) {
        PENDING_INVITES.entrySet().removeIf(entry -> entry.getValue().inviterId().equals(inviterId));
    }

    private static void removeExpiredInvites(MinecraftServer server) {
        long now = now(server);
        PENDING_INVITES.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private record PendingInvite(UUID inviterId, long expiresAt) {
    }

    private PartyService() {
    }
}
