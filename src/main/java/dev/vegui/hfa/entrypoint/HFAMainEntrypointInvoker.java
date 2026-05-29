package dev.vegui.hfa.entrypoint;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.vegui.hfa.registry.RegistryFork;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.data.EntrypointObject;
import io.canvasmc.horizon.plugin.data.HorizonPluginMetadata;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HFAMainEntrypointInvoker {
    private static final Logger LOGGER = LoggerFactory.getLogger("HFAEntrypoint");

    private HFAMainEntrypointInvoker() {
    }

    public static void invoke() {
        List<Entry> entries = new ArrayList<>();
        HorizonLoader loader = HorizonLoader.getInstance();

        for (HorizonPlugin plugin : loader.getPlugins().getAll()) {
            HorizonPluginMetadata metadata = plugin.pluginMetadata();

            for (EntrypointObject entrypoint : metadata.entrypoints()) {
                if ("main".equalsIgnoreCase(entrypoint.key())) {
                    entries.add(new Entry(plugin, entrypoint));
                }
            }
        }

        entries.sort(Comparator.comparingInt(entry -> entry.entrypoint().order()));

        for (Entry entry : entries) {
            invokeEntry(loader, entry);
        }
    }

    private static void invokeEntry(HorizonLoader loader, Entry entry) {
        String pluginId = entry.plugin().pluginMetadata().id();
        String targetClass = entry.entrypoint().clazz();

        RegistryFork.beginPlugin(pluginId);

        try {
            Class<? extends ModInitializer> implementationClass = Class.forName(targetClass, true, loader.getLaunchService().getClassLoader())
                    .asSubclass(ModInitializer.class);
            ModInitializer initializer = implementationClass.getDeclaredConstructor().newInstance();
            Method method = ModInitializer.class.getMethod("onInitialize");
            method.invoke(initializer);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to run main entrypoint '{}' for plugin '{}'", targetClass, pluginId, throwable);
        } finally {
            RegistryFork.endPlugin();
        }
    }

    private record Entry(HorizonPlugin plugin, EntrypointObject entrypoint) {
    }
}
