package dev.aero.runtime;

import dev.aero.api.ModuleManager;
import dev.aero.api.event.RenderEvent;
import dev.aero.api.ui.HudCanvas;
import dev.aero.runtime.failure.FailureTracker;
import dev.aero.runtime.ui.HudRegistryImpl;
import dev.aero.runtime.ui.KeybindRegistryImpl;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Top-level facade {@code aero-core} drives once per client tick and once
 * per rendered frame. This is the entire integration surface between the
 * Minecraft-facing side of Aero and the Minecraft-agnostic module engine.
 */
public final class ModuleRuntime {

    private final ModuleManagerImpl manager;
    private final AtomicLong tickCounter = new AtomicLong();

    public ModuleRuntime(ClassLoader aeroClassLoader, RuntimeLog log, GameStateProvider gameStateProvider) {
        this(aeroClassLoader, log, gameStateProvider, new FailureTracker());
    }

    public ModuleRuntime(
            ClassLoader aeroClassLoader, RuntimeLog log, GameStateProvider gameStateProvider, FailureTracker failureTracker) {
        this.manager = new ModuleManagerImpl(aeroClassLoader, log, gameStateProvider, failureTracker);
    }

    public ModuleManager modules() {
        return manager;
    }

    public void setGameStateProvider(GameStateProvider provider) {
        manager.setGameStateProvider(provider);
    }

    public KeybindRegistryImpl keybinds() {
        return manager.keybindRegistry();
    }

    public HudRegistryImpl hud() {
        return manager.hudRegistry();
    }

    /** Call once per client tick. */
    public void tick() {
        manager.tick(tickCounter.incrementAndGet());
    }

    /** Call once per rendered frame, after drawing the vanilla HUD. */
    public void render(HudCanvas canvas, float partialTick) {
        manager.eventBus().dispatch(new RenderEvent(canvas, partialTick));
        manager.hudRegistry().renderAll(canvas);
    }
}
