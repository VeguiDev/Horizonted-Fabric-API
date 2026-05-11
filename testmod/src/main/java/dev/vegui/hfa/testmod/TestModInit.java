package dev.vegui.hfa.testmod;

import dev.vegui.hfa.testmod.setup.ModItems;
import net.fabricmc.api.ModInitializer;

public class TestModInit implements ModInitializer {
    @Override
    public void onInitialize() {
        TestMod.LOGGER.info("Mod initializer invoked");
        ModItems.initialize();
    }
}
