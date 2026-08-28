package dev.aero.core;

import dev.aero.runtime.ModuleRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Shared access point for the rest of aero-core. {@link #runtime} and
 * {@link #modulesDir} are set once by {@code AeroClientMod} during client
 * init - Phase 1 has no server-side module support, so they stay null on a
 * dedicated server.
 *
 * <p>{@link #info}/{@link #warn}/{@link #error} are the one logging path
 * everything in Aero should use: they write to SLF4J (the normal game log)
 * and to {@link LogBuffer} (read by the config screen's log panel) in one
 * call, so nothing logged through them is invisible to the in-game UI.
 */
public final class Aero {

    public static final String MOD_ID = "aero";
    public static final Logger LOGGER = LoggerFactory.getLogger("Aero");

    public static volatile ModuleRuntime runtime;
    public static volatile Path modulesDir;

    private Aero() {
    }

    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
        LogBuffer.INSTANCE.add("[INFO] " + formatted(message, args));
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
        LogBuffer.INSTANCE.add("[WARN] " + formatted(message, args));
    }

    public static void error(String message, Throwable cause) {
        LOGGER.error(message, cause);
        LogBuffer.INSTANCE.add("[ERROR] " + message + (cause == null ? "" : ": " + cause));
    }

    private static String formatted(String message, Object... args) {
        // SLF4J's own "{}" placeholder substitution, kept simple since this
        // is only used for the plain-text log panel, not structured logging.
        String result = message;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", java.util.regex.Matcher.quoteReplacement(String.valueOf(arg)));
        }
        return result;
    }
}
