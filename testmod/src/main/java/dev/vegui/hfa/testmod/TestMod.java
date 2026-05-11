package dev.vegui.hfa.testmod;

import dev.vegui.hfa.testmod.setup.ModItems;
import io.canvasmc.horizon.service.entrypoint.ServerPostBootstrapEntrypoint;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMod implements ServerPostBootstrapEntrypoint {

    public static final String MOD_ID = "hfa_test_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("server_postbootstrap invoked");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping bye");
        });
    }
}
