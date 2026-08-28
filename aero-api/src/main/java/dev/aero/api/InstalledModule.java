package dev.aero.api;

/** Read-only view of a module the {@link ModuleManager} knows about. */
public interface InstalledModule {
    String id();

    ModuleManifest manifest();

    ModuleState state();
}
