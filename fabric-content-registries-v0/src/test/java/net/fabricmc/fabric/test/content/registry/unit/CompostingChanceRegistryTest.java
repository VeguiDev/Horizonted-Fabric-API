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

package net.fabricmc.fabric.test.content.registry.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ComposterBlock;

import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;

public class CompostingChanceRegistryTest {
    @BeforeAll
    static void beforeAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testAddAndGet() {
        CompostingChanceRegistry registry = CompostingChanceRegistry.INSTANCE;
        // Ensure obsidian is not already compostable
        Float initial = registry.get(Items.OBSIDIAN);
        // The default implementation returns 0.0F for non-existent entries
        assertEquals(0.0F, initial != null ? initial : 0.0F);

        // Add obsidian with chance 0.5F
        registry.add(Items.OBSIDIAN, 0.5F);
        Float chance = registry.get(Items.OBSIDIAN);
        assertEquals(0.5F, chance);

        // Verify the vanilla map was updated
        Float vanillaChance = ComposterBlock.COMPOSTABLES.getOrDefault(Items.OBSIDIAN, 0.0F);
        assertEquals(0.5F, vanillaChance);
    }

    @Test
    void testRemove() {
        CompostingChanceRegistry registry = CompostingChanceRegistry.INSTANCE;
        // Add first
        registry.add(Items.DIAMOND, 0.8F);
        assertEquals(0.8F, registry.get(Items.DIAMOND));

        // Remove
        registry.remove(Items.DIAMOND);
        Float chance = registry.get(Items.DIAMOND);
        assertEquals(0.0F, chance != null ? chance : 0.0F);
        // Verify removed from vanilla map
        Float vanillaChance = ComposterBlock.COMPOSTABLES.getOrDefault(Items.DIAMOND, 0.0F);
        assertEquals(0.0F, vanillaChance);
    }
}