package net.cgerwyu.basicrpgclasses;

import net.cgerwyu.basicrpgclasses.client.ClientEvents;
import net.cgerwyu.basicrpgclasses.client.ClientPayloadHandlers;
import net.cgerwyu.basicrpgclasses.client.ClientSkillVisuals;
import net.cgerwyu.basicrpgclasses.client.ClientSkillVfx;
import net.cgerwyu.basicrpgclasses.client.ClientEquipmentScreenEvents;
import net.cgerwyu.basicrpgclasses.registry.ModEntities;
import net.cgerwyu.basicrpgclasses.client.render.MageFireballRenderer;
import net.cgerwyu.basicrpgclasses.client.render.BackWeaponLayer;
import net.cgerwyu.basicrpgclasses.client.render.FirstfangPoisonAuraLayer;
import net.cgerwyu.basicrpgclasses.mixin.client.LivingEntityRendererAccessor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = BasicRPGClasses.MODID, dist = Dist.CLIENT)
public final class BasicRPGClassesClient {
    public BasicRPGClassesClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(ClientEvents::registerKeyMappings);
        modEventBus.addListener(ClientEvents::registerGuiLayers);
        modEventBus.addListener(ClientPayloadHandlers::register);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::addPlayerLayers);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onKeyInput);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderGuiLayerPre);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onCustomizeBossEvent);
        NeoForge.EVENT_BUS.addListener(ClientSkillVisuals::onRenderPlayerPre);
        NeoForge.EVENT_BUS.addListener(ClientSkillVisuals::onRenderPlayerPost);
        NeoForge.EVENT_BUS.addListener(ClientSkillVfx::onExtractLevelRenderState);
        NeoForge.EVENT_BUS.addListener(ClientSkillVfx::onSubmitCustomGeometry);
        NeoForge.EVENT_BUS.addListener(ClientEquipmentScreenEvents::onMousePressed);
        NeoForge.EVENT_BUS.addListener(ClientEquipmentScreenEvents::onRenderForeground);

        BasicRPGClasses.LOGGER.info("Basic RPG Classes client initialized");
    }

    private void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            ArmorModelSet<HumanoidModel<AvatarRenderState>> armorModels = ArmorModelSet.bake(
                    skin == PlayerModelType.SLIM ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
                    event.getEntityModels(),
                    HumanoidModel::new
            );
            ((LivingEntityRendererAccessor) renderer).basicrpgclasses$addLayer(
                    new FirstfangPoisonAuraLayer(renderer, armorModels)
            );
            ((LivingEntityRendererAccessor) renderer).basicrpgclasses$addLayer(new BackWeaponLayer(renderer));
        }
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.MAGE_FIREBALL.get(),
                MageFireballRenderer::new
        );
    }
}
