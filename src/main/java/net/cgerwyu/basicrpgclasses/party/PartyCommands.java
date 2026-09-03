package net.cgerwyu.basicrpgclasses.party;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BasicRPGClasses.MODID)
public final class PartyCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("party")
                .then(Commands.literal("invite")
                        .then(Commands.argument("nickname", EntityArgument.player())
                                .executes(context -> invite(
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "nickname")
                                ))))
                .then(Commands.literal("accept")
                        .then(Commands.argument("nickname", EntityArgument.player())
                                .executes(context -> accept(
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "nickname")
                                ))))
                .then(Commands.literal("deny")
                        .then(Commands.argument("nickname", EntityArgument.player())
                                .executes(context -> deny(
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "nickname")
                                ))))
                .then(Commands.literal("leave")
                        .executes(context -> result(PartyService.leave(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("nickname", EntityArgument.player())
                                .executes(context -> kick(
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "nickname")
                                )))));
    }

    private static int invite(ServerPlayer inviter, ServerPlayer invited) {
        return result(PartyService.invite(inviter, invited));
    }

    private static int accept(ServerPlayer invited, ServerPlayer inviter) {
        return result(PartyService.accept(invited, inviter));
    }

    private static int deny(ServerPlayer invited, ServerPlayer inviter) {
        return result(PartyService.deny(invited, inviter));
    }

    private static int kick(ServerPlayer leader, ServerPlayer target) {
        return result(PartyService.kick(leader, target));
    }

    private static int result(boolean successful) {
        return successful ? 1 : 0;
    }

    private PartyCommands() {
    }
}
