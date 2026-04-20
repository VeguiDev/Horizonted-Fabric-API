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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;

import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

public class ModNioResourcePack extends AbstractPackResources implements ModResourcePack {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModNioResourcePack.class);
	private static final Pattern RESOURCE_PACK_PATH = Pattern.compile("[a-z0-9-_.]+");
	private static final FileSystem DEFAULT_FS = FileSystems.getDefault();

	private final String id;
	private final ModContainer mod;
	private final List<Path> basePaths;
	private final PackType type;
	private final ResourcePackActivationType activationType;
	private final Map<PackType, Set<String>> namespaces;
	private final boolean modBundled;

	@Nullable
	public static ModNioResourcePack create(String id, ModContainer mod, String subPath, PackType type, ResourcePackActivationType activationType, boolean modBundled) {
		List<Path> rootPaths = mod.getRootPaths();
		List<Path> paths;

		if (subPath == null) {
			paths = rootPaths;
		} else {
			paths = new ArrayList<>(rootPaths.size());

			for (Path path : rootPaths) {
				path = path.toAbsolutePath().normalize();
				Path childPath = path.resolve(subPath.replace("/", path.getFileSystem().getSeparator())).normalize();

				if (!childPath.startsWith(path) || !exists(childPath)) {
					continue;
				}

				paths.add(childPath);
			}
		}

		if (paths.isEmpty()) {
			return null;
		}

		String packId = subPath != null && modBundled ? id + "_" + subPath : id;
		Component displayName = subPath == null
				? Component.translatable("pack.name.fabricMod", mod.getMetadata().getName())
				: Component.translatable("pack.name.fabricMod.subPack", mod.getMetadata().getName(), Component.translatable("resourcePack." + subPath + ".name"));
		PackLocationInfo location = new PackLocationInfo(
				packId,
				displayName,
				ModResourcePackCreator.RESOURCE_PACK_SOURCE,
				Optional.of(new KnownPack(ModResourcePackCreator.FABRIC, packId, mod.getMetadata().getVersion().getFriendlyString()))
		);
		ModNioResourcePack pack = new ModNioResourcePack(packId, mod, paths, type, activationType, modBundled, location);
		return pack.getNamespaces(type).isEmpty() ? null : pack;
	}

	private ModNioResourcePack(String id, ModContainer mod, List<Path> paths, PackType type, ResourcePackActivationType activationType, boolean modBundled, PackLocationInfo location) {
		super(location);
		this.id = id;
		this.mod = mod;
		this.basePaths = paths;
		this.type = type;
		this.activationType = activationType;
		this.modBundled = modBundled;
		this.namespaces = readNamespaces(paths, mod.getMetadata().getId());
	}

	@Override
	public ModNioResourcePack createOverlay(String overlay) {
		return new ModNioResourcePack(
				this.id,
				this.mod,
				this.basePaths.stream().map(path -> path.resolve(overlay)).toList(),
				this.type,
				this.activationType,
				this.modBundled,
				this.location()
		);
	}

	static Map<PackType, Set<String>> readNamespaces(List<Path> paths, String modId) {
		Map<PackType, Set<String>> ret = new EnumMap<>(PackType.class);

		for (PackType type : PackType.values()) {
			Set<String> namespaces = null;

			for (Path path : paths) {
				Path dir = path.resolve(type.getDirectory());
				if (!Files.isDirectory(dir)) {
					continue;
				}

				String separator = path.getFileSystem().getSeparator();

				try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
					for (Path p : ds) {
						if (!Files.isDirectory(p)) {
							continue;
						}

						String namespace = p.getFileName().toString().replace(separator, "");

						if (!RESOURCE_PACK_PATH.matcher(namespace).matches()) {
							LOGGER.warn("Fabric NioResourcePack: ignored invalid namespace: {} in mod ID {}", namespace, modId);
							continue;
						}

						if (namespaces == null) {
							namespaces = new HashSet<>();
						}

						namespaces.add(namespace);
					}
				} catch (IOException e) {
					LOGGER.warn("getNamespaces in mod {} failed", modId, e);
				}
			}

			ret.put(type, namespaces != null ? namespaces : Collections.emptySet());
		}

		return ret;
	}

	private Path getPath(String filename) {
		if (hasAbsentNamespace(filename)) {
			return null;
		}

		for (Path basePath : this.basePaths) {
			Path childPath = basePath.resolve(filename.replace("/", basePath.getFileSystem().getSeparator())).toAbsolutePath().normalize();

			if (childPath.startsWith(basePath) && exists(childPath)) {
				return childPath;
			}
		}

		return null;
	}

	private static final String RESOURCE_PREFIX = PackType.CLIENT_RESOURCES.getDirectory() + "/";
	private static final String DATA_PREFIX = PackType.SERVER_DATA.getDirectory() + "/";

	private boolean hasAbsentNamespace(String filename) {
		int prefixLen;
		PackType packType;

		if (filename.startsWith(RESOURCE_PREFIX)) {
			prefixLen = RESOURCE_PREFIX.length();
			packType = PackType.CLIENT_RESOURCES;
		} else if (filename.startsWith(DATA_PREFIX)) {
			prefixLen = DATA_PREFIX.length();
			packType = PackType.SERVER_DATA;
		} else {
			return false;
		}

		int namespaceEnd = filename.indexOf('/', prefixLen);
		if (namespaceEnd < 0) {
			return false;
		}

		return !this.namespaces.get(packType).contains(filename.substring(prefixLen, namespaceEnd));
	}

	@Nullable
	private IoSupplier<InputStream> openFile(String filename) {
		Path path = getPath(filename);

		if (path != null && Files.isRegularFile(path)) {
			return () -> Files.newInputStream(path);
		}

		if (ModResourcePackUtil.containsDefault(filename, this.modBundled)) {
			return () -> ModResourcePackUtil.openDefault(this.mod, this.type, filename);
		}

		return null;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getRootResource(String... pathSegments) {
		FileUtil.validatePath(pathSegments);
		return this.openFile(String.join("/", pathSegments));
	}

	@Override
	@Nullable
	public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
		Path path = getPath(getFilename(type, id));
		return path == null ? null : IoSupplier.create(path);
	}

	@Override
	public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput visitor) {
		if (!this.namespaces.getOrDefault(type, Collections.emptySet()).contains(namespace)) {
			return;
		}

		for (Path basePath : this.basePaths) {
			String separator = basePath.getFileSystem().getSeparator();
			Path namespacePath = basePath.resolve(type.getDirectory()).resolve(namespace);
			Path searchPath = namespacePath.resolve(path.replace("/", separator)).normalize();

			if (!exists(searchPath)) {
				continue;
			}

			try {
				Files.walkFileTree(searchPath, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						String filename = namespacePath.relativize(file).toString().replace(separator, "/");
						ResourceLocation identifier = ResourceLocation.tryBuild(namespace, filename);

						if (identifier == null) {
							LOGGER.error("Invalid path in mod resource-pack {}: {}:{}, ignoring", id, namespace, filename);
						} else {
							visitor.accept(identifier, IoSupplier.create(file));
						}

						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				LOGGER.warn("findResources at {} in namespace {}, mod {} failed", path, namespace, this.mod.getMetadata().getId(), e);
			}
		}
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		return this.namespaces.getOrDefault(type, Collections.emptySet());
	}

	@Override
	public void close() {
	}

	@Override
	public ModMetadata getFabricModMetadata() {
		return this.mod.getMetadata();
	}

	public ResourcePackActivationType getActivationType() {
		return this.activationType;
	}

	public String getId() {
		return this.id;
	}

	public PackResources openWithOverlays(List<String> overlays) {
		if (overlays.isEmpty()) {
			return this;
		}

		List<PackResources> packOverlays = new ArrayList<>(overlays.size());

		for (String overlay : overlays) {
			packOverlays.add(this.createOverlay(overlay));
		}

		return new CompositePackResources(this, packOverlays);
	}

	private static boolean exists(Path path) {
		return path.getFileSystem() == DEFAULT_FS ? path.toFile().exists() : Files.exists(path);
	}

	private static String getFilename(PackType type, ResourceLocation id) {
		return String.format(Locale.ROOT, "%s/%s/%s", type.getDirectory(), id.getNamespace(), id.getPath());
	}
}
