package net.fabricmc.loader.api;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;

import net.fabricmc.loader.api.metadata.ModMetadata;

public final class FabricLoader {
	private static final FabricLoader INSTANCE = new FabricLoader();

	private final Map<String, ModContainer> containers = new ConcurrentHashMap<>();

	private FabricLoader() {
	}

	public static FabricLoader getInstance() {
		return INSTANCE;
	}

	public Collection<ModContainer> getAllMods() {
		return HorizonLoader.getInstance().getPlugins().getAll().stream().map(this::wrap).toList();
	}

	public Optional<ModContainer> getModContainer(String id) {
		return getAllMods().stream().filter(container -> container.getMetadata().getId().equals(id)).findFirst();
	}

	private ModContainer wrap(HorizonPlugin plugin) {
		String id = HorizonBackedModMetadata.computeId(plugin);
		return containers.computeIfAbsent(id, ignored -> new HorizonBackedModContainer(plugin));
	}

	private record HorizonBackedModContainer(HorizonPlugin plugin) implements ModContainer {
		private static final String JAR_SUFFIX = ".jar";

		@Override
		public List<Path> getRootPaths() {
			return List.of(plugin.getPath());
		}

		@Override
		public ModMetadata getMetadata() {
			return new HorizonBackedModMetadata(plugin);
		}

		@Override
		public Optional<Path> findPath(String path) {
			return Optional.of(plugin.getPath().resolve(path)).filter(java.nio.file.Files::exists);
		}
	}

	private record HorizonBackedModMetadata(HorizonPlugin plugin) implements ModMetadata {
		private static String normalizeId(String value) {
			return value.toLowerCase(Locale.ROOT).replace(' ', '-');
		}

		static String computeId(HorizonPlugin plugin) {
			String fileName = plugin.file().ioFile().getName();
			if (fileName.endsWith(".jar")) {
				return normalizeId(fileName.substring(0, fileName.length() - 4));
			}

			return normalizeId(plugin.pluginMetadata().name());
		}

		@Override
		public String getId() {
			return computeId(plugin);
		}

		@Override
		public String getName() {
			return plugin.pluginMetadata().name();
		}

		@Override
		public Version getVersion() {
			return () -> plugin.pluginMetadata().version();
		}

		@Override
		public String getType() {
			return "plugin";
		}
	}
}
