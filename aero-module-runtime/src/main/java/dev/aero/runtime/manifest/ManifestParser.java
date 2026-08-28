package dev.aero.runtime.manifest;

import dev.aero.api.ModuleManifest;
import dev.aero.api.exception.ManifestException;
import dev.aero.runtime.json.Json;
import dev.aero.runtime.json.JsonParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parses and validates {@code module.json} into a {@link ModuleManifest}. */
public final class ManifestParser {

    private ManifestParser() {
    }

    public static ModuleManifest parse(String json) throws ManifestException {
        Map<String, Object> root;
        try {
            root = Json.parseObject(json);
        } catch (JsonParseException e) {
            throw new ManifestException("module.json is not valid JSON: " + e.getMessage(), e);
        }

        String id = requireString(root, "id");
        String name = requireString(root, "name");
        String version = requireString(root, "version");
        String entrypoint = requireString(root, "entrypoint");
        String apiVersion = requireString(root, "apiVersion");

        if (!id.matches("[a-z0-9][a-z0-9-]*")) {
            throw new ManifestException(
                    "Invalid module id '" + id + "': must be lowercase alphanumeric with hyphens, e.g. 'example-module'");
        }

        return new ModuleManifest(
                id,
                name,
                version,
                entrypoint,
                apiVersion,
                optString(root, "description", ""),
                optString(root, "author", ""),
                optString(root, "icon", ""),
                optString(root, "minecraftVersion", "*"),
                optStringList(root, "dependencies"),
                optStringList(root, "permissions"),
                optString(root, "updateChannel", "stable")
        );
    }

    private static String requireString(Map<String, Object> root, String key) throws ManifestException {
        Object value = root.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new ManifestException("module.json is missing required field '" + key + "'");
        }
        return s;
    }

    private static String optString(Map<String, Object> root, String key, String fallback) {
        Object value = root.get(key);
        return value instanceof String s ? s : fallback;
    }

    @SuppressWarnings("unchecked")
    private static List<String> optStringList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String s) {
                result.add(s);
            }
        }
        return result;
    }
}
