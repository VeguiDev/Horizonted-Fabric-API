package dev.vegui.hfa.impl.client.projection.v0;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.vegui.hfa.api.client.projection.v0.ItemProjectionConfig;
import dev.vegui.hfa.registry.RegistryFork;
import net.fabricmc.fabric.impl.networking.FabricRegistryByteBuf;

public final class ClientProjectionRegistryImpl {
    private static final ResourceLocation REGISTRY_SYNC_CHANNEL = ResourceLocation.fromNamespaceAndPath("fabric", "registry/sync/direct");
    private static final Map<Item, ItemProjectionConfig> ITEM_PROJECTIONS = new ConcurrentHashMap<>();

    private ClientProjectionRegistryImpl() {
    }

    public static void registerItemProjection(Item nativeItem, ItemProjectionConfig config) {
        Objects.requireNonNull(nativeItem, "nativeItem");
        Objects.requireNonNull(config, "config");
        validateProjectedItem(config.item());

        ItemProjectionConfig existing = ITEM_PROJECTIONS.putIfAbsent(nativeItem, config);

        if (existing != null && !existing.equals(config)) {
            throw new IllegalStateException("Item projection already registered for " + nativeItem);
        }
    }

    public static boolean hasItemProjection(Item nativeItem) {
        Objects.requireNonNull(nativeItem, "nativeItem");
        return ITEM_PROJECTIONS.containsKey(nativeItem);
    }

    public static Optional<ItemProjectionConfig> getItemProjection(Item nativeItem) {
        Objects.requireNonNull(nativeItem, "nativeItem");
        return Optional.ofNullable(ITEM_PROJECTIONS.get(nativeItem));
    }

    public static List<ResourceLocation> getItemsMissingProjection() {
        return RegistryFork.customItems().stream()
                .filter(item -> !ITEM_PROJECTIONS.containsKey(item))
                .map(BuiltInRegistries.ITEM::getKey)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    public static boolean hasCompleteItemProjectionCoverage() {
        return getItemsMissingProjection().isEmpty();
    }

    public static ItemStack projectItemStackForNetwork(RegistryFriendlyByteBuf buf, ItemStack stack) {
        Objects.requireNonNull(buf, "buf");
        Objects.requireNonNull(stack, "stack");

        if (!shouldProjectNetworkItems(buf) || stack.isEmpty()) {
            return stack;
        }

        ItemProjectionConfig projection = ITEM_PROJECTIONS.get(stack.getItem());

        if (projection == null) {
            return stack;
        }

        validateProjectedItem(projection.item());

        ItemStack projectedStack = stack.transmuteCopy(projection.item(), stack.getCount());
        projectedStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(projection.customModelData()), List.of()));
        return projectedStack;
    }

    public static Map<Item, ItemProjectionConfig> getItemProjections() {
        return Map.copyOf(ITEM_PROJECTIONS);
    }

    private static boolean shouldProjectNetworkItems(RegistryFriendlyByteBuf buf) {
        FabricRegistryByteBuf fabricRegistryByteBuf = (FabricRegistryByteBuf) buf;
        var sendableChannels = fabricRegistryByteBuf.fabric_getSendableConfigurationChannels();
        return sendableChannels == null || !sendableChannels.contains(REGISTRY_SYNC_CHANNEL);
    }

    private static void validateProjectedItem(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        if (itemId == null) {
            throw new IllegalStateException("Projected item is not registered in BuiltInRegistries.ITEM: " + item);
        }

        if (!ResourceLocation.DEFAULT_NAMESPACE.equals(itemId.getNamespace())) {
            throw new IllegalArgumentException("Projected item must be a vanilla item, got " + itemId);
        }
    }
}
