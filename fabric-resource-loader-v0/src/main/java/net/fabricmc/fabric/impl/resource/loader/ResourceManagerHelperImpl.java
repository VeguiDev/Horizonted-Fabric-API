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

package net.fabricmc.fabric.impl.resource.loader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.AbstractMap.SimpleEntry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.ModContainer;

public class ResourceManagerHelperImpl implements ResourceManagerHelper {
	private static final Map<PackType, ResourceManagerHelperImpl> REGISTRY_MAP = new HashMap<>();
	private static final Set<SimpleEntry<Component, ModNioResourcePack>> BUILTIN_RESOURCE_PACKS = new HashSet<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerHelperImpl.class);

	private final Set<ResourceLocation> addedListenerIds = new HashSet<>();
	private final Set<ListenerFactory> listenerFactories = new LinkedHashSet<>();
	private final Set<IdentifiableResourceReloadListener> addedListeners = new LinkedHashSet<>();
	private final PackType type;

	private ResourceManagerHelperImpl(PackType type) {
		this.type = type;
	}

	public static ResourceManagerHelperImpl get(PackType type) {
		return REGISTRY_MAP.computeIfAbsent(type, ResourceManagerHelperImpl::new);
	}

	public static boolean registerBuiltinResourcePack(ResourceLocation id, String subPath, ModContainer container, Component displayName, ResourcePackActivationType activationType) {
		List<Path> paths = container.getRootPaths();
		String separator = paths.getFirst().getFileSystem().getSeparator();
		subPath = subPath.replace("/", separator);
		ModNioResourcePack resourcePack = ModNioResourcePack.create(id.toString(), container, subPath, PackType.CLIENT_RESOURCES, activationType, false);
		ModNioResourcePack dataPack = ModNioResourcePack.create(id.toString(), container, subPath, PackType.SERVER_DATA, activationType, false);

		if (resourcePack == null && dataPack == null) {
			return false;
		}

		if (resourcePack != null) {
			BUILTIN_RESOURCE_PACKS.add(new SimpleEntry<>(displayName, resourcePack));
		}

		if (dataPack != null) {
			BUILTIN_RESOURCE_PACKS.add(new SimpleEntry<>(displayName, dataPack));
		}

		return true;
	}

	public static boolean registerBuiltinResourcePack(ResourceLocation id, String subPath, ModContainer container, ResourcePackActivationType activationType) {
		return registerBuiltinResourcePack(id, subPath, container, Component.literal(id.getNamespace() + "/" + id.getPath()), activationType);
	}

	public static void registerBuiltinResourcePacks(PackType resourceType, Consumer<Pack> consumer) {
		for (SimpleEntry<Component, ModNioResourcePack> entry : BUILTIN_RESOURCE_PACKS) {
			ModNioResourcePack pack = entry.getValue();

			if (pack.getNamespaces(resourceType).isEmpty()) {
				continue;
			}

			PackLocationInfo info = new PackLocationInfo(
					pack.getId(),
					entry.getKey(),
					new BuiltinModResourcePackSource(pack.getFabricModMetadata().getName()),
					pack.knownPackInfo()
			);
			PackSelectionConfig selectionConfig = new PackSelectionConfig(
					pack.getActivationType() == ResourcePackActivationType.ALWAYS_ENABLED,
					Pack.Position.TOP,
					false
			);
			Pack profile = Pack.readMetaAndCreate(info, new ModResourcePackFactory(pack), resourceType, selectionConfig);

			if (profile != null) {
				consumer.accept(profile);
			}
		}
	}

	public static List<PreparableReloadListener> sort(PackType type, List<PreparableReloadListener> listeners) {
		if (type == null) {
			return listeners;
		}

		ResourceManagerHelperImpl instance = get(type);

		if (instance != null) {
			List<PreparableReloadListener> mutable = new ArrayList<>(listeners);
			instance.sort(mutable);
			return Collections.unmodifiableList(mutable);
		}

		return listeners;
	}

	protected void sort(List<PreparableReloadListener> listeners) {
		listeners.removeAll(this.addedListeners);

		HolderLookup.Provider wrapperLookup = getWrapperLookup(listeners);
		List<IdentifiableResourceReloadListener> listenersToAdd = Lists.newArrayList();

		for (ListenerFactory addedListener : this.listenerFactories) {
			listenersToAdd.add(addedListener.get(wrapperLookup));
		}

		this.addedListeners.clear();
		this.addedListeners.addAll(listenersToAdd);

		Set<ResourceLocation> resolvedIds = new HashSet<>();

		for (PreparableReloadListener listener : listeners) {
			if (listener instanceof IdentifiableResourceReloadListener identifiable) {
				resolvedIds.add(identifiable.getFabricId());
			}
		}

		int lastSize = -1;

		while (listeners.size() != lastSize) {
			lastSize = listeners.size();
			Iterator<IdentifiableResourceReloadListener> it = listenersToAdd.iterator();

			while (it.hasNext()) {
				IdentifiableResourceReloadListener listener = it.next();

				if (resolvedIds.containsAll(listener.getFabricDependencies())) {
					resolvedIds.add(listener.getFabricId());
					listeners.add(listener);
					it.remove();
				}
			}
		}

		for (IdentifiableResourceReloadListener listener : listenersToAdd) {
			LOGGER.warn("Could not resolve dependencies for listener: {}!", listener.getFabricId());
		}
	}

	@Nullable
	private HolderLookup.Provider getWrapperLookup(List<PreparableReloadListener> listeners) {
		if (this.type == PackType.CLIENT_RESOURCES) {
			return null;
		}

		for (PreparableReloadListener resourceReloader : listeners) {
			if (resourceReloader instanceof FabricRecipeManager recipeManager) {
				return recipeManager.fabric_getRegistries();
			}
		}

		throw new IllegalStateException("No RecipeManager found in listeners!");
	}

	@Override
	public void registerReloadListener(IdentifiableResourceReloadListener listener) {
		registerReloadListener(new SimpleResourceReloaderFactory(listener));
	}

	@Override
	public void registerReloadListener(ResourceLocation identifier, Function<HolderLookup.Provider, IdentifiableResourceReloadListener> listenerFactory) {
		if (this.type == PackType.CLIENT_RESOURCES) {
			throw new IllegalArgumentException("Cannot register a registry listener for the client resource type!");
		}

		registerReloadListener(new RegistryResourceReloaderFactory(identifier, listenerFactory));
	}

	private void registerReloadListener(ListenerFactory factory) {
		if (!this.addedListenerIds.add(factory.id())) {
			LOGGER.warn("Tried to register resource reload listener {} twice!", factory.id());
			return;
		}

		if (!this.listenerFactories.add(factory)) {
			throw new RuntimeException("Listener with previously unknown ID " + factory.id() + " already in listener set!");
		}
	}

	private sealed interface ListenerFactory permits SimpleResourceReloaderFactory, RegistryResourceReloaderFactory {
		ResourceLocation id();

		IdentifiableResourceReloadListener get(HolderLookup.Provider registry);
	}

	private record SimpleResourceReloaderFactory(IdentifiableResourceReloadListener listener) implements ListenerFactory {
		@Override
		public ResourceLocation id() {
			return listener.getFabricId();
		}

		@Override
		public IdentifiableResourceReloadListener get(HolderLookup.Provider registry) {
			return listener;
		}
	}

	private record RegistryResourceReloaderFactory(ResourceLocation id, Function<HolderLookup.Provider, IdentifiableResourceReloadListener> listenerFactory) implements ListenerFactory {
		private RegistryResourceReloaderFactory {
			Objects.requireNonNull(listenerFactory);
		}

		@Override
		public IdentifiableResourceReloadListener get(HolderLookup.Provider registry) {
			IdentifiableResourceReloadListener listener = listenerFactory.apply(registry);

			if (!id.equals(listener.getFabricId())) {
				throw new IllegalStateException("Listener factory for " + id + " created a listener with ID " + listener.getFabricId());
			}

			return listener;
		}
	}
}
