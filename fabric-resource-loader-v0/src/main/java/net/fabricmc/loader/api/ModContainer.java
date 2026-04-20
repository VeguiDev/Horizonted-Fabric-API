package net.fabricmc.loader.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import net.fabricmc.loader.api.metadata.ModMetadata;

public interface ModContainer {
	List<Path> getRootPaths();

	ModMetadata getMetadata();

	Optional<Path> findPath(String path);
}
