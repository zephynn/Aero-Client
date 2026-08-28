package dev.aero.api;

/** Lifecycle state of an installed module, in the order a module normally moves through them. */
public enum ModuleState {
    /** Manifest read and validated, but no code loaded yet. */
    DISCOVERED,
    /** Classloader created, entrypoint class loaded and instantiated, {@code onLoad} called. */
    LOADED,
    /** {@code onEnable} has run; the module is receiving events. */
    ENABLED,
    /** {@code onDisable} has run and all registrations were torn down. */
    DISABLED,
    /** The module was disabled automatically after repeated callback failures. */
    FAILED,
    /** Fully removed; its classloader is no longer referenced by the runtime. */
    UNLOADED
}
