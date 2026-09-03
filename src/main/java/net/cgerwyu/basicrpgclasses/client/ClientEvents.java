package net.cgerwyu.basicrpgclasses.client;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.client.gui.ClassProgressionScreen;
import net.cgerwyu.basicrpgclasses.client.gui.ClassSelectionScreen;
import net.cgerwyu.basicrpgclasses.client.hud.RpgHud;
import net.cgerwyu.basicrpgclasses.client.hud.SkillHudAnimations;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentManager;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.cgerwyu.basicrpgclasses.skill.SkillVfxType;
import net.cgerwyu.basicrpgclasses.network.payload.SkillVfxPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class ClientEvents {
    private static net.minecraft.client.gui.GuiGraphicsExtractor bossGraphics;
    private static int bossBarIndex;

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(ModKeyMappings.CATEGORY);
        event.register(ModKeyMappings.OPEN_PROGRESSION);
        event.register(ModKeyMappings.TOGGLE_COMBAT_MODE);
        event.register(ModKeyMappings.ACTIVATE_SELECTED_SKILL);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, BasicRPGClasses.id("rpg_hud"), RpgHud::render);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            CombatModeController.deactivate();
            SkillHudAnimations.reset();
            ClientToggleSkillStates.clear();
            ClientCastState.clear();
            return;
        }
        if (!minecraft.player.isAlive()) {
            CombatModeController.deactivate();
            return;
        }

        while (ModKeyMappings.OPEN_PROGRESSION.consumeClick()) {
            PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
            minecraft.gui.setScreen(data.hasClass() ? new ClassProgressionScreen() : new ClassSelectionScreen());
        }

        while (ModKeyMappings.TOGGLE_COMBAT_MODE.consumeClick()) {
            if (minecraft.gui.screen() == null) {
                CombatModeController.toggle();
                minecraft.player.sendSystemMessage(Component.translatable(
                        CombatModeController.active()
                                ? "message.basicrpgclasses.combat_mode_on"
                                : "message.basicrpgclasses.combat_mode_off"
                ));
            }
        }

        while (ModKeyMappings.ACTIVATE_SELECTED_SKILL.consumeClick()) {
            if (capturesSkillBarInput(minecraft)) {
                PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
                if (!usesRightMouseCasting(data.rpgClass())) {
                    CombatModeController.activateSelectedSkill();
                }
            }
        }


        if (capturesSkillBarInput(minecraft)) {
            renderTargetingPreview(minecraft);
            boolean held = usesRightMouseCasting(minecraft.player.getData(ModAttachments.PLAYER_CLASS).rpgClass())
                    && minecraft.options.keyUse.isDown()
                    && PlayerEquipmentManager.hasMainWeapon(minecraft.player);
            CombatModeController.setCastHeld(held);
        } else if (CombatModeController.castHeld()) {
            CombatModeController.setCastHeld(false);
        }
    }

    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!capturesSkillBarInput(minecraft)) {
            return;
        }

        for (int slot = 0; slot < CombatModeController.SKILL_SLOT_COUNT; slot++) {
            KeyMapping hotbarKey = minecraft.options.keyHotbarSlots[slot];
            if (!hotbarKey.matches(event.getKeyEvent())) {
                continue;
            }

            // InputEvent.Key fires after vanilla has queued the KeyMapping click. Remove the
            // queued item-hotbar click immediately; the client mixin is a second safety net.
            while (hotbarKey.consumeClick()) {
                // Drain every repeat accumulated before the next client tick.
            }
            hotbarKey.setDown(false);

            if (event.getAction() == InputConstants.PRESS) {
                CombatModeController.selectSkillSlot(slot);
                PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
                if (!usesRightMouseCasting(data.rpgClass())) {
                    CombatModeController.activateSelectedSkill();
                }
            }
            return;
        }
    }

    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!capturesSkillBarInput(minecraft) || !event.isUseItem()) {
            return;
        }

        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        if (!usesRightMouseCasting(data.rpgClass())) {
            return;
        }

        event.setCanceled(true);
        boolean mainHand = event.getHand() == InteractionHand.MAIN_HAND;
        event.setSwingHand(false);
        if (mainHand) {
            CombatModeController.setCastHeld(PlayerEquipmentManager.hasMainWeapon(minecraft.player));
            CombatModeController.activateSelectedSkill();
        }
    }

    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!capturesSkillBarInput(minecraft)) {
            return;
        }

        int wheel = event.getAccumulatedScrollY() == 0
                ? -event.getAccumulatedScrollX()
                : event.getAccumulatedScrollY();
        if (wheel == 0) {
            return;
        }

        int nextSlot = ScrollWheelHandler.getNextScrollWheelSelection(
                wheel,
                CombatModeController.selectedSkillSlot(),
                CombatModeController.SKILL_SLOT_COUNT
        );
        CombatModeController.selectSkillSlot(nextSlot);
        event.setCanceled(true);
    }

    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!CombatModeController.active()) {
            return;
        }

        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)
                || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) {
            event.setCanceled(true);
        }
    }

    /** Replaces each vanilla boss bar with the gothic RPG treatment without changing gameplay data. */
    public static void onCustomizeBossEvent(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (bossGraphics != event.getGuiGraphics()) {
            bossGraphics = event.getGuiGraphics();
            bossBarIndex = 0;
        }

        int y = 8 + bossBarIndex * 40;
        bossBarIndex++;
        if (y + 34 <= event.getGuiGraphics().guiHeight() / 3) {
            RpgHud.renderBossBar(event.getGuiGraphics(), Minecraft.getInstance(), event.getBossEvent(), y);
        }
        event.setCanceled(true);
    }

    private static boolean capturesSkillBarInput(Minecraft minecraft) {
        return CombatModeController.active()
                && minecraft.player != null
                && !minecraft.player.isSpectator()
                && minecraft.level != null
                && minecraft.gui.screen() == null
                && minecraft.gui.overlay() == null;
    }

    private static boolean usesRightMouseCasting(RpgClass rpgClass) {
        return rpgClass == RpgClass.MAGE || rpgClass == RpgClass.PRIEST;
    }

    private static void renderTargetingPreview(Minecraft minecraft) {
        if (minecraft.player.tickCount % 6 != 0
                || ClientCastState.active()
                || !PlayerEquipmentManager.hasMainWeapon(minecraft.player)) {
            return;
        }
        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        SkillId skill = data.skillAtSlot(CombatModeController.selectedSkillSlot());
        int rank = data.skillRank(skill);
        float radius;
        int color;
        if (skill == SkillId.METEOR) {
            radius = (float) SkillScaling.meteorRadius(rank);
            color = 0xFF5A18;
        } else if (skill == SkillId.SKY_RAYS) {
            radius = (float) SkillScaling.skyRaysRadius(rank);
            color = 0xFFFFD35A;
        } else if (skill == SkillId.HOLY_STORM) {
            radius = (float) SkillScaling.holyRadius(rank);
            color = 0xFFF1A8;
        } else {
            return;
        }
        double range = skill == SkillId.METEOR ? SkillScaling.meteorCastRange(rank) : 36.0;
        Vec3 point = aimedGroundPreview(minecraft, range);
        ClientSkillVfx.addEffect(new SkillVfxPayload(
                SkillVfxType.TARGET_RING.id(), -1,
                point.x, point.y, point.z,
                point.x, point.y, point.z,
                color, radius, 8
        ));
    }

    /** Mirrors SkillExecutor.aimedGroundPoint so the preview and the server use one point. */
    private static Vec3 aimedGroundPreview(Minecraft minecraft, double range) {
        Vec3 start = minecraft.player.getEyePosition();
        Vec3 end = start.add(minecraft.player.getLookAngle().normalize().scale(range));
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player
        ));
        Vec3 aimed = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        BlockPos startPos = BlockPos.containing(aimed);
        for (int down = 0; down <= 18; down++) {
            BlockPos candidate = startPos.below(down);
            if (!minecraft.level.getBlockState(candidate).isAir()) {
                return new Vec3(aimed.x, candidate.getY() + 1.05, aimed.z);
            }
        }
        return aimed;
    }

    private ClientEvents() {
    }
}
