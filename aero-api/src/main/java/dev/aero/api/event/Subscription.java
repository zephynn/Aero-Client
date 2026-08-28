package dev.aero.api.event;

/**
 * A handle to a listener registered via {@link EventBus#subscribe}. Modules
 * rarely need to call {@link #cancel()} themselves - the runtime cancels
 * every subscription a module owns when it is disabled - but it's available
 * for a module that wants to stop listening early.
 */
public interface Subscription {
    void cancel();
}
