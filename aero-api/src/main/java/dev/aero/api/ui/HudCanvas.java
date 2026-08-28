package dev.aero.api.ui;

/**
 * Aero-controlled drawing surface handed to modules for HUD rendering. This
 * deliberately does not expose the underlying Minecraft render context (e.g.
 * {@code GuiGraphics}) - a module can draw through this narrow surface, but
 * cannot reach into arbitrary Minecraft rendering internals.
 */
public interface HudCanvas {

    void drawText(String text, int x, int y, int argbColor);

    int screenWidth();

    int screenHeight();
}
