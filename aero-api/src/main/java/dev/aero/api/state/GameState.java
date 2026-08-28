package dev.aero.api.state;

/**
 * Controlled, read-only view of basic game state. This is the only way a
 * module observes Minecraft - it never receives a raw Minecraft object.
 * Every accessor may return {@code null} (e.g. {@link #player()} before a
 * world is loaded).
 */
public interface GameState {
    PlayerInfo player();

    WorldInfo world();

    ScreenInfo currentScreen();
}
