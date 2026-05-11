package dev.vegui.hfa.testmod;

import io.canvasmc.horizon.service.entrypoint.ServerPostBootstrapEntrypoint;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class TestMod implements ServerPostBootstrapEntrypoint {

    @Override
    public void onInitialize() {
        System.out.println("[HFA Test Mod] server_postbootstrap invoked");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("[HFA Test Mod] Server started");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            System.out.println("[HFA Test Mod] Server stopping bye");
        });
    }
}
