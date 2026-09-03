package net.cgerwyu.basicrpgclasses.event;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.data.ClassResourceRules;
import net.cgerwyu.basicrpgclasses.data.InfiniteResourceManager;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.skill.SkillParticleEffects;
import net.cgerwyu.basicrpgclasses.skill.SkillVfxType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BasicRPGClasses.MODID)
public final class ModCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("brc_infinite_mana")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> set(context.getSource().getPlayerOrException(), null))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> set(
                                context.getSource().getPlayerOrException(),
                                BoolArgumentType.getBool(context, "enabled")
                        ))));
        event.getDispatcher().register(Commands.literal("brc_test_vfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> showcase(context.getSource().getPlayerOrException())));
    }

    private static int showcase(ServerPlayer player) {
        var level = player.level();
        Vec3 center = player.position();
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-5) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        SkillParticleEffects.attached(level, SkillVfxType.SHIELD, player, Vec3.ZERO,
                0x5EBBFF, 1.7F, 100);
        SkillParticleEffects.attached(level, SkillVfxType.SLASH_ORBIT, player, Vec3.ZERO,
                0xFFD76A, 2.6F, 50);
        SkillParticleEffects.healingField(level, center.add(right.scale(5.0)), 3.2F, 80);
        SkillParticleEffects.frostField(level, center.add(right.scale(-5.0)), 3.2F, 80);
        SkillParticleEffects.lightningArc(level, center.add(0.0, 1.2, 0.0),
                center.add(forward.scale(10.0)).add(0.0, 1.2, 0.0));
        SkillParticleEffects.skyRayCross(level, center.add(forward.scale(13.0)));
        SkillParticleEffects.meteorFlightPath(level,
                center.add(right.scale(8.0)).add(0.0, 14.0, 0.0),
                center.add(right.scale(8.0)), 10, 50);
        SkillParticleEffects.groundTremor(level, center, forward, 8.0, 45.0);
        player.sendSystemMessage(Component.literal("Basic RPG Classes: VFX showcase started"));
        return 1;
    }

    private static int set(ServerPlayer player, Boolean requested) {
        boolean enabled;
        if (requested == null) {
            enabled = InfiniteResourceManager.toggle(player);
        } else {
            enabled = requested;
            InfiniteResourceManager.set(player, enabled);
        }
        if (enabled) {
            var classData = player.getData(ModAttachments.PLAYER_CLASS);
            var combat = player.getData(ModAttachments.PLAYER_COMBAT);
            player.setData(
                    ModAttachments.PLAYER_COMBAT,
                    combat.gainResource(ClassResourceRules.maxResource(classData), ClassResourceRules.maxResource(classData))
            );
        }
        player.sendSystemMessage(Component.translatable(
                enabled
                        ? "command.basicrpgclasses.infinite_resource_on"
                        : "command.basicrpgclasses.infinite_resource_off"
        ));
        return enabled ? 1 : 0;
    }

    private ModCommands() {
    }
}
