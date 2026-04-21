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
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Supplier;

public final class OxidizableBlocksRegistryImpl {
	private static final Unsafe UNSAFE = getUnsafe();
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
			setStaticField(WeatheringCopper.class, "NEXT_BY_BLOCK", nextSupplier);
			setStaticField(WeatheringCopper.class, "PREVIOUS_BY_BLOCK", previousSupplier);
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
			setStaticField(HoneycombItem.class, "WAXABLES", waxablesSupplier);
			setStaticField(HoneycombItem.class, "WAX_OFF_BY_BLOCK", unwaxedSupplier);
			waxablesAreMutable = true;
		}
	}

	private static void refreshRandomTickCache(Block block) {
		block.getStateDefinition().getPossibleStates().forEach(state -> ((RandomTickCacheRefresher) state).fabric_api$refreshRandomTickCache());
	}

	private static void setStaticField(Class<?> owner, String fieldName, Object value) {
		try {
			Field field = owner.getDeclaredField(fieldName);
			Object base = UNSAFE.staticFieldBase(field);
			long offset = UNSAFE.staticFieldOffset(field);
			UNSAFE.putObjectVolatile(base, offset, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to replace " + owner.getName() + "::" + fieldName, e);
		}
	}

	private static Unsafe getUnsafe() {
		try {
			Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			return (Unsafe) unsafeField.get(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to access sun.misc.Unsafe", e);
		}
	}

	public interface RandomTickCacheRefresher {
		void fabric_api$refreshRandomTickCache();
	}
}
