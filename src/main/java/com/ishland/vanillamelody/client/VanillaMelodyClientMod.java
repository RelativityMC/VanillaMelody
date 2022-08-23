package com.ishland.vanillamelody.client;

import com.ishland.vanillamelody.client.playback.ClientSyncedPlaybackManager;
import net.fabricmc.api.ClientModInitializer;

public class VanillaMelodyClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSyncedPlaybackManager.init();
    }
}
