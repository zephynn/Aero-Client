package dev.aero.runtime.failure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks consecutive callback failures per module. A module's counter resets
 * to zero on any successful callback; once it reaches {@link #threshold}
 * consecutive failures, {@link #recordFailure} returns {@code true} once and
 * the caller (see {@code ModuleManagerImpl}) auto-disables the module.
 */
public final class FailureTracker {

    public static final int DEFAULT_THRESHOLD = 5;

    private final Map<String, AtomicInteger> consecutiveFailures = new ConcurrentHashMap<>();
    private final int threshold;

    public FailureTracker() {
        this(DEFAULT_THRESHOLD);
    }

    public FailureTracker(int threshold) {
        this.threshold = threshold;
    }

    public void recordSuccess(String moduleId) {
        AtomicInteger counter = consecutiveFailures.get(moduleId);
        if (counter != null) {
            counter.set(0);
        }
    }

    /** @return true the moment the module's consecutive-failure count reaches the threshold. */
    public boolean recordFailure(String moduleId) {
        int count = consecutiveFailures.computeIfAbsent(moduleId, id -> new AtomicInteger()).incrementAndGet();
        return count == threshold;
    }

    public void reset(String moduleId) {
        consecutiveFailures.remove(moduleId);
    }

    public int currentCount(String moduleId) {
        AtomicInteger counter = consecutiveFailures.get(moduleId);
        return counter == null ? 0 : counter.get();
    }

    public int threshold() {
        return threshold;
    }
}
