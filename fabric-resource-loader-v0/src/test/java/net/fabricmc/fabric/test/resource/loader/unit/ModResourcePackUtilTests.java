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

package net.fabricmc.fabric.test.resource.loader.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;

public class ModResourcePackUtilTests {
	private static final Gson GSON = new Gson();

	@BeforeAll
	static void beforeAll() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void testSerializeMetadata() {
		testMetadataSerialization("");
		testMetadataSerialization("Quotes: \"\" \"");
		testMetadataSerialization("Backslash: \\ \\\\");
	}

	@Disabled("Requires mixin-applied Pack/FabricResourcePackProfile runtime to test auto-enabled pack ordering")
	@Test
	void testRefreshAutoEnabledPacks() {
	}

	private void testMetadataSerialization(String description) {
		String metadata = ModResourcePackUtil.serializeMetadata(1, description);
		JsonObject json = assertDoesNotThrow(() -> GSON.fromJson(metadata, JsonObject.class), () -> "Failed to serialize " + description);

		String parsedDescription = json.get("pack").getAsJsonObject().get("description").getAsString();
		assertEquals(description, parsedDescription, "Parsed description differs from original one");
	}
}
