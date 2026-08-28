package dev.aero.api.ui;

/**
 * A HUD element registered by a module via {@link dev.aero.api.ModuleContext#hud()}.
 * Registered elements are removed automatically when the module is disabled.
 */
public interface HudElement {
    void render(HudCanvas canvas);
}
