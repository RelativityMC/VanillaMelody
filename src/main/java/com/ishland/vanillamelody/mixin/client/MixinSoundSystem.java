package com.ishland.vanillamelody.mixin.client;

import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = SoundSystem.class, priority = 500)
public class MixinSoundSystem {

    @ModifyConstant(method = "getAdjustedPitch", constant = @Constant(floatValue = 0.5f), require = 0)
    private float modifyMinimumPitch(float constant) {
        if (constant == 0.5f) return 0.01f;
        return constant;
    }

    @ModifyConstant(method = "getAdjustedPitch", constant = @Constant(floatValue = 2.0f), require = 0)
    private float modifyMaximumPitch(float constant) {
        if (constant == 2.0f) return Float.POSITIVE_INFINITY;
        return constant;
    }

    @ModifyConstant(method = "getAdjustedVolume", constant = @Constant(floatValue = 1.0f), require = 0)
    private float modifyMaximumVolume(float constant) {
        if (constant == 1.0f) return Float.POSITIVE_INFINITY;
        return constant;
    }

}
