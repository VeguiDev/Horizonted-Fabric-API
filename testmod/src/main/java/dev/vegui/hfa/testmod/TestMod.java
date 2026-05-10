package dev.vegui.hfa.testmod;

import io.canvasmc.horizon.service.entrypoint.ServerPostBootstrapEntrypoint;

public class TestMod implements ServerPostBootstrapEntrypoint {

    @Override
    public void onInitialize() {
        System.out.println("[HFA Test Mod] server_postbootstrap invoked");
    }
}
