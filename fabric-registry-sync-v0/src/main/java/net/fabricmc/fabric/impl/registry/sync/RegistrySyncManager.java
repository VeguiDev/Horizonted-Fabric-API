/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.registry.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationNetworkHandler;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl;
import net.fabricmc.fabric.impl.registry.sync.packet.DirectRegistryPacketHandler;

public final class RegistrySyncManager {
	public static final boolean DEBUG = Boolean.getBoolean("fabric.registry.debug");

	public static final DirectRegistryPacketHandler DIRECT_PACKET_HANDLER = new DirectRegistryPacketHandler();

	private static final Logger LOGGER = LoggerFactory.getLogger("FabricRegistrySync");
	private static final boolean DEBUG_WRITE_REGISTRY_DATA = Boolean.getBoolean("fabric.registry.debug.writeContentsAsCsv");

	public static boolean postBootstrap = false;

	private RegistrySyncManager() {
	}

	public static void configureClient(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
		Map<ResourceLocation, Object2IntMap<ResourceLocation>> map = createAndPopulateRegistryMap();

		if (map == null) {
			return;
		}

		if (!ServerConfigurationNetworking.canSend(handler, DIRECT_PACKET_HANDLER.getPacketId())) {
			if (areAllRegistriesOptional(map)) {
				return;
			}

			Component message = getIncompatibleClientText(ServerNetworkingImpl.getAddon(handler).getClientBrand(), map);
			handler.disconnect(message);
			return;
		}

		((FabricServerConfigurationNetworkHandler) handler).addTask(new SyncConfigurationTask(map));
	}

	private static Component getIncompatibleClientText(@Nullable String brand, Map<ResourceLocation, Object2IntMap<ResourceLocation>> map) {
		String brandText = switch (brand) {
			case "fabric" -> "Fabric API";
			case null, default -> "Fabric Loader and Fabric API";
		};

		int toDisplay = 4;
		List<String> namespaces = map.values().stream()
				.map(Object2IntMap::keySet)
				.flatMap(Set::stream)
				.map(ResourceLocation::getNamespace)
				.filter(namespace -> !ResourceLocation.DEFAULT_NAMESPACE.equals(namespace))
				.distinct()
				.sorted()
				.toList();

		MutableComponent text = Component.literal("The following registry entry namespaces may be related:\n\n");

		for (int i = 0; i < Math.min(namespaces.size(), toDisplay); i++) {
			text.append(Component.literal(namespaces.get(i)).withStyle(ChatFormatting.YELLOW));
			text.append(CommonComponents.NEW_LINE);
		}

		if (namespaces.size() > toDisplay) {
			text.append(Component.literal("And %d more...".formatted(namespaces.size() - toDisplay)));
		}

		return Component.literal("This server requires ")
				.append(Component.literal(brandText).withStyle(ChatFormatting.GREEN))
				.append(" installed on your client!")
				.append(CommonComponents.NEW_LINE)
				.append(text)
				.append(CommonComponents.NEW_LINE)
				.append(CommonComponents.NEW_LINE)
				.append(Component.literal("Contact the server's administrator for more information!").withStyle(ChatFormatting.GOLD));
	}

	private static boolean areAllRegistriesOptional(Map<ResourceLocation, Object2IntMap<ResourceLocation>> map) {
		return map.keySet().stream()
				.map(BuiltInRegistries.REGISTRY::getValue)
				.filter(Objects::nonNull)
				.map(RegistryAttributeHolder::get)
				.allMatch(attributes -> attributes.hasAttribute(RegistryAttribute.OPTIONAL));
	}

	public record SyncConfigurationTask(Map<ResourceLocation, Object2IntMap<ResourceLocation>> map) implements ConfigurationTask {
		public static final Type TYPE = new Type("fabric:registry/sync");

		@Override
		public void start(Consumer<Packet<?>> sender) {
			DIRECT_PACKET_HANDLER.sendPacket(payload -> sender.accept(ServerConfigurationNetworking.createS2CPacket(payload)), map);
		}

		@Override
		public Type type() {
			return TYPE;
		}
	}

	@Nullable
	public static Map<ResourceLocation, Object2IntMap<ResourceLocation>> createAndPopulateRegistryMap() {
		Map<ResourceLocation, Object2IntMap<ResourceLocation>> map = new LinkedHashMap<>();

		for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
			ResourceLocation registryId = getRegistryId(registry);

			if (registryId == null) {
				continue;
			}

			if (DEBUG_WRITE_REGISTRY_DATA) {
				writeDebugContents(registryId, registry);
			}

			RegistryAttributeHolder attributeHolder = RegistryAttributeHolder.get(registry.key());

			if (!attributeHolder.hasAttribute(RegistryAttribute.SYNCED)) {
				LOGGER.debug("Not syncing registry: {}", registryId);
				continue;
			}

			if (!attributeHolder.hasAttribute(RegistryAttribute.MODDED)) {
				LOGGER.debug("Skipping un-modded registry: {}", registryId);
				continue;
			}

			LOGGER.debug("Syncing registry: {}", registryId);

			Object2IntMap<ResourceLocation> idMap = createRegistryIdMap(registryId, registry);

			if (!idMap.isEmpty()) {
				map.put(registryId, idMap);
			}
		}

		return map.isEmpty() ? null : map;
	}

	private static void writeDebugContents(ResourceLocation registryId, Registry<?> registry) {
		File location = new File(".fabric" + File.separatorChar + "debug" + File.separatorChar + "registry");
		boolean canWrite = true;

		if (!location.exists() && !location.mkdirs()) {
			LOGGER.warn("[fabric-registry-sync debug] Could not create {} directory!", location.getAbsolutePath());
			canWrite = false;
		}

		if (!canWrite) {
			return;
		}

		File file = new File(location, registryId.toString().replace(':', '.').replace('/', '.') + ".csv");

		try (FileOutputStream stream = new FileOutputStream(file)) {
			StringBuilder builder = new StringBuilder("Raw ID,String ID,Class Type\n");
			appendDebugRows(builder, registry);
			stream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOGGER.warn("[fabric-registry-sync debug] Could not write to {}!", file.getAbsolutePath(), e);
		}
	}

	private static <T> void appendDebugRows(StringBuilder builder, Registry<T> registry) {
		for (T entry : registry) {
			ResourceLocation id = registry.getKey(entry);

			if (id == null) {
				continue;
			}

			builder.append('"')
					.append(registry.getId(entry))
					.append("\",\"")
					.append(id)
					.append("\",\"")
					.append(entry == null ? "null" : entry.getClass().getName())
					.append("\"\n");
		}
	}

	private static <T> Object2IntMap<ResourceLocation> createRegistryIdMap(ResourceLocation registryId, Registry<T> registry) {
		Object2IntMap<ResourceLocation> idMap = new Object2IntLinkedOpenHashMap<>();
		IntSet rawIdsFound = DEBUG ? new IntOpenHashSet() : null;

		for (T entry : registry) {
			ResourceLocation id = registry.getKey(entry);

			if (id == null) {
				continue;
			}

			int rawId = registry.getId(entry);

			if (DEBUG) {
				if (registry.getValue(id) != entry) {
					LOGGER.error("[fabric-registry-sync] Inconsistency detected in {}: object {} -> string ID {} -> object {}!", registryId, entry, id, registry.getValue(id));
				}

				if (registry.byId(rawId) != entry) {
					LOGGER.error("[fabric-registry-sync] Inconsistency detected in {}: object {} -> integer ID {} -> object {}!", registryId, entry, rawId, registry.byId(rawId));
				}

				if (!rawIdsFound.add(rawId)) {
					LOGGER.error("[fabric-registry-sync] Inconsistency detected in {}: multiple objects hold the raw ID {} (this one is {})", registryId, rawId, id);
				}
			}

			idMap.put(id, rawId);
		}

		return idMap;
	}

	public static void bootstrapRegistries() {
		postBootstrap = true;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static ResourceLocation getRegistryId(Registry<?> registry) {
		return ((Registry) BuiltInRegistries.REGISTRY).getKey(registry);
	}
}
