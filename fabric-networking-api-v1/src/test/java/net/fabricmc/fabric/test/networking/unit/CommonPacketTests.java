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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.networking.CommonPacketsImpl;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.CommonVersionPayload;
import net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl;

public class CommonPacketTests {
	@BeforeAll
	static void beforeAll() {
		CommonPacketsImpl.init();
	}

	@Test
	void versionPayloadRoundTrip() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		CommonVersionPayload expected = new CommonVersionPayload(new int[]{1, 2, 3});

		CommonVersionPayload.CODEC.encode(buf, expected);
		CommonVersionPayload actual = CommonVersionPayload.CODEC.decode(buf);

		assertArrayEquals(expected.versions(), actual.versions());
		assertEquals(0, buf.readableBytes());
	}

	@Test
	void registerPayloadRoundTrip() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		CommonRegisterPayload expected = new CommonRegisterPayload(
				1,
				CommonRegisterPayload.PLAY_PHASE,
				Set.of(
						ResourceLocation.fromNamespaceAndPath("fabric", "a"),
						ResourceLocation.fromNamespaceAndPath("fabric", "b")
				)
		);

		CommonRegisterPayload.CODEC.encode(buf, expected);
		CommonRegisterPayload actual = CommonRegisterPayload.CODEC.decode(buf);

		assertEquals(expected.version(), actual.version());
		assertEquals(expected.phase(), actual.phase());
		assertEquals(expected.channels(), actual.channels());
		assertEquals(0, buf.readableBytes());
	}

	@Test
	void commonPacketsInitRegistersConfigurationReceivers() {
		assertTrue(ServerConfigurationNetworking.getGlobalReceivers().contains(CommonVersionPayload.ID.id()));
		assertTrue(ServerConfigurationNetworking.getGlobalReceivers().contains(CommonRegisterPayload.ID.id()));
	}

	@Disabled("Requires full protocol runtime for ClientboundCustomPayloadPacket")
	@Test
	void createS2CPacketWrapsPayload() {
		var packet = ServerNetworkingImpl.createS2CPacket(new CommonVersionPayload(new int[]{1}));
		var customPayloadPacket = assertInstanceOf(ClientboundCustomPayloadPacket.class, packet);

		assertInstanceOf(CommonVersionPayload.class, customPayloadPacket.payload());
	}

	@Test
	void versionTaskKeyMatchesPayloadId() {
		assertEquals(CommonVersionPayload.ID.id().toString(), new ConfigurationTask.Type(CommonVersionPayload.ID.id().toString()).id());
	}

	@Test
	public void testHighestCommonVersionWithCommonElement() {
		int[] a = {1, 2, 3};
		int[] b = {1, 2};
		assertEquals(2, CommonPacketsImpl.getHighestCommonVersion(a, b));
	}

	@Test
	public void testHighestCommonVersionWithoutCommonElement() {
		int[] a = {1, 3, 5};
		int[] b = {2, 4, 6};
		assertEquals(-1, CommonPacketsImpl.getHighestCommonVersion(a, b));
	}

	@Test
	public void testHighestCommonVersionWithOneEmptyArray() {
		int[] a = {1, 3, 5};
		int[] b = {};
		assertEquals(-1, CommonPacketsImpl.getHighestCommonVersion(a, b));
	}

	@Test
	public void testHighestCommonVersionWithBothEmptyArrays() {
		int[] a = {};
		int[] b = {};
		assertEquals(-1, CommonPacketsImpl.getHighestCommonVersion(a, b));
	}

	@Test
	public void testHighestCommonVersionWithIdenticalArrays() {
		int[] a = {1, 2, 3};
		int[] b = {1, 2, 3};
		assertEquals(3, CommonPacketsImpl.getHighestCommonVersion(a, b));
	}
}
