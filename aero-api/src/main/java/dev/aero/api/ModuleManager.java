package dev.aero.api;

import dev.aero.api.exception.ModuleException;

import java.util.Collection;
import java.util.Optional;

/**
 * Central point for driving module lifecycle. This is deliberately the whole
 * surface: later phases (a Community Modules UI, the {@code .aero} package
 * format, the marketplace, automatic updates, licensing, verification,
 * permissions) all become callers of these same seven operations rather than
 * needing their own lifecycle logic.
 */
public interface ModuleManager {

    /** Reads/validates the manifest and loads the module's code, but does not enable it. */
    InstalledModule install(ModulePackage pkg) throws ModuleException;

    void enable(String moduleId) throws ModuleException;

    void disable(String moduleId) throws ModuleException;

    /** Disables the current version (if enabled), disposes it, and installs+enables {@code pkg} in its place. */
    void update(String moduleId, ModulePackage pkg) throws ModuleException;

    /** Disables (if needed) and removes the module entirely. */
    void uninstall(String moduleId) throws ModuleException;

    Optional<InstalledModule> getModule(String moduleId);

    Collection<InstalledModule> getModules();
}
