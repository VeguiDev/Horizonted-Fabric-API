package dev.vegui.fabricapi;

import io.canvasmc.horizon.service.entrypoint.DedicatedServerInitializer;

public class FabricNetworkingApi implements DedicatedServerInitializer {
    @Override
    public void onInitialize() {
        net.fabricmc.fabric.impl.networking.CommonPacketsImpl.init();
        net.fabricmc.fabric.impl.networking.NetworkingImpl.init();
    }
}
