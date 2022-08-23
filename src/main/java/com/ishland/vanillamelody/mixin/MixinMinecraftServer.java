package com.ishland.vanillamelody.mixin;

import com.ishland.vanillamelody.common.playback.ServerSyncedPlaybackManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Inject(method = "tickWorlds", at = @At("RETURN"))
    private void postTickWorld(CallbackInfo ci) {
//        SongPlayer.INSTANCE.tick();
    }

    @Inject(method = "loadWorld", at = @At("RETURN"))
    private void postLoadWorld(CallbackInfo ci) {
        ServerSyncedPlaybackManager.init();
    }

}
