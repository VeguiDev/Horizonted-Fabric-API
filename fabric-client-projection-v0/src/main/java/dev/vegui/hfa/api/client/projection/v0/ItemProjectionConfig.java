package dev.vegui.hfa.api.client.projection.v0;

import java.util.Objects;

import net.minecraft.world.item.Item;

public record ItemProjectionConfig(Item item, String customModelData) {
    public ItemProjectionConfig {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(customModelData, "customModelData");

        if (customModelData.isBlank()) {
            throw new IllegalArgumentException("customModelData cannot be blank");
        }
    }
}
