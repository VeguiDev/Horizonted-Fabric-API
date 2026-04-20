package net.fabricmc.loader.api.metadata;

import java.util.Optional;

import net.fabricmc.loader.api.Version;

public interface ModMetadata {
	String getId();

	String getName();

	Version getVersion();

	default String getType() {
		return "plugin";
	}

	default Optional<String> getIconPath(int size) {
		return Optional.empty();
	}

	default CustomValue getCustomValue(String key) {
		return null;
	}
}
