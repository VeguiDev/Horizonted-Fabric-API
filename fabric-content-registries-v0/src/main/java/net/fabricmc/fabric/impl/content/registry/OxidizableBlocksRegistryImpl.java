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

package net.fabricmc.fabric.impl.content.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.fabricmc.fabric.mixin.content.registry.HoneycombItemAccessor;
import net.fabricmc.fabric.mixin.content.registry.WeatheringCopperAccessor;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;

import java.util.Objects;
import java.util.function.Supplier;

public final class OxidizableBlocksRegistryImpl {
	private static volatile boolean waxablesAreMutable;
	private static volatile boolean oxidizablesAreMutable;

	private OxidizableBlocksRegistryImpl() {
	}

	public static void registerOxidizableBlockPair(Block less, Block more) {
		Objects.requireNonNull(less, "Oxidizable block cannot be null!");
		Objects.requireNonNull(more, "Oxidizable block cannot be null!");
		ensureOxidizableMapIsMutable();
		WeatheringCopper.NEXT_BY_BLOCK.get().put(less, more);
		// Fix #4371
		refreshRandomTickCache(less);
		refreshRandomTickCache(more);
	}

	public static void registerWaxableBlockPair(Block unwaxed, Block waxed) {
		Objects.requireNonNull(unwaxed, "Unwaxed block cannot be null!");
		Objects.requireNonNull(waxed, "Waxed block cannot be null!");
		ensureWaxableMapIsMutable();
		HoneycombItem.WAXABLES.get().put(unwaxed, waxed);
	}

	private static void ensureOxidizableMapIsMutable() {
		if (oxidizablesAreMutable) {
			return;
		}

		synchronized (OxidizableBlocksRegistryImpl.class) {
			if (oxidizablesAreMutable) {
				return;
			}

			BiMap<Block, Block> mutableMap = HashBiMap.create(WeatheringCopperAccessor.fabric_getNextByBlockSupplier().get());
			Supplier<BiMap<Block, Block>> nextSupplier = () -> mutableMap;
			Supplier<BiMap<Block, Block>> previousSupplier = mutableMap::inverse;
			WeatheringCopperAccessor.fabric_setNextByBlockSupplier(nextSupplier);
			WeatheringCopperAccessor.fabric_setPreviousByBlockSupplier(previousSupplier);
			oxidizablesAreMutable = true;
		}
	}

	private static void ensureWaxableMapIsMutable() {
		if (waxablesAreMutable) {
			return;
		}

		synchronized (OxidizableBlocksRegistryImpl.class) {
			if (waxablesAreMutable) {
				return;
			}

			BiMap<Block, Block> mutableMap = HashBiMap.create(HoneycombItemAccessor.fabric_getWaxablesSupplier().get());
			Supplier<BiMap<Block, Block>> waxablesSupplier = () -> mutableMap;
			Supplier<BiMap<Block, Block>> unwaxedSupplier = mutableMap::inverse;
			HoneycombItemAccessor.fabric_setWaxablesSupplier(waxablesSupplier);
			HoneycombItemAccessor.fabric_setWaxOffByBlockSupplier(unwaxedSupplier);
			waxablesAreMutable = true;
		}
	}

	private static void refreshRandomTickCache(Block block) {
		block.getStateDefinition().getPossibleStates().forEach(state -> ((RandomTickCacheRefresher) state).fabric_api$refreshRandomTickCache());
	}

	public interface RandomTickCacheRefresher {
		void fabric_api$refreshRandomTickCache();
	}
}
