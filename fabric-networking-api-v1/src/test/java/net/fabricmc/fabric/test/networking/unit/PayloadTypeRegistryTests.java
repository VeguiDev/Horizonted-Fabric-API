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

package net.fabricmc.fabric.test.networking.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;

public class PayloadTypeRegistryTests {
	@Test
	void configurationRegistrationAndLookup() {
		CustomPacketPayload.Type<ConfigPayload> id = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("fabric_test", "config"));
		PayloadTypeRegistry.configurationC2S().register(id, ConfigPayload.CODEC);

		assertNotNull(PayloadTypeRegistryImpl.CONFIGURATION_C2S.get(id));
		assertEquals(ConnectionProtocol.CONFIGURATION, PayloadTypeRegistryImpl.CONFIGURATION_C2S.getPhase());
		assertEquals(PacketFlow.SERVERBOUND, PayloadTypeRegistryImpl.CONFIGURATION_C2S.getSide());
	}

	@Test
	void playRegistrationAndLookup() {
		CustomPacketPayload.Type<PlayPayload> id = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("fabric_test", "play"));
		PayloadTypeRegistry.playS2C().register(id, PlayPayload.CODEC);

		assertNotNull(PayloadTypeRegistryImpl.PLAY_S2C.get(id));
		assertEquals(ConnectionProtocol.PLAY, PayloadTypeRegistryImpl.PLAY_S2C.getPhase());
		assertEquals(PacketFlow.CLIENTBOUND, PayloadTypeRegistryImpl.PLAY_S2C.getSide());
	}

	@Test
	void duplicateRegistrationFails() {
		CustomPacketPayload.Type<ConfigPayload> id = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("fabric_test", "duplicate"));
		PayloadTypeRegistry.configurationS2C().register(id, ConfigPayload.CODEC);

		assertThrows(IllegalArgumentException.class, () -> PayloadTypeRegistry.configurationS2C().register(id, ConfigPayload.CODEC));
	}

	@Test
	void largeRegistrationStoresMaxPacketSize() {
		CustomPacketPayload.Type<ConfigPayload> id = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("fabric_test", "large"));
		PayloadTypeRegistry.configurationS2C().registerLarge(id, ConfigPayload.CODEC, 1024 * 1024);

		assertNotNull(PayloadTypeRegistryImpl.CONFIGURATION_S2C.get(id));
		assertEquals(true, PayloadTypeRegistryImpl.CONFIGURATION_S2C.getMaxPacketSize(id.id()) > 0);
	}

	@Test
	void configurationPayloadCodecRoundTrip() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		ConfigPayload expected = new ConfigPayload("hello");

		ConfigPayload.CODEC.encode(buf, expected);
		ConfigPayload actual = ConfigPayload.CODEC.decode(buf);

		assertEquals(expected.value(), actual.value());
	}

	@Test
	void playPayloadCodecRoundTrip() {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(PacketByteBufs.create(), RegistryAccess.EMPTY);
		PlayPayload expected = new PlayPayload("hello");

		PlayPayload.CODEC.encode(buf, expected);
		PlayPayload actual = PlayPayload.CODEC.decode(buf);

		assertEquals(expected.value(), actual.value());
	}

	private record ConfigPayload(String value) implements CustomPacketPayload {
		private static final CustomPacketPayload.Type<ConfigPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("fabric_test", "config_payload"));
		private static final StreamCodec<FriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.ofMember(
				(ConfigPayload payload, FriendlyByteBuf buf) -> buf.writeUtf(payload.value()),
				(FriendlyByteBuf buf) -> new ConfigPayload(buf.readUtf())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private record PlayPayload(String value) implements CustomPacketPayload {
		private static final CustomPacketPayload.Type<PlayPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("fabric_test", "play_payload"));
		private static final StreamCodec<RegistryFriendlyByteBuf, PlayPayload> CODEC = StreamCodec.ofMember(
				(PlayPayload payload, RegistryFriendlyByteBuf buf) -> buf.writeUtf(payload.value()),
				(RegistryFriendlyByteBuf buf) -> new PlayPayload(buf.readUtf())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
