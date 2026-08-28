package dev.aero.api.state;

/**
 * Read-only snapshot of the currently open GUI screen. {@code null} means no
 * screen is open (i.e. the in-game HUD has focus). {@code screenId} is a
 * stable-ish identifier (e.g. the screen's simple class name) - never the
 * raw Minecraft {@code Screen} instance.
 */
public record ScreenInfo(String screenId) {
}
