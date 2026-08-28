package dev.aero.runtime.event;

import dev.aero.api.event.AeroEvent;
import dev.aero.api.event.EventBus;
import dev.aero.api.event.Subscription;
import dev.aero.runtime.failure.FailureCallback;
import dev.aero.runtime.registry.ModuleRegistrations;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The shared, global event bus. Modules never see this class directly - each
 * gets a scoped {@link EventBus} view via {@link #forModule} whose
 * subscriptions are tagged with the module's id (for failure attribution)
 * and tracked in that module's {@link ModuleRegistrations} (for cleanup).
 *
 * <p>{@link #dispatch} is the try/catch boundary between a module callback
 * and Aero/Minecraft's own tick or render loop: a listener that throws is
 * caught, logged, and reported to the {@link FailureCallback} - it never
 * propagates, and never stops other listeners from running.
 */
public final class EventBusImpl {

    @FunctionalInterface
    public interface FailureLogger {
        void log(String moduleId, String eventType, Throwable failure);
    }

    private record ListenerEntry(String moduleId, Consumer<AeroEvent> listener) {
    }

    private final Map<Class<?>, List<ListenerEntry>> listenersByType = new ConcurrentHashMap<>();
    private final FailureCallback failureCallback;
    private final FailureLogger failureLogger;

    public EventBusImpl(FailureCallback failureCallback, FailureLogger failureLogger) {
        this.failureCallback = failureCallback;
        this.failureLogger = failureLogger;
    }

    public EventBus forModule(String moduleId, ModuleRegistrations registrations) {
        return new EventBus() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends AeroEvent> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
                ListenerEntry entry = new ListenerEntry(moduleId, (Consumer<AeroEvent>) listener);
                List<ListenerEntry> list =
                        listenersByType.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>());
                list.add(entry);
                return registrations.track(() -> list.remove(entry));
            }
        };
    }

    public <T extends AeroEvent> void dispatch(T event) {
        List<ListenerEntry> entries = listenersByType.get(event.getClass());
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (ListenerEntry entry : entries) {
            try {
                entry.listener().accept(event);
                failureCallback.onSuccess(entry.moduleId());
            } catch (Throwable t) {
                failureLogger.log(entry.moduleId(), event.getClass().getSimpleName(), t);
                failureCallback.onFailure(entry.moduleId());
            }
        }
    }
}
