package net.cgerwyu.basicrpgclasses.client;

import net.cgerwyu.basicrpgclasses.client.gui.ClassSelectionScreen;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.network.payload.OpenClassSelectionPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkillUpgradeResultPayload;
import net.cgerwyu.basicrpgclasses.network.payload.WhirlwindAnimationPayload;
import net.cgerwyu.basicrpgclasses.network.payload.ToggleSkillStatePayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkyRayVfxPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SkillVfxPayload;
import net.cgerwyu.basicrpgclasses.network.payload.CastStatePayload;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.client.gui.ClassProgressionScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandlers {
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenClassSelectionPayload.TYPE, ClientPayloadHandlers::openClassSelection);
        event.register(SkillUpgradeResultPayload.TYPE, ClientPayloadHandlers::skillUpgradeResult);
        event.register(WhirlwindAnimationPayload.TYPE, ClientPayloadHandlers::whirlwindAnimation);
        event.register(ToggleSkillStatePayload.TYPE, ClientPayloadHandlers::toggleSkillState);
        event.register(SkyRayVfxPayload.TYPE, ClientPayloadHandlers::skyRayVfx);
        event.register(SkillVfxPayload.TYPE, ClientPayloadHandlers::skillVfx);
        event.register(CastStatePayload.TYPE, ClientPayloadHandlers::castState);
    }

    private static void skillVfx(SkillVfxPayload payload, IPayloadContext context) {
        ClientSkillVfx.addEffect(payload);
    }

    private static void castState(CastStatePayload payload, IPayloadContext context) {
        if (payload.active()) {
            ClientCastState.start(SkillId.byId(payload.skillId()), payload.durationTicks());
        } else {
            ClientCastState.clear();
        }
    }

    private static void skyRayVfx(SkyRayVfxPayload payload, IPayloadContext context) {
        ClientSkillVfx.addSkyRay(payload);
    }

    private static void toggleSkillState(ToggleSkillStatePayload payload, IPayloadContext context) {
        ClientToggleSkillStates.set(SkillId.byId(payload.skillId()), payload.active());
    }

    private static void whirlwindAnimation(WhirlwindAnimationPayload payload, IPayloadContext context) {
        ClientSkillVisuals.startWhirlwind(payload.entityId(), payload.durationTicks());
    }

    private static void skillUpgradeResult(SkillUpgradeResultPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().gui.screen() instanceof ClassProgressionScreen screen) {
            screen.handleUpgradeResult(payload.applied());
        }
    }

    private static void openClassSelection(OpenClassSelectionPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !minecraft.player.getData(ModAttachments.PLAYER_CLASS).hasClass()) {
            minecraft.gui.setScreen(new ClassSelectionScreen());
        }
    }

    private ClientPayloadHandlers() {
    }
}
