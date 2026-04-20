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

import java.util.Set;

import io.canvasmc.horizon.service.entrypoint.DedicatedServerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationNetworkHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.registry.sync.packet.DirectRegistryPacketHandler;

public class FabricRegistryInit implements DedicatedServerInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("FabricRegistrySync");
	private static final Set<String> VANILLA_NAMESPACES = Set.of(ResourceLocation.DEFAULT_NAMESPACE, "brigadier");

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.configurationC2S().register(SyncCompletePayload.ID, SyncCompletePayload.CODEC);
		PayloadTypeRegistry.configurationS2C().register(DirectRegistryPacketHandler.Payload.ID, DirectRegistryPacketHandler.Payload.CODEC);

		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register(RegistrySyncManager::configureClient);
		ServerConfigurationNetworking.registerGlobalReceiver(SyncCompletePayload.ID, (payload, context) ->
				((FabricServerConfigurationNetworkHandler) context.networkHandler()).completeTask(RegistrySyncManager.SyncConfigurationTask.TYPE));

		registerSyncedRegistries();
		try {
			markExistingModdedRegistries();
		} catch (Throwable t) {
			LOGGER.warn("Failed to mark existing modded registries: {}", t.toString());
		}
		RegistrySyncManager.bootstrapRegistries();
	}

	private static void registerSyncedRegistries() {
		RegistryAttributeHolder.get(Registries.SOUND_EVENT).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.FLUID).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.MOB_EFFECT).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.BLOCK).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.ENTITY_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.ITEM).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.POTION).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.PARTICLE_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.BLOCK_ENTITY_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.CUSTOM_STAT).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.ATTRIBUTE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.POSITION_SOURCE_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.COMMAND_ARGUMENT_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.MENU).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.STAT_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.VILLAGER_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.VILLAGER_PROFESSION).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.GAME_EVENT).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.NUMBER_FORMAT_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.DATA_COMPONENT_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.DATA_COMPONENT_PREDICATE_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.MAP_DECORATION_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.CONSUME_EFFECT_TYPE).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.RECIPE_DISPLAY).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.SLOT_DISPLAY).addAttribute(RegistryAttribute.SYNCED);
		RegistryAttributeHolder.get(Registries.RECIPE_BOOK_CATEGORY).addAttribute(RegistryAttribute.SYNCED);

		RegistryAttributeHolder.get(Registries.CARVER);
		RegistryAttributeHolder.get(Registries.FEATURE);
		RegistryAttributeHolder.get(Registries.BLOCK_STATE_PROVIDER_TYPE);
		RegistryAttributeHolder.get(Registries.FOLIAGE_PLACER_TYPE);
		RegistryAttributeHolder.get(Registries.TRUNK_PLACER_TYPE);
		RegistryAttributeHolder.get(Registries.TREE_DECORATOR_TYPE);
		RegistryAttributeHolder.get(Registries.FEATURE_SIZE_TYPE);
		RegistryAttributeHolder.get(Registries.BIOME_SOURCE);
		RegistryAttributeHolder.get(Registries.CHUNK_STATUS);
		RegistryAttributeHolder.get(Registries.STRUCTURE_TYPE);
		RegistryAttributeHolder.get(Registries.STRUCTURE_PIECE);
		RegistryAttributeHolder.get(Registries.RULE_TEST);
		RegistryAttributeHolder.get(Registries.POS_RULE_TEST);
		RegistryAttributeHolder.get(Registries.STRUCTURE_PROCESSOR);
		RegistryAttributeHolder.get(Registries.STRUCTURE_POOL_ELEMENT);
		RegistryAttributeHolder.get(Registries.RECIPE_TYPE);
		RegistryAttributeHolder.get(Registries.POINT_OF_INTEREST_TYPE);
		RegistryAttributeHolder.get(Registries.MEMORY_MODULE_TYPE);
		RegistryAttributeHolder.get(Registries.SENSOR_TYPE);
		RegistryAttributeHolder.get(Registries.SCHEDULE);
		RegistryAttributeHolder.get(Registries.ACTIVITY);
		RegistryAttributeHolder.get(Registries.LOOT_POOL_ENTRY_TYPE);
		RegistryAttributeHolder.get(Registries.LOOT_FUNCTION_TYPE);
		RegistryAttributeHolder.get(Registries.LOOT_CONDITION_TYPE);
	}

	private static void markExistingModdedRegistries() {
		for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
			boolean modded = registry.keySet().stream()
					.map(ResourceLocation::getNamespace)
					.anyMatch(namespace -> !VANILLA_NAMESPACES.contains(namespace));

			if (modded) {
				RegistryAttributeHolder.get(registry).addAttribute(RegistryAttribute.MODDED);
			}
		}
	}


}
