package dev.aero.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Fixed-size ring buffer of recent Aero log lines, fed by {@link Aero}'s
 * logging helpers and read by the config screen's log panel. Deliberately
 * separate from SLF4J/the game's own log file - this is just enough state
 * for a super basic in-game log view, not a logging framework.
 */
public final class LogBuffer {

    public static final LogBuffer INSTANCE = new LogBuffer();

    private static final int CAPACITY = 200;

    private final Deque<String> lines = new ArrayDeque<>();

    private LogBuffer() {
    }

    public synchronized void add(String line) {
        lines.addLast(line);
        while (lines.size() > CAPACITY) {
            lines.removeFirst();
        }
    }

    /** The most recent {@code count} lines, oldest first. */
    public synchronized List<String> recent(int count) {
        List<String> all = new ArrayList<>(lines);
        int from = Math.max(0, all.size() - count);
        return Collections.unmodifiableList(all.subList(from, all.size()));
    }
}
