package net.fabricmc.fabric.impl.client.projection;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;

public final class ClientProjectionRegistryImpl {
	private static final Map<ResourceLocation, ResourceLocation> ITEM_PROJECTIONS = new ConcurrentHashMap<>();

	private ClientProjectionRegistryImpl() {
	}

	public static void registerItemProjection(ResourceLocation itemId, ResourceLocation clientItemId) {
		Objects.requireNonNull(itemId, "itemId");
		Objects.requireNonNull(clientItemId, "clientItemId");

		ResourceLocation existing = ITEM_PROJECTIONS.putIfAbsent(itemId, clientItemId);

		if (existing != null && !existing.equals(clientItemId)) {
			throw new IllegalStateException("Item projection already registered for " + itemId + ": " + existing);
		}
	}

	public static boolean hasItemProjection(ResourceLocation itemId) {
		Objects.requireNonNull(itemId, "itemId");
		return ITEM_PROJECTIONS.containsKey(itemId);
	}

	public static Optional<ResourceLocation> getItemProjection(ResourceLocation itemId) {
		Objects.requireNonNull(itemId, "itemId");
		return Optional.ofNullable(ITEM_PROJECTIONS.get(itemId));
	}

	public static Map<ResourceLocation, ResourceLocation> getItemProjections() {
		return Map.copyOf(ITEM_PROJECTIONS);
	}
}
