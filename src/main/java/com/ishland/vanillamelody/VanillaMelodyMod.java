package com.ishland.vanillamelody;

import com.ishland.vanillamelody.common.Config;
import net.fabricmc.api.ModInitializer;

public class VanillaMelodyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.init();
    }
}
