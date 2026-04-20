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

package net.fabricmc.fabric.mixin.registry.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.fabricmc.fabric.impl.registry.sync.ListenableRegistry;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> implements ListenableRegistry<T>, FabricRegistry {
	@Unique
	private static final Set<String> VANILLA_NAMESPACES = Set.of(ResourceLocation.DEFAULT_NAMESPACE, "brigadier");

	@Shadow
	@Final
	private Map<ResourceLocation, Holder.Reference<T>> byLocation;
	@Shadow
	@Final
	private Reference2IntMap<T> toId;
	@Shadow
	@Final
	private ResourceKey<? extends Registry<T>> key;

	@Unique
	private Event<RegistryEntryAddedCallback<T>> fabricAddObjectEvent;
	@Unique
	private Event<RegistryIdRemapCallback<T>> fabricPostRemapEvent;
	@Unique
	private Map<ResourceLocation, ResourceLocation> aliases = new HashMap<>();

	@Inject(method = "<init>(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;Z)V", at = @At("RETURN"))
	private void init(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, boolean hasIntrusiveHolders, CallbackInfo ci) {
		fabricAddObjectEvent = EventFactory.createArrayBacked(RegistryEntryAddedCallback.class, callbacks -> (rawId, id, object) -> {
			for (RegistryEntryAddedCallback<T> callback : callbacks) {
				callback.onEntryAdded(rawId, id, object);
			}
		});
		fabricPostRemapEvent = EventFactory.createArrayBacked(RegistryIdRemapCallback.class, callbacks -> state -> {
			for (RegistryIdRemapCallback<T> callback : callbacks) {
				callback.onRemap(state);
			}
		});
	}

	@Inject(method = "register", at = @At("RETURN"))
	private void onRegister(ResourceKey<T> key, T value, RegistrationInfo registrationInfo, CallbackInfoReturnable<Holder.Reference<T>> cir) {
		fabricAddObjectEvent.invoker().onEntryAdded(toId.getInt(value), key.location(), value);
		onChange(key);
	}

	@Override
	public Event<RegistryEntryAddedCallback<T>> fabric_getAddObjectEvent() {
		return fabricAddObjectEvent;
	}

	@Override
	public Event<RegistryIdRemapCallback<T>> fabric_getRemapEvent() {
		return fabricPostRemapEvent;
	}

	@Override
	public void addAlias(ResourceLocation old, ResourceLocation newId) {
		Objects.requireNonNull(old, "alias cannot be null");
		Objects.requireNonNull(newId, "aliased id cannot be null");

		if (aliases.containsKey(old)) {
			throw new IllegalArgumentException("Tried adding %s as an alias for %s, but it is already an alias (for %s) in registry %s".formatted(old, newId, aliases.get(old), this.key));
		}

		if (byLocation.containsKey(old)) {
			throw new IllegalArgumentException("Tried adding %s as an alias, but it is already present in registry %s".formatted(old, this.key));
		}

		if (old.equals(aliases.get(newId))) {
			throw new IllegalArgumentException("Making %1$s an alias of %2$s would create a cycle, as %2$s is already an alias of %1$s (registry %3$s)".formatted(old, newId, this.key));
		}

		ResourceLocation deepest = aliases.getOrDefault(newId, newId);

		for (Map.Entry<ResourceLocation, ResourceLocation> entry : aliases.entrySet()) {
			if (old.equals(entry.getValue())) {
				entry.setValue(deepest);
			}
		}

		aliases.put(old, deepest);
	}

	@ModifyVariable(
			method = {
					"get(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;",
					"getValue(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;",
					"containsKey(Lnet/minecraft/resources/ResourceLocation;)Z"
			},
			at = @At("HEAD"),
			argsOnly = true
	)
	private ResourceLocation aliasResourceLocationParameter(ResourceLocation original) {
		return aliases.getOrDefault(original, original);
	}

	@ModifyVariable(
			method = {
					"get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
					"getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
					"containsKey(Lnet/minecraft/resources/ResourceKey;)Z"
			},
			at = @At("HEAD"),
			argsOnly = true
	)
	private ResourceKey<T> aliasRegistryKeyParameter(ResourceKey<T> original) {
		if (original == null) {
			return null;
		}

		ResourceLocation aliased = aliases.get(original.location());
		return aliased == null ? original : ResourceKey.create(original.registryKey(), aliased);
	}

	@Unique
	private void onChange(ResourceKey<T> registryKey) {
		if (RegistrySyncManager.postBootstrap || !VANILLA_NAMESPACES.contains(registryKey.location().getNamespace())) {
			RegistryAttributeHolder holder = RegistryAttributeHolder.get(this.key);

			if (!holder.hasAttribute(RegistryAttribute.MODDED)) {
				RegistryAttributeHolder.get(this.key).addAttribute(RegistryAttribute.MODDED);
			}
		}
	}
}
