package dev.aero.runtime;

import dev.aero.api.state.GameState;
import dev.aero.api.state.PlayerInfo;
import dev.aero.api.state.ScreenInfo;
import dev.aero.api.state.WorldInfo;

/** Thin adapter from the internal {@link GameStateProvider} SPI to the public {@link GameState} API. */
final class GameStateImpl implements GameState {

    private final GameStateProvider provider;

    GameStateImpl(GameStateProvider provider) {
        this.provider = provider;
    }

    @Override
    public PlayerInfo player() {
        return provider.player();
    }

    @Override
    public WorldInfo world() {
        return provider.world();
    }

    @Override
    public ScreenInfo currentScreen() {
        return provider.currentScreen();
    }
}
