package dev.aero.core.client;

import dev.aero.core.Aero;
import dev.aero.runtime.RuntimeLog;

/** Routes the module runtime's log output through Aero's own SLF4J logger. */
public final class Slf4jRuntimeLog implements RuntimeLog {

    @Override
    public void info(String message) {
        Aero.LOGGER.info(message);
    }

    @Override
    public void warn(String message) {
        Aero.LOGGER.warn(message);
    }

    @Override
    public void error(String message, Throwable cause) {
        Aero.LOGGER.error(message, cause);
    }
}
