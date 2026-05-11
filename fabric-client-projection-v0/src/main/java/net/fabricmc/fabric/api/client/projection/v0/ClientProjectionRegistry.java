package net.fabricmc.fabric.api.client.projection.v0;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.impl.client.projection.ClientProjectionRegistryImpl;

public final class ClientProjectionRegistry {
	private ClientProjectionRegistry() {
	}

	public static void registerItem(ResourceLocation itemId, ResourceLocation clientItemId) {
		ClientProjectionRegistryImpl.registerItemProjection(itemId, clientItemId);
	}

	public static void registerItem(ResourceLocation itemId, Item clientItem) {
		registerItem(itemId, getRequiredItemId(clientItem));
	}

	public static void registerItem(Item item, Item clientItem) {
		registerItem(getRequiredItemId(item), getRequiredItemId(clientItem));
	}

	public static boolean hasItemProjection(ResourceLocation itemId) {
		return ClientProjectionRegistryImpl.hasItemProjection(itemId);
	}

	public static Optional<ResourceLocation> getItemProjection(ResourceLocation itemId) {
		return ClientProjectionRegistryImpl.getItemProjection(itemId);
	}

	public static Map<ResourceLocation, ResourceLocation> getItemProjections() {
		return ClientProjectionRegistryImpl.getItemProjections();
	}

	private static ResourceLocation getRequiredItemId(Item item) {
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

		if (itemId == null) {
			throw new IllegalArgumentException("Item is not registered in BuiltInRegistries.ITEM");
		}

		return itemId;
	}
}
