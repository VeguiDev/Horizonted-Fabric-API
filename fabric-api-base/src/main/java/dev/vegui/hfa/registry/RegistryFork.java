package dev.vegui.hfa.registry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class RegistryFork {
    private static final Set<ResourceKey<?>> FORKED_REGISTRIES = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<?>, Set<ResourceLocation>> BASELINE_ENTRIES = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, Set<ResourceLocation>> CUSTOM_ENTRIES = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, Map<ResourceLocation, String>> CUSTOM_ENTRY_OWNERS = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_PLUGIN = new ThreadLocal<>();

    private static volatile boolean capturing;

    private RegistryFork() {
    }

    public static void captureBaseline() {
        FORKED_REGISTRIES.clear();
        BASELINE_ENTRIES.clear();
        CUSTOM_ENTRIES.clear();
        CUSTOM_ENTRY_OWNERS.clear();
        CURRENT_PLUGIN.remove();

        captureRegistry(BuiltInRegistries.ITEM);
        capturing = true;
    }

    public static void completeFork() {
        capturing = false;
        CURRENT_PLUGIN.remove();
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static boolean isRegistryForked(ResourceKey<?> registryKey) {
        Objects.requireNonNull(registryKey, "registryKey");
        return capturing && FORKED_REGISTRIES.contains(registryKey);
    }

    public static void beginPlugin(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId");
        CURRENT_PLUGIN.set(pluginId);
    }

    public static void endPlugin() {
        CURRENT_PLUGIN.remove();
    }

    public static void markCustomEntry(ResourceKey<?> registryKey, ResourceLocation id) {
        Objects.requireNonNull(registryKey, "registryKey");
        Objects.requireNonNull(id, "id");

        if (!isRegistryForked(registryKey)) {
            return;
        }

        CUSTOM_ENTRIES.computeIfAbsent(registryKey, key -> ConcurrentHashMap.newKeySet()).add(id);
        CUSTOM_ENTRY_OWNERS
                .computeIfAbsent(registryKey, key -> new ConcurrentHashMap<>())
                .put(id, currentPluginId());
    }

    public static Set<ResourceLocation> baselineEntries(ResourceKey<?> registryKey) {
        Objects.requireNonNull(registryKey, "registryKey");
        return immutableSet(BASELINE_ENTRIES.get(registryKey));
    }

    public static Set<ResourceLocation> customEntries(ResourceKey<?> registryKey) {
        Objects.requireNonNull(registryKey, "registryKey");
        return immutableSet(CUSTOM_ENTRIES.get(registryKey));
    }

    public static Map<ResourceLocation, String> customEntryOwners(ResourceKey<?> registryKey) {
        Objects.requireNonNull(registryKey, "registryKey");
        Map<ResourceLocation, String> owners = CUSTOM_ENTRY_OWNERS.get(registryKey);

        if (owners == null) {
            return Map.of();
        }

        return Map.copyOf(owners);
    }

    public static String customEntryOwner(ResourceKey<?> registryKey, ResourceLocation id) {
        Objects.requireNonNull(registryKey, "registryKey");
        Objects.requireNonNull(id, "id");

        Map<ResourceLocation, String> owners = CUSTOM_ENTRY_OWNERS.get(registryKey);
        return owners == null ? null : owners.get(id);
    }

    public static Set<ResourceLocation> customItemIds() {
        return customEntries(Registries.ITEM);
    }

    public static Map<ResourceLocation, String> customItemOwners() {
        return customEntryOwners(Registries.ITEM);
    }

    public static Set<Item> customItems() {
        Set<Item> items = new LinkedHashSet<>();

        for (ResourceLocation itemId : customItemIds()) {
            Item item = BuiltInRegistries.ITEM.getValue(itemId);

            if (item != null) {
                items.add(item);
            }
        }

        return Collections.unmodifiableSet(items);
    }

    private static <T> void captureRegistry(Registry<T> registry) {
        ResourceKey<? extends Registry<T>> registryKey = registry.key();
        Set<ResourceLocation> baseline = ConcurrentHashMap.newKeySet();

        for (T entry : registry) {
            ResourceLocation id = registry.getKey(entry);

            if (id != null) {
                baseline.add(id);
            }
        }

        FORKED_REGISTRIES.add(registryKey);
        BASELINE_ENTRIES.put(registryKey, baseline);
        CUSTOM_ENTRIES.put(registryKey, ConcurrentHashMap.newKeySet());
        CUSTOM_ENTRY_OWNERS.put(registryKey, new ConcurrentHashMap<>());
    }

    private static String currentPluginId() {
        String pluginId = CURRENT_PLUGIN.get();
        return pluginId == null ? "unknown" : pluginId;
    }

    private static Set<ResourceLocation> immutableSet(Set<ResourceLocation> entries) {
        if (entries == null) {
            return Set.of();
        }

        return Collections.unmodifiableSet(new LinkedHashSet<>(entries));
    }
}
