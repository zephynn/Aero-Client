package dev.aero.runtime.config;

import dev.aero.api.config.ModuleConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Phase 1 {@link ModuleConfig}: in-memory only, reset when the module is unloaded. */
public final class InMemoryModuleConfig implements ModuleConfig {

    private final Map<String, String> values = new ConcurrentHashMap<>();

    @Override
    public String getString(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    @Override
    public void set(String key, String value) {
        values.put(key, value);
    }
}
