package dev.aero.runtime.registry;

import dev.aero.api.event.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Every registration (event listener, keybind, HUD element) a single module
 * makes flows through one of these. It is the mechanism behind "disabling a
 * module unregisters everything it registered" - {@link #cancelAll()} is
 * called unconditionally when the module is disabled, so cleanup happens
 * even if the module's {@code onDisable} does nothing.
 */
public final class ModuleRegistrations {

    private final List<Subscription> owned = new ArrayList<>();

    /**
     * Wraps {@code cancelAction} (which performs the real un-registration,
     * e.g. removing a listener from a map) as a {@link Subscription} that is
     * also tracked here for bulk cleanup.
     */
    public synchronized Subscription track(Runnable cancelAction) {
        Subscription subscription = new TrackingSubscription(cancelAction);
        owned.add(subscription);
        return subscription;
    }

    public synchronized void cancelAll() {
        List<Subscription> copy = new ArrayList<>(owned);
        owned.clear();
        for (Subscription subscription : copy) {
            subscription.cancel();
        }
    }

    public synchronized int activeCount() {
        return owned.size();
    }
}
