package dev.vegui.hfa.api.client.projection.v0;

import net.minecraft.world.item.Item;

import dev.vegui.hfa.impl.client.projection.v0.ClientProjectionRegistryImpl;

public final class ClientProjectionRegistry {
    private ClientProjectionRegistry() {
    }

    public static void registerItemProjection(Item nativeItem, ItemProjectionConfig config) {
        ClientProjectionRegistryImpl.registerItemProjection(nativeItem, config);
    }
}
