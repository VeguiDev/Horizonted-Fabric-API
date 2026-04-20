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

package net.fabricmc.fabric.mixin.event.lifecycle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.impl.event.lifecycle.ChunkLevelTypeEventTracker;

@Mixin(NewChunkHolder.class)
public abstract class ChunkHolderMixin {
	@Shadow
	private ServerLevel world;

	@Shadow
	private ChunkHolder vanillaChunkHolder;

	@Unique
	private ChunkAccess currentChunk;

	@Shadow
	private FullChunkStatus currentFullChunkStatus;

	/**
	 * Moonrise handles full status changes in NewChunkHolder rather than ChunkHolder#updateFutures.
	 */
	@Inject(method = "updateCurrentState", at = @At("HEAD"))
	private void updateCurrentState(FullChunkStatus newLevelType, CallbackInfo ci) {
		ChunkLevelTypeEventTracker tracker = (ChunkLevelTypeEventTracker) this.vanillaChunkHolder;
		FullChunkStatus oldLevelType = this.currentFullChunkStatus;

		if (tracker.fabric_getCurrentEventLevelType() != oldLevelType) {
			return;
		}

		if (!(this.currentChunk instanceof LevelChunk worldChunk)) {
			return;
		}

		if (oldLevelType == newLevelType) {
			return;
		}

		ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.invoker().onChunkLevelTypeChange(this.world, worldChunk, oldLevelType, newLevelType);
		tracker.fabric_setCurrentEventLevelType(newLevelType);
	}
}
