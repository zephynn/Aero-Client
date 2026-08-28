package dev.aero.api;

import dev.aero.api.config.ModuleConfig;
import dev.aero.api.event.EventBus;
import dev.aero.api.state.GameState;
import dev.aero.api.ui.HudRegistry;
import dev.aero.api.ui.KeybindRegistry;

/**
 * Everything a {@link Module} is given to interact with Aero. A module holds
 * no other reference into Aero - this is the entire surface area, and every
 * registration made through it is scoped to the module and torn down
 * automatically when the module is disabled.
 */
public interface ModuleContext {

    ModuleManifest manifest();

    EventBus events();

    KeybindRegistry keybinds();

    HudRegistry hud();

    GameState gameState();

    ModuleConfig config();

    /** Logs a message tagged with this module's id, via Aero's own logger. */
    void log(String message);
}
