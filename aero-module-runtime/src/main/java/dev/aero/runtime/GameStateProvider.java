package dev.aero.runtime;

import dev.aero.api.state.PlayerInfo;
import dev.aero.api.state.ScreenInfo;
import dev.aero.api.state.WorldInfo;

/**
 * Supplies live game state snapshots to the runtime. This module never
 * touches a Minecraft class directly - {@code aero-core} implements this
 * interface using real Minecraft/Fabric APIs and hands the implementation to
 * {@link ModuleRuntime}. That keeps {@code aero-module-runtime} buildable and
 * unit-testable with zero Minecraft/Fabric on its classpath.
 */
public interface GameStateProvider {
    PlayerInfo player();

    WorldInfo world();

    ScreenInfo currentScreen();

    GameStateProvider NONE = new GameStateProvider() {
        @Override
        public PlayerInfo player() {
            return null;
        }

        @Override
        public WorldInfo world() {
            return null;
        }

        @Override
        public ScreenInfo currentScreen() {
            return null;
        }
    };
}
