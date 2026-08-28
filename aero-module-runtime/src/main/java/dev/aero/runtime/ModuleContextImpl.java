package dev.aero.runtime;

import dev.aero.api.ModuleContext;
import dev.aero.api.ModuleManifest;
import dev.aero.api.config.ModuleConfig;
import dev.aero.api.event.EventBus;
import dev.aero.api.state.GameState;
import dev.aero.api.ui.HudRegistry;
import dev.aero.api.ui.KeybindRegistry;
import dev.aero.runtime.config.InMemoryModuleConfig;

final class ModuleContextImpl implements ModuleContext {

    private final ModuleManifest manifest;
    private final EventBus events;
    private final KeybindRegistry keybinds;
    private final HudRegistry hud;
    private final GameState gameState;
    private final ModuleConfig config = new InMemoryModuleConfig();
    private final RuntimeLog log;

    ModuleContextImpl(
            ModuleManifest manifest,
            EventBus events,
            KeybindRegistry keybinds,
            HudRegistry hud,
            GameState gameState,
            RuntimeLog log
    ) {
        this.manifest = manifest;
        this.events = events;
        this.keybinds = keybinds;
        this.hud = hud;
        this.gameState = gameState;
        this.log = log;
    }

    @Override
    public ModuleManifest manifest() {
        return manifest;
    }

    @Override
    public EventBus events() {
        return events;
    }

    @Override
    public KeybindRegistry keybinds() {
        return keybinds;
    }

    @Override
    public HudRegistry hud() {
        return hud;
    }

    @Override
    public GameState gameState() {
        return gameState;
    }

    @Override
    public ModuleConfig config() {
        return config;
    }

    @Override
    public void log(String message) {
        log.info("[" + manifest.id() + "] " + message);
    }
}
