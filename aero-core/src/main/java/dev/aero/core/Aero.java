package dev.aero.core;

import dev.aero.runtime.ModuleRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared access point for the rest of aero-core. {@link #runtime} is set
 * once by {@code AeroClientMod} during client init - Phase 1 has no
 * server-side module support, so it is null on a dedicated server.
 */
public final class Aero {

    public static final String MOD_ID = "aero";
    public static final Logger LOGGER = LoggerFactory.getLogger("Aero");

    public static volatile ModuleRuntime runtime;

    private Aero() {
    }
}
