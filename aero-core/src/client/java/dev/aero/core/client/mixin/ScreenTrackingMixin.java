package dev.aero.core.client.mixin;

import dev.aero.core.client.ScreenTracker;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenTrackingMixin {

    @Inject(method = "added", at = @At("HEAD"))
    private void aero$onAdded(CallbackInfo ci) {
        ScreenTracker.setCurrent(getClass().getSimpleName());
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void aero$onRemoved(CallbackInfo ci) {
        ScreenTracker.clear();
    }
}
