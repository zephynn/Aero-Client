package dev.aero.runtime;

import dev.aero.api.InstalledModule;
import dev.aero.api.Module;
import dev.aero.api.ModuleContext;
import dev.aero.api.ModuleManifest;
import dev.aero.api.ModuleState;
import dev.aero.runtime.classloader.ModuleClassLoader;
import dev.aero.runtime.registry.ModuleRegistrations;

/**
 * Mutable internal record of one installed module. Deliberately holds the
 * *only* strong references {@code aero-module-runtime} keeps to a module's
 * instance and classloader - {@link ModuleManagerImpl#uninstall} drops this
 * object entirely (and {@link #clear()}s its fields first) so nothing here
 * keeps the module's classloader reachable after removal.
 */
final class ModuleHandleImpl implements InstalledModule {

    private final ModuleManifest manifest;
    private final ModuleRegistrations registrations = new ModuleRegistrations();
    private volatile ModuleState state;
    private ModuleClassLoader classLoader;
    private Module moduleInstance;
    private ModuleContext context;

    ModuleHandleImpl(ModuleManifest manifest) {
        this.manifest = manifest;
        this.state = ModuleState.DISCOVERED;
    }

    @Override
    public String id() {
        return manifest.id();
    }

    @Override
    public ModuleManifest manifest() {
        return manifest;
    }

    @Override
    public ModuleState state() {
        return state;
    }

    void setState(ModuleState state) {
        this.state = state;
    }

    ModuleRegistrations registrations() {
        return registrations;
    }

    ModuleClassLoader classLoader() {
        return classLoader;
    }

    void setClassLoader(ModuleClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    Module moduleInstance() {
        return moduleInstance;
    }

    void setModuleInstance(Module moduleInstance) {
        this.moduleInstance = moduleInstance;
    }

    ModuleContext context() {
        return context;
    }

    void setContext(ModuleContext context) {
        this.context = context;
    }

    /**
     * Drops every reference this handle holds. After this call, nothing in
     * {@code aero-module-runtime} keeps the module's classloader reachable;
     * whether it is actually collected is up to the JVM (there is no
     * {@code unloadClass()} in Java - see the module README).
     */
    void clear() {
        moduleInstance = null;
        classLoader = null;
        context = null;
    }
}
