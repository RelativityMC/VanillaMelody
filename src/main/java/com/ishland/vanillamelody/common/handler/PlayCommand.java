package com.ishland.vanillamelody.common.handler;

import com.ishland.vanillamelody.common.Config;
import com.ishland.vanillamelody.common.playback.ServerSongPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.concurrent.CompletableFuture;

public class PlayCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> command =
                CommandManager.literal("vanillamelody")
                        .then(
                                CommandManager.literal("reload")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(PlayCommand::handleReload)
                        )
                        .then(
                                CommandManager.literal("radio")
                                        .then(
                                                CommandManager.literal("next")
                                                        .requires(source -> Config.ALLOW_NON_OPERATOR_RADIO_CHANGE || source.hasPermissionLevel(2))
                                                        .executes(PlayCommand::handleRadioNext)
                                        )
                                        .then(
                                                CommandManager.literal("setSong")
                                                        .requires(source -> Config.ALLOW_NON_OPERATOR_RADIO_CHANGE || source.hasPermissionLevel(2))
                                                        .then(
                                                                CommandManager.argument("song", MessageArgumentType.message())
                                                                        .suggests((context, builder) -> CommandSource.suggestMatching(ServerSongPlayer.playlistSuggestion(), builder))
                                                                        .executes(PlayCommand::handleRadioSetSong)
                                                        )
                                        )
                                        .then(
                                                CommandManager.literal("join")
                                                        .requires(source -> source.getEntity() instanceof PlayerEntity)
                                                        .executes(PlayCommand::handleRadioJoin)
                                        )
                                        .then(
                                                CommandManager.literal("leave")
                                                        .requires(source -> source.getEntity() instanceof PlayerEntity)
                                                        .executes(PlayCommand::handleRadioLeave)
                                        )
                        );
        dispatcher.register(
                command
        );
    }

    private static int handleRadioSetSong(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        final String song = MessageArgumentType.getMessage(ctx, "song").getString();
        ServerSongPlayer.EXECUTOR.execute(() -> ServerSongPlayer.INSTANCE.setSong(song));
        return 0;
    }

    private static int handleRadioNext(CommandContext<ServerCommandSource> ctx) {
        ServerSongPlayer.EXECUTOR.execute(ServerSongPlayer.INSTANCE::nextSong);
        return 0;
    }

    private static int handleReload(CommandContext<ServerCommandSource> ctx) {
        CompletableFuture.runAsync(() -> {
                    Config.reload();
                    ServerSongPlayer.reload();
                })
                .thenRunAsync(() -> ctx.getSource().sendFeedback(new LiteralText("Reloaded songs"), true), ctx.getSource().getServer());
        return 0;
    }

    private static int handleRadioJoin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerPlayerEntity player = context.getSource().getPlayer();
        ServerSongPlayer.INSTANCE.removePlayer(player);
        ServerSongPlayer.INSTANCE.addPlayer(player);
        return 0;
    }

    private static int handleRadioLeave(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerPlayerEntity player = context.getSource().getPlayer();
        ServerSongPlayer.INSTANCE.removePlayer(player);
        return 0;
    }
}
