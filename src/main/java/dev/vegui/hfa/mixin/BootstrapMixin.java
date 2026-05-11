package dev.vegui.hfa.mixin;

import io.canvasmc.horizon.service.entrypoint.EntrypointContainer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class BootstrapMixin {

    @Inject(
        method = "bootStrap",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/registries/BuiltInRegistries;bootStrap(Ljava/lang/Runnable;)V"
        )
    )
    private static void hfa$runMainEntrypoints(CallbackInfo ci) {
        EntrypointContainer.buildProvider("main", ModInitializer.class, Void.class).invoke();
    }
}
