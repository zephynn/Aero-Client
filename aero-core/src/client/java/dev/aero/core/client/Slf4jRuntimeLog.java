package dev.aero.core.client;

import dev.aero.core.Aero;
import dev.aero.runtime.RuntimeLog;

/** Routes the module runtime's log output through {@link Aero}'s single logging path. */
public final class Slf4jRuntimeLog implements RuntimeLog {

    @Override
    public void info(String message) {
        Aero.info(message);
    }

    @Override
    public void warn(String message) {
        Aero.warn(message);
    }

    @Override
    public void error(String message, Throwable cause) {
        Aero.error(message, cause);
    }
}
