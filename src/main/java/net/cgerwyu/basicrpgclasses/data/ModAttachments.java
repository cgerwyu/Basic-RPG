package net.cgerwyu.basicrpgclasses.data;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.equipment.PlayerEquipmentData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BasicRPGClasses.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerClassData>> PLAYER_CLASS =
            ATTACHMENTS.register("player_class", () -> AttachmentType.builder(PlayerClassData::unassigned)
                    .serialize(PlayerClassData.CODEC)
                    .copyOnDeath()
                    // Class identity is required on tracking clients for class-specific
                    // third-person visuals, such as hiding a camouflaged Hunter's held items.
                    .sync((holder, receivingPlayer) -> true, PlayerClassData.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCombatData>> PLAYER_COMBAT =
            ATTACHMENTS.register("player_combat", () -> AttachmentType.builder(() -> PlayerCombatData.fresh())
                    .serialize(PlayerCombatData.CODEC)
                    .copyOnDeath()
                    .sync((holder, receivingPlayer) -> holder == receivingPlayer, PlayerCombatData.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerEquipmentData>> PLAYER_EQUIPMENT =
            ATTACHMENTS.register("player_equipment", () -> AttachmentType.builder(PlayerEquipmentData::empty)
                    .serialize(PlayerEquipmentData.CODEC)
                    .copyOnDeath()
                    // Other clients need this to render sheathed weapons on the player's back.
                    .sync((holder, receivingPlayer) -> true, PlayerEquipmentData.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCombatModeData>> PLAYER_COMBAT_MODE =
            ATTACHMENTS.register("player_combat_mode", () -> AttachmentType.builder(PlayerCombatModeData::inactive)
                    .sync((holder, receivingPlayer) -> true, PlayerCombatModeData.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerPartyData>> PLAYER_PARTY =
            ATTACHMENTS.register("player_party", () -> AttachmentType.builder(PlayerPartyData::empty)
                    .serialize(PlayerPartyData.CODEC)
                    .copyOnDeath()
                    // Party identity is displayed for other members in the client HUD.
                    .sync((holder, receivingPlayer) -> true, PlayerPartyData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }

    private ModAttachments() {
    }
}
