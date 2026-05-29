package dev.vegui.hfa.metadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.util.tree.Format;
import io.canvasmc.horizon.util.tree.ObjectTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HFAPluginMetadataRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("HFAPluginMetadata");
    private static final Map<String, HFAPluginMetadata> METADATA = new ConcurrentHashMap<>();

    private HFAPluginMetadataRegistry() {
    }

    public static void loadAll() {
        METADATA.clear();

        for (HorizonPlugin plugin : HorizonLoader.getInstance().getPlugins().getAll()) {
            HFAPluginMetadata metadata = load(plugin);
            METADATA.put(metadata.pluginId(), metadata);
            LOGGER.debug("Loaded HFA metadata for {}: client_required={}, effective={}",
                    metadata.pluginId(), metadata.clientRequired(), metadata.effectiveClientMode());
        }
    }

    public static Map<String, HFAPluginMetadata> all() {
        return Map.copyOf(METADATA);
    }

    public static HFAPluginMetadata get(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId");
        return METADATA.getOrDefault(pluginId, HFAPluginMetadata.serverOnly(pluginId));
    }

    public static void setEffectiveClientMode(String pluginId, HFAClientMode mode) {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(mode, "mode");

        METADATA.compute(pluginId, (id, metadata) -> {
            HFAPluginMetadata current = metadata == null ? HFAPluginMetadata.serverOnly(id) : metadata;
            return new HFAPluginMetadata(current.pluginId(), current.clientRequired(), mode);
        });
    }

    private static HFAPluginMetadata load(HorizonPlugin plugin) {
        String pluginId = plugin.pluginMetadata().id();
        boolean clientRequired = readClientRequired(plugin);

        if (clientRequired) {
            return HFAPluginMetadata.required(pluginId, true);
        }

        return HFAPluginMetadata.serverOnly(pluginId);
    }

    private static boolean readClientRequired(HorizonPlugin plugin) {
        Path metadataPath = plugin.fileSystem().getPath("horizon.plugin.json");

        if (!Files.exists(metadataPath)) {
            return false;
        }

        try (BufferedReader reader = Files.newBufferedReader(metadataPath)) {
            ObjectTree root = ObjectTree.read()
                    .format(Format.JSON)
                    .from(reader);

            return root.getTreeOptional("hfa")
                    .flatMap(tree -> tree.getValueSafe("client_required").asBooleanOptional())
                    .orElse(false);
        } catch (IOException e) {
            LOGGER.warn("Failed to read HFA metadata for {}", plugin.pluginMetadata().id(), e);
            return false;
        } catch (Exception e) {
            LOGGER.warn("Failed to parse HFA metadata for {}", plugin.pluginMetadata().id(), e);
            return false;
        }
    }
}
