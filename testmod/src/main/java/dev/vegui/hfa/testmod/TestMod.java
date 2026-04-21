package dev.vegui.hfa.testmod;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.service.entrypoint.DedicatedServerInitializer;

public class TestMod implements DedicatedServerInitializer {

    public static Logger LOGGER = HorizonLoader.LOGGER;

    @Override
    public void onInitialize() {
        LOGGER.info("Hello from TestMod!");
    }
}