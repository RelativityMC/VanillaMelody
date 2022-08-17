package com.ishland.vanillamelody.mixin;

import com.ishland.vanillamelody.common.playback.SongPlayer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Inject(method = "tickWorlds", at = @At("RETURN"))
    private void postTickWorld(CallbackInfo ci) {
//        SongPlayer.INSTANCE.tick();
    }

}
