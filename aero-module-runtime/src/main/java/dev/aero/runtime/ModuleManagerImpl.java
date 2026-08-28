package dev.aero.runtime;

import dev.aero.api.InstalledModule;
import dev.aero.api.Module;
import dev.aero.api.ModuleContext;
import dev.aero.api.ModuleManager;
import dev.aero.api.ModuleManifest;
import dev.aero.api.ModulePackage;
import dev.aero.api.ModuleState;
import dev.aero.api.exception.ManifestException;
import dev.aero.api.exception.ModuleException;
import dev.aero.runtime.classloader.ModuleClassLoader;
import dev.aero.runtime.event.EventBusImpl;
import dev.aero.runtime.failure.FailureCallback;
import dev.aero.runtime.failure.FailureTracker;
import dev.aero.runtime.ui.HudRegistryImpl;
import dev.aero.runtime.ui.KeybindRegistryImpl;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the module lifecycle described in the Phase 1 spec:
 *
 * <pre>
 * discover -> read manifest -> validate -> create classloader -> load
 * entrypoint -> instantiate -> onLoad/onEnable -> registered
 * </pre>
 *
 * A module failure is always caught at this boundary; it is logged with the
 * module id, version, and stack trace, and (for repeated callback failures,
 * via {@link FailureTracker}) can result in the module being disabled
 * automatically. Nothing here ever lets a module exception propagate to the
 * caller of {@link #tick} / HUD render / keybind dispatch.
 */
public final class ModuleManagerImpl implements ModuleManager {

    private final Map<String, ModuleHandleImpl> modules = new ConcurrentHashMap<>();
    private final ClassLoader aeroClassLoader;
    private final EventBusImpl eventBus;
    private final HudRegistryImpl hudRegistry;
    private final KeybindRegistryImpl keybindRegistry;
    private final FailureTracker failureTracker;
    private final RuntimeLog log;
    private volatile GameStateProvider gameStateProvider;

    public ModuleManagerImpl(ClassLoader aeroClassLoader, RuntimeLog log, GameStateProvider gameStateProvider) {
        this(aeroClassLoader, log, gameStateProvider, new FailureTracker());
    }

    public ModuleManagerImpl(
            ClassLoader aeroClassLoader, RuntimeLog log, GameStateProvider gameStateProvider, FailureTracker failureTracker) {
        this.aeroClassLoader = aeroClassLoader;
        this.log = log;
        this.gameStateProvider = gameStateProvider;
        this.failureTracker = failureTracker;

        FailureCallback failureCallback = new FailureCallback() {
            @Override
            public void onSuccess(String moduleId) {
                failureTracker.recordSuccess(moduleId);
            }

            @Override
            public void onFailure(String moduleId) {
                if (failureTracker.recordFailure(moduleId)) {
                    autoDisable(moduleId);
                }
            }
        };

        this.eventBus = new EventBusImpl(failureCallback, (moduleId, eventType, failure) ->
                log.error("Module '" + moduleId + "' threw from a " + eventType + " listener - disabling after "
                        + failureTracker.threshold() + " consecutive failures if this continues", failure));
        this.hudRegistry = new HudRegistryImpl(failureCallback, (moduleId, failure) ->
                log.error("Module '" + moduleId + "' threw while rendering its HUD element", failure));
        this.keybindRegistry = new KeybindRegistryImpl(
                failureCallback,
                (moduleId, keybindId, failure) ->
                        log.error("Module '" + moduleId + "' threw handling keybind '" + keybindId + "'", failure)
        );
    }

    public EventBusImpl eventBus() {
        return eventBus;
    }

    public HudRegistryImpl hudRegistry() {
        return hudRegistry;
    }

    public KeybindRegistryImpl keybindRegistry() {
        return keybindRegistry;
    }

    public void setGameStateProvider(GameStateProvider gameStateProvider) {
        this.gameStateProvider = gameStateProvider;
    }

    /** Advances Aero's tick counter and dispatches a {@code TickEvent} to every enabled module. */
    public void tick(long tickCount) {
        eventBus.dispatch(new dev.aero.api.event.TickEvent(tickCount));
    }

    // ---- ModuleManager -----------------------------------------------

    @Override
    public InstalledModule install(ModulePackage pkg) throws ModuleException {
        ModuleManifest manifest = pkg.manifest();

        ModuleHandleImpl existing = modules.get(manifest.id());
        if (existing != null && existing.state() != ModuleState.UNLOADED) {
            throw new ModuleException(
                    "Module '" + manifest.id() + "' is already installed (state=" + existing.state() + "); use update() instead");
        }

        ModuleHandleImpl handle = new ModuleHandleImpl(manifest);
        modules.put(manifest.id(), handle);

        try {
            URL[] classpath = pkg.classpathUrls();
            ModuleClassLoader classLoader = new ModuleClassLoader(manifest.id(), classpath, aeroClassLoader);
            handle.setClassLoader(classLoader);

            Module instance = instantiateEntrypoint(manifest, classLoader);
            handle.setModuleInstance(instance);

            ModuleContext context = buildContext(handle);
            handle.setContext(context);

            handle.setState(ModuleState.LOADED);
            try {
                instance.onLoad(context);
            } catch (Throwable t) {
                throw new ModuleException("Module '" + manifest.id() + "' threw from onLoad()", t);
            }

            return handle;
        } catch (ModuleException e) {
            rollbackFailedInstall(handle);
            throw e;
        } catch (Throwable t) {
            rollbackFailedInstall(handle);
            throw new ModuleException("Failed to install module '" + manifest.id() + "': " + t.getMessage(), t);
        }
    }

    @Override
    public void enable(String moduleId) throws ModuleException {
        ModuleHandleImpl handle = requireHandle(moduleId);
        if (handle.state() == ModuleState.ENABLED) {
            return;
        }
        if (handle.state() != ModuleState.LOADED && handle.state() != ModuleState.DISABLED
                && handle.state() != ModuleState.FAILED) {
            throw new ModuleException("Cannot enable module '" + moduleId + "' from state " + handle.state());
        }
        try {
            handle.moduleInstance().onEnable(handle.context());
        } catch (Throwable t) {
            log.error("Module '" + moduleId + "' threw from onEnable() - rolling back its registrations", t);
            handle.registrations().cancelAll();
            handle.setState(ModuleState.FAILED);
            throw new ModuleException("Module '" + moduleId + "' failed to enable: " + t.getMessage(), t);
        }
        handle.setState(ModuleState.ENABLED);
        failureTracker.reset(moduleId);
    }

    @Override
    public void disable(String moduleId) throws ModuleException {
        ModuleHandleImpl handle = requireHandle(moduleId);
        disableHandle(handle, ModuleState.DISABLED);
    }

    @Override
    public void update(String moduleId, ModulePackage pkg) throws ModuleException {
        ModuleHandleImpl old = requireHandle(moduleId);
        boolean wasEnabled = old.state() == ModuleState.ENABLED;

        // Validate the replacement *before* touching the running module at all -
        // a bad path, unreadable jar, or malformed manifest must leave the
        // currently-running version completely untouched.
        ModuleManifest newManifest = pkg.manifest();
        if (!newManifest.id().equals(moduleId)) {
            throw new ModuleException("Cannot update '" + moduleId + "': replacement manifest declares id '"
                    + newManifest.id() + "'");
        }

        // Temporarily vacate the id so install() doesn't reject it as a
        // duplicate, but keep `old` fully intact - if install() below fails,
        // put it right back with nothing lost (it was never disabled or
        // disposed).
        modules.remove(moduleId);
        try {
            install(pkg);
        } catch (ModuleException e) {
            modules.put(moduleId, old);
            throw new ModuleException(
                    "Update failed, kept running the previous version of '" + moduleId + "': " + e.getMessage(), e);
        }

        // The new version is confirmed installed - only now is it safe to retire the old one.
        if (wasEnabled) {
            disableHandle(old, ModuleState.DISABLED);
        }
        disposeHandle(old);

        if (wasEnabled) {
            enable(moduleId);
        }
    }

    @Override
    public void uninstall(String moduleId) throws ModuleException {
        ModuleHandleImpl handle = requireHandle(moduleId);
        if (handle.state() == ModuleState.ENABLED) {
            disableHandle(handle, ModuleState.DISABLED);
        }
        disposeHandle(handle);
        modules.remove(moduleId);
    }

    @Override
    public Optional<InstalledModule> getModule(String moduleId) {
        return Optional.ofNullable(modules.get(moduleId));
    }

    @Override
    public Collection<InstalledModule> getModules() {
        return modules.values().stream()
                .sorted(Comparator.comparing(InstalledModule::id))
                .map(InstalledModule.class::cast)
                .toList();
    }

    // ---- internals ------------------------------------------------------

    private void autoDisable(String moduleId) {
        ModuleHandleImpl handle = modules.get(moduleId);
        if (handle == null || handle.state() != ModuleState.ENABLED) {
            return;
        }
        log.error("Module '" + moduleId + "' (v" + handle.manifest().version() + ") exceeded "
                + failureTracker.threshold() + " consecutive callback failures - disabling automatically", null);
        try {
            disableHandle(handle, ModuleState.FAILED);
        } catch (ModuleException e) {
            log.error("Failed to auto-disable module '" + moduleId + "'", e);
        }
    }

    private void disableHandle(ModuleHandleImpl handle, ModuleState resultState) throws ModuleException {
        if (handle.state() != ModuleState.ENABLED) {
            handle.setState(resultState);
            return;
        }
        try {
            handle.moduleInstance().onDisable(handle.context());
        } catch (Throwable t) {
            log.error("Module '" + handle.id() + "' threw from onDisable() - continuing cleanup anyway", t);
        } finally {
            // Unregister events/keybinds/HUD elements/tasks unconditionally: a
            // broken onDisable() must not leave the module's hooks live.
            handle.registrations().cancelAll();
        }
        handle.setState(resultState);
        failureTracker.reset(handle.id());
    }

    private void disposeHandle(ModuleHandleImpl handle) {
        handle.registrations().cancelAll();
        ModuleClassLoader classLoader = handle.classLoader();
        handle.clear();
        handle.setState(ModuleState.UNLOADED);
        if (classLoader != null) {
            classLoader.close();
        }
    }

    private void rollbackFailedInstall(ModuleHandleImpl handle) {
        modules.remove(handle.id());
        disposeHandle(handle);
    }

    private ModuleHandleImpl requireHandle(String moduleId) throws ModuleException {
        ModuleHandleImpl handle = modules.get(moduleId);
        if (handle == null) {
            throw new ModuleException("No such module '" + moduleId + "'");
        }
        return handle;
    }

    private Module instantiateEntrypoint(ModuleManifest manifest, ModuleClassLoader classLoader) throws ModuleException {
        Class<?> entrypointClass;
        try {
            entrypointClass = Class.forName(manifest.entrypoint(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ModuleException(
                    "Entrypoint class '" + manifest.entrypoint() + "' not found in module '" + manifest.id() + "'", e);
        }
        if (!Module.class.isAssignableFrom(entrypointClass)) {
            throw new ModuleException(
                    "Entrypoint '" + manifest.entrypoint() + "' does not implement " + Module.class.getName());
        }
        try {
            return (Module) entrypointClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new ModuleException("Entrypoint '" + manifest.entrypoint() + "' has no no-args constructor", e);
        } catch (InvocationTargetException e) {
            throw new ModuleException("Entrypoint '" + manifest.entrypoint() + "' threw during construction", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new ModuleException("Could not instantiate entrypoint '" + manifest.entrypoint() + "'", e);
        }
    }

    private ModuleContext buildContext(ModuleHandleImpl handle) {
        return new ModuleContextImpl(
                handle.manifest(),
                eventBus.forModule(handle.id(), handle.registrations()),
                keybindRegistry.forModule(handle.id(), handle.registrations()),
                hudRegistry.forModule(handle.id(), handle.registrations()),
                new GameStateImpl(gameStateProvider),
                log
        );
    }

    /** Convenience for tests/introspection: how many modules currently loaded (any state but UNLOADED). */
    List<ModuleHandleImpl> installedHandles() {
        return List.copyOf(modules.values());
    }
}
