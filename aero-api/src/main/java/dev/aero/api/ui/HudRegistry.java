package dev.aero.api.ui;

import dev.aero.api.event.Subscription;

/** Registers HUD elements for the owning module; see {@link HudElement}. */
public interface HudRegistry {
    Subscription register(HudElement element);
}
