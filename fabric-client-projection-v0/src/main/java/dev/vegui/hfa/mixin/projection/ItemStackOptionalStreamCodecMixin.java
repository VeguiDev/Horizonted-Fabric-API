package dev.vegui.hfa.mixin.projection;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.vegui.hfa.impl.client.projection.v0.ClientProjectionRegistryImpl;

@Mixin(targets = "net.minecraft.world.item.ItemStack$2")
public abstract class ItemStackOptionalStreamCodecMixin {
    @ModifyVariable(
            method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private ItemStack hfa$projectItemStack(ItemStack stack, RegistryFriendlyByteBuf buf) {
        return ClientProjectionRegistryImpl.projectItemStackForNetwork(buf, stack);
    }
}
