package com.ishland.vanillamelody.mixin;

import com.ishland.vanillamelody.common.Config;
import com.ishland.vanillamelody.common.playback.ServerSongPlayer;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        if (Config.DEFAULT_RADIO_ENABLED)
            ServerSongPlayer.INSTANCE.addPlayer(player);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(ServerPlayerEntity player, CallbackInfo ci) {
        ServerSongPlayer.INSTANCE.removePlayer(player);
    }

    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void onRespawn(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        if (ServerSongPlayer.INSTANCE.removePlayer(player))
            ServerSongPlayer.INSTANCE.addPlayer(cir.getReturnValue());
    }

}
