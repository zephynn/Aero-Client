package dev.aero.runtime.manifest;

import dev.aero.api.ModuleManifest;
import dev.aero.api.exception.ManifestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestParserTest {

    @Test
    void parsesMinimalManifest() throws Exception {
        ModuleManifest manifest = ManifestParser.parse("""
                {
                  "id": "example-module",
                  "name": "Example Module",
                  "version": "1.0.0",
                  "entrypoint": "com.example.ExampleModule",
                  "apiVersion": "1.0"
                }
                """);

        assertEquals("example-module", manifest.id());
        assertEquals("Example Module", manifest.name());
        assertEquals("1.0.0", manifest.version());
        assertEquals("com.example.ExampleModule", manifest.entrypoint());
        assertEquals("1.0", manifest.apiVersion());
        assertTrue(manifest.dependencies().isEmpty());
        assertTrue(manifest.permissions().isEmpty());
        assertEquals("*", manifest.minecraftVersion());
    }

    @Test
    void parsesOptionalFields() throws Exception {
        ModuleManifest manifest = ManifestParser.parse("""
                {
                  "id": "example-module",
                  "name": "Example Module",
                  "version": "1.0.0",
                  "entrypoint": "com.example.ExampleModule",
                  "apiVersion": "1.0",
                  "description": "Does a thing",
                  "author": "Someone",
                  "minecraftVersion": "1.21.11",
                  "dependencies": ["other-module"],
                  "permissions": ["hud"],
                  "updateChannel": "beta"
                }
                """);

        assertEquals("Does a thing", manifest.description());
        assertEquals("Someone", manifest.author());
        assertEquals("1.21.11", manifest.minecraftVersion());
        assertEquals(1, manifest.dependencies().size());
        assertEquals("other-module", manifest.dependencies().get(0));
        assertEquals(1, manifest.permissions().size());
        assertEquals("beta", manifest.updateChannel());
    }

    @Test
    void rejectsMissingRequiredField() {
        ManifestException e = assertThrows(ManifestException.class, () -> ManifestParser.parse("""
                {
                  "id": "example-module",
                  "name": "Example Module",
                  "version": "1.0.0"
                }
                """));
        assertTrue(e.getMessage().contains("entrypoint"));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(ManifestException.class, () -> ManifestParser.parse("{not json"));
    }

    @Test
    void rejectsInvalidModuleId() {
        ManifestException e = assertThrows(ManifestException.class, () -> ManifestParser.parse("""
                {
                  "id": "Not A Valid Id!",
                  "name": "x",
                  "version": "1.0.0",
                  "entrypoint": "com.example.X",
                  "apiVersion": "1.0"
                }
                """));
        assertTrue(e.getMessage().contains("Invalid module id"));
    }
}
