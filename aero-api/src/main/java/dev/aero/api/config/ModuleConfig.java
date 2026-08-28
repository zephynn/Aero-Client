package dev.aero.api.config;

/**
 * Minimal per-module key/value config store. Phase 1 keeps this deliberately
 * small (in-memory, no schema, no persistence guarantees) - it exists so the
 * architecture has a config seam a module can code against; a real backing
 * store (e.g. a JSON file per module under the module's own data directory)
 * can be dropped in later without changing this interface.
 */
public interface ModuleConfig {
    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    void set(String key, String value);
}
