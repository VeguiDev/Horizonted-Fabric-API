package net.fabricmc.api;

import io.canvasmc.horizon.service.entrypoint.EntrypointHandler;

@EntrypointHandler(value = "onInitialize", argTypes = {})
public interface ModInitializer {
    void onInitialize();
}
