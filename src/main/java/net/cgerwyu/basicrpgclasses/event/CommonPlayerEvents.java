package net.cgerwyu.basicrpgclasses.event;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.InfiniteResourceManager;
import net.cgerwyu.basicrpgclasses.network.payload.OpenClassSelectionPayload;
import net.cgerwyu.basicrpgclasses.skill.SkillRuntimeEffects;
import net.cgerwyu.basicrpgclasses.combat.PvpBalance;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.cgerwyu.basicrpgclasses.party.PartyService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BasicRPGClasses.MODID)
public final class CommonPlayerEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillRuntimeEffects.clearPlayer(player);
            PvpBalance.clearPlayer(player);
            PvpBalance.grantLoginProtection(player);
            PartyService.reconcileMembership(player);
            PlayerClassData data = player.getData(ModAttachments.PLAYER_CLASS);
            player.syncData(ModAttachments.PLAYER_CLASS);
            player.syncData(ModAttachments.PLAYER_COMBAT);
            player.syncData(ModAttachments.PLAYER_EQUIPMENT);
            player.syncData(ModAttachments.PLAYER_COMBAT_MODE);
            player.syncData(ModAttachments.PLAYER_PARTY);
            PlayerEquipmentManager.refreshWeaponAttributes(player);
            if (!data.hasClass()) {
                PacketDistributor.sendToPlayer(player, OpenClassSelectionPayload.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillRuntimeEffects.clearPlayer(player);
            PvpBalance.clearPlayer(player);
            PvpBalance.grantRespawnProtection(player);
            player.syncData(ModAttachments.PLAYER_CLASS);
            player.syncData(ModAttachments.PLAYER_COMBAT);
            player.syncData(ModAttachments.PLAYER_EQUIPMENT);
            player.syncData(ModAttachments.PLAYER_COMBAT_MODE);
            PartyService.reconcileMembership(player);
            player.syncData(ModAttachments.PLAYER_PARTY);
            PlayerEquipmentManager.refreshWeaponAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillRuntimeEffects.clearPlayer(player);
            PvpBalance.clearPlayer(player);
            player.syncData(ModAttachments.PLAYER_CLASS);
            player.syncData(ModAttachments.PLAYER_COMBAT);
            player.syncData(ModAttachments.PLAYER_EQUIPMENT);
            player.syncData(ModAttachments.PLAYER_COMBAT_MODE);
            PartyService.reconcileMembership(player);
            player.syncData(ModAttachments.PLAYER_PARTY);
            PlayerEquipmentManager.refreshWeaponAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillRuntimeEffects.clearPlayer(player);
            PvpBalance.clearPlayer(player);
            InfiniteResourceManager.clear(player);
        }
    }

    private CommonPlayerEvents() {
    }
}
