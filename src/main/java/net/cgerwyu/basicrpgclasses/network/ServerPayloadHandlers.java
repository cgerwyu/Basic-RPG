package net.cgerwyu.basicrpgclasses.network;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.PlayerCombatData;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.data.SkillPointCosts;
import net.cgerwyu.basicrpgclasses.data.ClassChangeRules;
import net.cgerwyu.basicrpgclasses.data.ClassResourceRules;
import net.cgerwyu.basicrpgclasses.data.InfiniteResourceManager;
import net.cgerwyu.basicrpgclasses.network.payload.ChooseClassPayload;
import net.cgerwyu.basicrpgclasses.network.payload.PurchaseSkillPointPayload;
import net.cgerwyu.basicrpgclasses.network.payload.RequestClassChangePayload;
import net.cgerwyu.basicrpgclasses.network.payload.OpenClassSelectionPayload;
import net.cgerwyu.basicrpgclasses.network.payload.ActivateSkillSlotPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SetActionBarSlotPayload;
import net.cgerwyu.basicrpgclasses.network.payload.ApplySkillUpgradesPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkillUpgradeResultPayload;
import net.cgerwyu.basicrpgclasses.network.payload.CastHoldPayload;
import net.cgerwyu.basicrpgclasses.network.payload.EquipInventoryItemPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SetCombatModePayload;
import net.cgerwyu.basicrpgclasses.data.PlayerCombatModeData;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinition;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillExecutor;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillRuntimeEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Locale;

public final class ServerPayloadHandlers {
    private static final int SKILL_SLOT_COUNT = 9;

    public static void chooseClass(ChooseClassPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        PlayerClassData current = player.getData(ModAttachments.PLAYER_CLASS);
        RpgClass selected = RpgClass.byId(payload.classId());
        if (current.hasClass() || !selected.playable()) {
            player.sendSystemMessage(Component.translatable("message.basicrpgclasses.class_locked"));
            return;
        }

        player.setData(ModAttachments.PLAYER_CLASS, current.selectClass(selected));
        player.setData(ModAttachments.PLAYER_COMBAT, PlayerCombatData.fresh(selected));
        player.sendSystemMessage(Component.translatable("message.basicrpgclasses.class_selected", Component.translatable(selected.translationKey())));
    }

    public static void purchaseSkillPoint(PurchaseSkillPointPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        PlayerClassData current = player.getData(ModAttachments.PLAYER_CLASS);
        if (!current.hasClass()) {
            player.sendSystemMessage(Component.translatable("message.basicrpgclasses.choose_class_first"));
            return;
        }
        int cost = SkillPointCosts.nextPointCost(current);
        if (player.experienceLevel < cost) {
            player.sendSystemMessage(Component.translatable("message.basicrpgclasses.not_enough_levels", cost));
            return;
        }

        player.giveExperienceLevels(-cost);
        player.setData(ModAttachments.PLAYER_CLASS, current.purchaseSkillPoint(cost));
        player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.skill_point_bought", cost));
    }

    public static void requestClassChange(RequestClassChangePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        PlayerClassData current = player.getData(ModAttachments.PLAYER_CLASS);
        if (!current.hasClass()) {
            player.sendSystemMessage(Component.translatable("message.basicrpgclasses.choose_class_first"));
            return;
        }

        int refund = ClassChangeRules.refundLevels(current);
        SkillRuntimeEffects.clearPlayer(player);
        player.setData(ModAttachments.PLAYER_CLASS, current.resetClass());
        player.setData(ModAttachments.PLAYER_COMBAT, PlayerCombatData.fresh());
        PlayerEquipmentManager.unequipInvalid(player);
        if (refund > 0) {
            player.giveExperienceLevels(refund);
        }

        player.sendSystemMessage(Component.translatable("message.basicrpgclasses.class_reset", refund));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, OpenClassSelectionPayload.INSTANCE);
    }

    public static void activateSkillSlot(ActivateSkillSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (payload.slot() < 0 || payload.slot() >= SKILL_SLOT_COUNT) {
            return;
        }

        PlayerClassData current = player.getData(ModAttachments.PLAYER_CLASS);
        if (!current.hasClass()) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.choose_class_first"));
            return;
        }
        if (!PlayerEquipmentManager.hasMainWeapon(player)) {
            SkillRuntimeEffects.setCastHeld(player, false);
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.weapon_must_be_equipped"));
            return;
        }
        if (SkillRuntimeEffects.groundStunActive(player)) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.stunned"));
            return;
        }

        SkillId skillId = current.skillAtSlot(payload.slot());
        if (skillId == SkillId.NONE) {
            player.sendOverlayMessage(Component.translatable(
                    "message.basicrpgclasses.skill_slot_empty",
                    payload.slot() + 1
            ));
            return;
        }

        SkillDefinition definition = SkillDefinitions.get(skillId);
        int rank = current.skillRank(skillId);
        if (definition == null || skillId.isPassive() || definition.ownerClass() != current.rpgClass() || rank <= 0) {
            return;
        }
        if (mobilitySkill(skillId) && SkillRuntimeEffects.mobilityLocked(player)) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.mobility_locked"));
            return;
        }
        if (SkillRuntimeEffects.ultraThrustCasting(player)) {
            return;
        }
        if (!player.getAbilities().instabuild && SkillRuntimeEffects.globalCooldownActive(player)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        boolean creative = player.getAbilities().instabuild;
        PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
        long remainingTicks = combat.remainingRechargeTicks(definition, rank, gameTime);
        if (!creative && combat.availableCharges(definition, rank, gameTime) <= 0) {
            String remaining = String.format(Locale.ROOT, "%.1f", Math.ceil(remainingTicks / 2.0) / 10.0);
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.skill_on_cooldown", remaining));
            return;
        }
        boolean infiniteResource = InfiniteResourceManager.active(player);
        if (skillId == SkillId.ULTRA_THRUST
                && !infiniteResource
                && combat.resource() < ClassResourceRules.maxResource(current)) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.ultra_thrust_full_fury"));
            return;
        }
        if (!infiniteResource && !combat.canAfford(definition.manaCost(rank))) {
            player.sendOverlayMessage(Component.translatable(
                    "message.basicrpgclasses.not_enough_resource",
                    Component.translatable(ClassResourceRules.nameTranslationKey(current.rpgClass())),
                    definition.manaCost(rank)
            ));
            return;
        }
        if (!SkillExecutor.execute(player, definition, rank)) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.skill_no_valid_effect"));
            return;
        }

        if (skillId != SkillId.CAMOUFLAGE) {
            SkillRuntimeEffects.breakCamouflage(player);
        }
        if (!creative) {
            SkillRuntimeEffects.startGlobalCooldown(player);
        }

        PlayerCombatData updatedCombat = creative
                ? combat
                : combat.useSkill(definition, rank, gameTime, !infiniteResource);
        if (skillId == SkillId.ULTRA_THRUST && !creative && !infiniteResource) {
            updatedCombat = updatedCombat.spendResource(updatedCombat.resource(), gameTime);
        }
        if (skillId == SkillId.MAGIC_SHIELD) {
            updatedCombat = updatedCombat.withMagicShield(gameTime + net.cgerwyu.basicrpgclasses.skill.SkillScaling.magicShieldDurationTicks());
        }
        player.setData(ModAttachments.PLAYER_COMBAT, updatedCombat);
    }

    private static boolean mobilitySkill(SkillId skillId) {
        return skillId == SkillId.BLINK
                || skillId == SkillId.DASH
                || skillId == SkillId.WINDRUN
                || skillId == SkillId.CAMOUFLAGE
                || skillId == SkillId.ULTRA_THRUST
                || skillId == SkillId.WARRIOR_LEAP;
    }

    public static void castHold(CastHoldPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SkillRuntimeEffects.setCastHeld(
                    player,
                    payload.held() && PlayerEquipmentManager.hasMainWeapon(player)
            );
        }
    }

    public static void equipInventoryItem(EquipInventoryItemPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu == player.inventoryMenu) {
            PlayerEquipmentManager.equipFromInventory(player, payload.sourceSlot());
        }
    }

    public static void setCombatMode(SetCombatModePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            player.setData(ModAttachments.PLAYER_COMBAT_MODE, new PlayerCombatModeData(payload.active()));
            PlayerEquipmentManager.refreshWeaponAttributes(player);
        }
    }

    public static void applySkillUpgrades(ApplySkillUpgradesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        PlayerClassData current = player.getData(ModAttachments.PLAYER_CLASS);
        PlayerClassData updated = current.applySkillRanks(payload.desiredRanks());
        if (updated == current) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new SkillUpgradeResultPayload(false));
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.skill_upgrade_denied"));
            return;
        }

        int spent = current.unspentSkillPoints() - updated.unspentSkillPoints();
        player.setData(ModAttachments.PLAYER_CLASS, updated);
        PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
        player.setData(
                ModAttachments.PLAYER_COMBAT,
                combat.reconcileSkillRanks(current, updated, player.level().getGameTime())
        );
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new SkillUpgradeResultPayload(true));
        player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.skill_upgrades_applied", spent));
    }

    public static void setActionBarSlot(SetActionBarSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (payload.slot() < 0 || payload.slot() >= SKILL_SLOT_COUNT) {
            return;
        }

        PlayerClassData current = player.getData(ModAttachments.PLAYER_CLASS);
        SkillId skillId = SkillId.byId(payload.skillId());
        PlayerClassData updated = current.setActionBarSlot(payload.slot(), skillId);
        if (updated == current) {
            player.sendOverlayMessage(Component.translatable("message.basicrpgclasses.skill_assignment_denied"));
            return;
        }

        player.setData(ModAttachments.PLAYER_CLASS, updated);
        player.sendOverlayMessage(Component.translatable(
                "message.basicrpgclasses.skill_assigned",
                Component.translatable(skillId.translationKey()),
                payload.slot() + 1
        ));
    }

    private ServerPayloadHandlers() {
    }
}
