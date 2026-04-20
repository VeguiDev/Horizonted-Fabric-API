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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;

public class PlaceholderResourcePack extends AbstractPackResources {
	private static final Component DESCRIPTION_TEXT = Component.translatable("pack.description.modResources");

	private final PackType type;

	public PlaceholderResourcePack(PackType type, PackLocationInfo location) {
		super(location);
		this.type = type;
	}

	public PackMetadataSection getMetadata() {
		return ModResourcePackUtil.getMetadataPack(SharedConstants.getCurrentVersion().packVersion(this.type), DESCRIPTION_TEXT);
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getRootResource(String... segments) {
		if (segments.length > 0) {
			switch (segments[0]) {
			case "pack.mcmeta":
				return () -> {
					DataResult<JsonElement> result = PackMetadataSection.CODEC.encodeStart(JsonOps.INSTANCE, getMetadata());
					String metadata = "{\"pack\":" + result.getOrThrow() + "}";
					return IOUtils.toInputStream(metadata, StandardCharsets.UTF_8);
				};
			case "pack.png":
				return ModResourcePackUtil::getDefaultIcon;
			default:
				return null;
			}
		}

		return null;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
		return null;
	}

	@Override
	public void listResources(PackType type, String namespace, String prefix, PackResources.ResourceOutput consumer) {
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		return Collections.emptySet();
	}

	@Override
	public void close() {
	}

	public record Factory(PackType type, PackLocationInfo location) implements Pack.ResourcesSupplier {
		@Override
		public PackResources openPrimary(PackLocationInfo location) {
			return new PlaceholderResourcePack(this.type, this.location);
		}

		@Override
		public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
			return openPrimary(location);
		}
	}
}
