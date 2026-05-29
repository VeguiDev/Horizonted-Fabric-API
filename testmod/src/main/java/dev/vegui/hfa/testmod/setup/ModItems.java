package dev.vegui.hfa.testmod.setup;

import dev.vegui.hfa.api.client.projection.v0.ClientProjectionRegistry;
import dev.vegui.hfa.api.client.projection.v0.ItemProjectionConfig;
import dev.vegui.hfa.testmod.TestMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Function;

public class ModItems {

    public static final Item TEST_ITEM = register("test_item", Item::new, new Item.Properties());

    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        try {
            // Create the item key.
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TestMod.MOD_ID, name));

            // Create the item instance.
            T item = itemFactory.apply(settings.setId(itemKey));

            // Register the item.
            Registry.register(BuiltInRegistries.ITEM, itemKey, item);

            return item;
        } catch (Exception e) {
            TestMod.LOGGER.error("Error while registering item: {}", name, e);
            throw e; // Rethrow to prevent silent failures.
        }
    }

    public static void initialize() {

//        ClientProjectionRegistry.registerItemProjection(
//                ModItems.TEST_ITEM,
//                new ItemProjectionConfig(Items.PAPER, "hfa_test_mod:test_item")
//        );
    }
}
