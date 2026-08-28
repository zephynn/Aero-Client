package dev.aero.runtime.registry;

import dev.aero.api.event.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;

/** A {@link Subscription} that runs its cancel action at most once. */
final class TrackingSubscription implements Subscription {

    private final Runnable cancelAction;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    TrackingSubscription(Runnable cancelAction) {
        this.cancelAction = cancelAction;
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            cancelAction.run();
        }
    }
}
