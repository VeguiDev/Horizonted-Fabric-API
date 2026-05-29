package dev.vegui.hfa.impl.client.projection.v0;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dev.vegui.hfa.metadata.HFAClientCompatibility;
import dev.vegui.hfa.metadata.HFAClientMode;
import dev.vegui.hfa.metadata.HFAPluginMetadata;
import dev.vegui.hfa.metadata.HFAPluginMetadataRegistry;
import dev.vegui.hfa.registry.RegistryFork;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientProjectionAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger("HFAClientProjection");

    private ClientProjectionAnalyzer() {
    }

    public static void analyze() {
        List<String> clientRequiredPlugins = HFAPluginMetadataRegistry.all().values().stream()
                .filter(HFAPluginMetadata::clientRequired)
                .map(HFAPluginMetadata::pluginId)
                .sorted()
                .toList();

        if (!clientRequiredPlugins.isEmpty()) {
            HFAClientCompatibility.setEffectiveClientMode(
                    HFAClientMode.REQUIRED,
                    clientRequiredPlugins.stream()
                            .map(pluginId -> pluginId + " declares hfa.client_required=true")
                            .toList()
            );
            clientRequiredPlugins.forEach(pluginId -> HFAPluginMetadataRegistry.setEffectiveClientMode(pluginId, HFAClientMode.REQUIRED));
            LOGGER.info("HFA client mode: REQUIRED ({})", String.join(", ", clientRequiredPlugins));
            return;
        }

        if (RegistryFork.customItemIds().isEmpty()) {
            HFAClientCompatibility.setEffectiveClientMode(HFAClientMode.SERVER_ONLY, List.of());
            LOGGER.info("HFA client mode: SERVER_ONLY");
            return;
        }

        List<ResourceLocation> missingItemProjections = ClientProjectionRegistryImpl.getItemsMissingProjection();

        if (!missingItemProjections.isEmpty()) {
            throw new IllegalStateException("Missing item projections for custom items: " + missingItemProjections.stream()
                    .map(itemId -> itemId + " owned by " + RegistryFork.customItemOwners().getOrDefault(itemId, "unknown"))
                    .toList());
        }

        Map<ResourceLocation, String> customItemOwners = RegistryFork.customItemOwners();
        Set<String> projectedPlugins = customItemOwners.values().stream()
                .filter(pluginId -> !"unknown".equals(pluginId))
                .collect(Collectors.toSet());
        projectedPlugins.forEach(pluginId -> HFAPluginMetadataRegistry.setEffectiveClientMode(pluginId, HFAClientMode.PROJECTED));

        HFAClientCompatibility.setEffectiveClientMode(
                HFAClientMode.PROJECTED,
                customItemOwners.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> "Projected item " + entry.getKey() + " owned by " + entry.getValue())
                        .toList()
        );
        LOGGER.info("HFA client mode: PROJECTED ({} custom item projection(s), owners: {})",
                RegistryFork.customItemIds().size(),
                projectedPlugins.stream()
                        .sorted()
                        .collect(Collectors.joining(", ")));
    }
}
