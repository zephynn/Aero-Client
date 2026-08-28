package dev.aero.core.client;

import dev.aero.api.state.ScreenInfo;

/**
 * Fed by {@code ScreenTrackingMixin}. This is the one place aero-core uses a
 * Mixin (rather than a Fabric API callback) to hook Minecraft internals
 * directly, precisely to demonstrate the architecture: Minecraft -> Aero's
 * internal hook (the mixin) -> this tracker -> {@code GameStateProviderImpl}
 * -> the {@link ScreenInfo} abstraction a Community Module actually sees. A
 * module never touches the real {@code Screen} instance.
 */
public final class ScreenTracker {

    private static volatile String currentScreenId;

    private ScreenTracker() {
    }

    public static void setCurrent(String screenId) {
        currentScreenId = screenId;
    }

    public static void clear() {
        currentScreenId = null;
    }

    public static ScreenInfo current() {
        String id = currentScreenId;
        return id == null ? null : new ScreenInfo(id);
    }
}
