package dev.aero.api;

import java.util.List;

/**
 * Parsed {@code module.json}. Only {@code id}, {@code name}, {@code version},
 * {@code entrypoint}, and {@code apiVersion} are required and enforced in
 * Phase 1; the remaining fields exist so the manifest schema doesn't need to
 * change shape when permissions, dependency resolution, signing, etc. are
 * built in later phases - they parse today (defaulting to empty) but are not
 * yet acted on by the runtime.
 *
 * <pre>{@code
 * {
 *   "id": "example-module",
 *   "name": "Example Module",
 *   "version": "1.0.0",
 *   "entrypoint": "com.example.ExampleModule",
 *   "apiVersion": "1.0"
 * }
 * }</pre>
 */
public record ModuleManifest(
        String id,
        String name,
        String version,
        String entrypoint,
        String apiVersion,
        String description,
        String author,
        String icon,
        String minecraftVersion,
        List<String> dependencies,
        List<String> permissions,
        String updateChannel
) {
    public ModuleManifest {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
