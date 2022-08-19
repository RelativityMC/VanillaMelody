package com.ishland.vanillamelody.common.handler;

import com.ishland.vanillamelody.common.playback.ServerSongPlayer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class PlayCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
//        dispatcher.register(
//                CommandManager.literal("playmusic")
//                        .executes(context -> {
//                            ServerSongPlayer.INSTANCE.playDefault();
//                            return 0;
//                        })
//        );
    }

}
