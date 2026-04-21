package dev.vegui.hfa.testmod;

import io.canvasmc.horizon.service.entrypoint.DedicatedServerInitializer;

public class TestMod implements DedicatedServerInitializer {

    @Override
    public void onInitialize() {
        System.out.println("[HFA Test Mod] server_postbootstrap invoked");
    }
}
