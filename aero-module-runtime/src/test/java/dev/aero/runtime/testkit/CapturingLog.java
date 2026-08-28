package dev.aero.runtime.testkit;

import dev.aero.runtime.RuntimeLog;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Captures every log line so tests can assert on the module lifecycle trail. */
public final class CapturingLog implements RuntimeLog {

    private final List<String> lines = new CopyOnWriteArrayList<>();

    @Override
    public void info(String message) {
        lines.add(message);
    }

    @Override
    public void warn(String message) {
        lines.add("WARN:" + message);
    }

    @Override
    public void error(String message, Throwable cause) {
        lines.add("ERROR:" + message);
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean anyContains(String substring) {
        return lines.stream().anyMatch(l -> l.contains(substring));
    }

    public long countContaining(String substring) {
        return lines.stream().filter(l -> l.contains(substring)).count();
    }
}
