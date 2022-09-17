package com.ishland.vanillamelody.common;

import net.fabricmc.loader.api.FabricLoader;

public class Constants {

    public static final boolean isRSLSInstalled = FabricLoader.getInstance().isModLoaded("rsls");

    private Constants() {
    }

}
