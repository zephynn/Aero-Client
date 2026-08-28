package dev.aero.runtime.ui;

import dev.aero.api.ui.Keybind;
import dev.aero.api.ui.KeybindRegistry;
import dev.aero.runtime.failure.FailureCallback;
import dev.aero.runtime.registry.ModuleRegistrations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bookkeeping for keybinds registered by modules. Like {@link HudRegistryImpl}
 * this knows nothing about Minecraft's real keybinding system - {@code
 * aero-core} adds a {@link Listener} to create or retire the corresponding
 * real {@code KeyMapping} as modules enable/disable at runtime, and calls
 * {@link #firePress} when it detects (via the real binding) that a key was
 * pressed.
 */
public final class KeybindRegistryImpl {

    public record Entry(String qualifiedId, String moduleId, Keybind keybind) {
    }

    public interface Listener {
        void onRegistered(Entry entry);

        void onUnregistered(Entry entry);
    }

    @FunctionalInterface
    public interface FailureLogger {
        void log(String moduleId, String keybindId, Throwable failure);
    }

    private final Map<String, Entry> entriesById = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final FailureCallback failureCallback;
    private final FailureLogger failureLogger;

    public KeybindRegistryImpl(FailureCallback failureCallback, FailureLogger failureLogger) {
        this.failureCallback = failureCallback;
        this.failureLogger = failureLogger;
    }

    /** Called for every already-registered keybind, then for every future registration/removal. */
    public void addListener(Listener listener) {
        listeners.add(listener);
        for (Entry entry : entriesById.values()) {
            listener.onRegistered(entry);
        }
    }

    public KeybindRegistry forModule(String moduleId, ModuleRegistrations registrations) {
        return keybind -> {
            String qualifiedId = moduleId + ":" + keybind.id();
            Entry entry = new Entry(qualifiedId, moduleId, keybind);
            entriesById.put(qualifiedId, entry);
            for (Listener listener : listeners) {
                listener.onRegistered(entry);
            }
            return registrations.track(() -> {
                if (entriesById.remove(qualifiedId, entry)) {
                    for (Listener listener : listeners) {
                        listener.onUnregistered(entry);
                    }
                }
            });
        };
    }

    public void firePress(String qualifiedId) {
        Entry entry = entriesById.get(qualifiedId);
        if (entry == null) {
            return;
        }
        try {
            entry.keybind().onPress().run();
            failureCallback.onSuccess(entry.moduleId());
        } catch (Throwable t) {
            failureLogger.log(entry.moduleId(), entry.keybind().id(), t);
            failureCallback.onFailure(entry.moduleId());
        }
    }

    public Optional<Entry> get(String qualifiedId) {
        return Optional.ofNullable(entriesById.get(qualifiedId));
    }

    public Collection<Entry> active() {
        return entriesById.values();
    }
}
