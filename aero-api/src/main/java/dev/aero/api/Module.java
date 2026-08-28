package dev.aero.api;

/**
 * Entry point implemented by a Community Module.
 *
 * <p>A module never touches Minecraft, Fabric, or Aero internals directly -
 * everything it can do is reached through the {@link ModuleContext} handed
 * to it here. This is what makes it safe for the Aero Module Runtime to
 * enable, disable, update, and unload a module while the game keeps running.
 */
public interface Module {

    /**
     * Called once, immediately after the module class is instantiated, before
     * {@link #onEnable}. Use this only for setup that must not depend on the
     * module being active yet (most modules can leave this empty).
     */
    default void onLoad(ModuleContext context) {
    }

    /**
     * Called when the module becomes active. Register tick/render listeners,
     * keybinds, and HUD elements here via {@code context} - never store them
     * anywhere Aero can't reach, or they won't be cleaned up on disable.
     */
    void onEnable(ModuleContext context);

    /**
     * Called when the module is disabled, updated, or uninstalled. The
     * runtime already unregisters everything the module registered through
     * {@code context} before or after calling this (see the runtime's
     * lifecycle docs); use this method for any *additional* cleanup the
     * module owns itself (closing files, stopping its own threads, etc).
     */
    void onDisable(ModuleContext context);
}
