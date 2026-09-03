package net.cgerwyu.basicrpgclasses.network;

import net.cgerwyu.basicrpgclasses.network.payload.ChooseClassPayload;
import net.cgerwyu.basicrpgclasses.network.payload.OpenClassSelectionPayload;
import net.cgerwyu.basicrpgclasses.network.payload.PurchaseSkillPointPayload;
import net.cgerwyu.basicrpgclasses.network.payload.RequestClassChangePayload;
import net.cgerwyu.basicrpgclasses.network.payload.ActivateSkillSlotPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SetActionBarSlotPayload;
import net.cgerwyu.basicrpgclasses.network.payload.ApplySkillUpgradesPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkillUpgradeResultPayload;
import net.cgerwyu.basicrpgclasses.network.payload.WhirlwindAnimationPayload;
import net.cgerwyu.basicrpgclasses.network.payload.ToggleSkillStatePayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkyRayVfxPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkillVfxPayload;
import net.cgerwyu.basicrpgclasses.network.payload.CastHoldPayload;
import net.cgerwyu.basicrpgclasses.network.payload.CastStatePayload;
import net.cgerwyu.basicrpgclasses.network.payload.EquipInventoryItemPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SetCombatModePayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("13");
        registrar.playToServer(ChooseClassPayload.TYPE, ChooseClassPayload.STREAM_CODEC, ServerPayloadHandlers::chooseClass);
        registrar.playToServer(PurchaseSkillPointPayload.TYPE, PurchaseSkillPointPayload.STREAM_CODEC, ServerPayloadHandlers::purchaseSkillPoint);
        registrar.playToServer(RequestClassChangePayload.TYPE, RequestClassChangePayload.STREAM_CODEC, ServerPayloadHandlers::requestClassChange);
        registrar.playToServer(ActivateSkillSlotPayload.TYPE, ActivateSkillSlotPayload.STREAM_CODEC, ServerPayloadHandlers::activateSkillSlot);
        registrar.playToServer(SetActionBarSlotPayload.TYPE, SetActionBarSlotPayload.STREAM_CODEC, ServerPayloadHandlers::setActionBarSlot);
        registrar.playToServer(ApplySkillUpgradesPayload.TYPE, ApplySkillUpgradesPayload.STREAM_CODEC, ServerPayloadHandlers::applySkillUpgrades);
        registrar.playToServer(CastHoldPayload.TYPE, CastHoldPayload.STREAM_CODEC, ServerPayloadHandlers::castHold);
        registrar.playToServer(EquipInventoryItemPayload.TYPE, EquipInventoryItemPayload.STREAM_CODEC, ServerPayloadHandlers::equipInventoryItem);
        registrar.playToServer(SetCombatModePayload.TYPE, SetCombatModePayload.STREAM_CODEC, ServerPayloadHandlers::setCombatMode);
        registrar.playToClient(OpenClassSelectionPayload.TYPE, OpenClassSelectionPayload.STREAM_CODEC);
        registrar.playToClient(SkillUpgradeResultPayload.TYPE, SkillUpgradeResultPayload.STREAM_CODEC);
        registrar.playToClient(WhirlwindAnimationPayload.TYPE, WhirlwindAnimationPayload.STREAM_CODEC);
        registrar.playToClient(ToggleSkillStatePayload.TYPE, ToggleSkillStatePayload.STREAM_CODEC);
        registrar.playToClient(SkyRayVfxPayload.TYPE, SkyRayVfxPayload.STREAM_CODEC);
        registrar.playToClient(SkillVfxPayload.TYPE, SkillVfxPayload.STREAM_CODEC);
        registrar.playToClient(CastStatePayload.TYPE, CastStatePayload.STREAM_CODEC);
    }

    private ModPayloads() {
    }
}
