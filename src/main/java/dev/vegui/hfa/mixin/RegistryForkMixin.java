package dev.vegui.hfa.mixin;

import dev.vegui.hfa.registry.RegistryFork;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class RegistryForkMixin<T> {
    @Shadow
    @Final
    private ResourceKey<? extends Registry<T>> key;

    @Inject(method = "register", at = @At("RETURN"))
    private void hfa$markCustomRegistryEntry(ResourceKey<T> entryKey, T value, RegistrationInfo registrationInfo, CallbackInfoReturnable<?> cir) {
        RegistryFork.markCustomEntry(this.key, entryKey.location());
    }
}
