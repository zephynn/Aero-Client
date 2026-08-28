package dev.aero.api.ui;

import dev.aero.api.event.Subscription;

/** Registers keybinds for the owning module; see {@link Keybind}. */
public interface KeybindRegistry {
    Subscription register(Keybind keybind);
}
