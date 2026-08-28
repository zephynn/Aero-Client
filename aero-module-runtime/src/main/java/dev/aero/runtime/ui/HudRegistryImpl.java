package dev.aero.runtime.ui;

import dev.aero.api.event.Subscription;
import dev.aero.api.ui.HudCanvas;
import dev.aero.api.ui.HudElement;
import dev.aero.api.ui.HudRegistry;
import dev.aero.runtime.failure.FailureCallback;
import dev.aero.runtime.registry.ModuleRegistrations;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bookkeeping for HUD elements registered by modules. This class knows
 * nothing about Minecraft rendering - {@code aero-core} calls
 * {@link #renderAll} once per frame with a real {@link HudCanvas}
 * implementation wrapping {@code GuiGraphics}.
 */
public final class HudRegistryImpl {

    public record Entry(String moduleId, HudElement element) {
    }

    @FunctionalInterface
    public interface FailureLogger {
        void log(String moduleId, Throwable failure);
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final FailureCallback failureCallback;
    private final FailureLogger failureLogger;

    public HudRegistryImpl(FailureCallback failureCallback, FailureLogger failureLogger) {
        this.failureCallback = failureCallback;
        this.failureLogger = failureLogger;
    }

    public HudRegistry forModule(String moduleId, ModuleRegistrations registrations) {
        return element -> {
            Entry entry = new Entry(moduleId, element);
            entries.add(entry);
            return registrations.track(() -> entries.remove(entry));
        };
    }

    public void renderAll(HudCanvas canvas) {
        for (Entry entry : entries) {
            try {
                entry.element().render(canvas);
                failureCallback.onSuccess(entry.moduleId());
            } catch (Throwable t) {
                failureLogger.log(entry.moduleId(), t);
                failureCallback.onFailure(entry.moduleId());
            }
        }
    }

    public List<Entry> active() {
        return List.copyOf(entries);
    }
}
