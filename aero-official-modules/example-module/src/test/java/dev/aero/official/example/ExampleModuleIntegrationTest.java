package dev.aero.official.example;

import dev.aero.api.ModuleState;
import dev.aero.api.ui.HudCanvas;
import dev.aero.runtime.GameStateProvider;
import dev.aero.runtime.ModuleRuntime;
import dev.aero.runtime.RuntimeLog;
import dev.aero.runtime.pkg.JarModulePackage;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the *real, independently-built* {@code example-module-1.0.0.jar}
 * and {@code example-module-2.0.0.jar} (see {@code v1Jar}/{@code v2Jar} in
 * this module's build.gradle.kts) through the *real* Aero Module Runtime -
 * no fixtures, no mocks. This is the closest thing to the in-game
 * demonstration script that can run headlessly: install, HUD renders,
 * keybind toggles it off, update to v2 while "running", uninstall.
 */
class ExampleModuleIntegrationTest {

    private static final String MODULE_ID = "example-module";

    private static File jarPath(String systemProperty) {
        String path = System.getProperty(systemProperty);
        if (path == null) {
            throw new IllegalStateException(
                    systemProperty + " was not set - run this test via Gradle (`./gradlew :aero-official-modules:example-module:test`), "
                            + "which wires it up to the v1Jar/v2Jar outputs");
        }
        return new File(path);
    }

    @Test
    void installEnableHudTogglePressUpdateUninstall() throws Exception {
        ModuleRuntime runtime = new ModuleRuntime(getClass().getClassLoader(), RuntimeLog.systemOut(), GameStateProvider.NONE);
        RecordingCanvas canvas = new RecordingCanvas();

        // install + enable v1 -> HUD appears
        runtime.modules().install(new JarModulePackage(jarPath("aero.test.v1Jar")));
        runtime.modules().enable(MODULE_ID);
        assertEquals(ModuleState.ENABLED, runtime.modules().getModule(MODULE_ID).orElseThrow().state());
        assertEquals("1.0.0", runtime.modules().getModule(MODULE_ID).orElseThrow().manifest().version());

        runtime.tick();
        canvas.reset();
        runtime.render(canvas, 0f);
        assertTrue(canvas.anyContains("Aero Module Loaded!"), "v1 must draw its HUD text: " + canvas.drawn);

        // press its keybind -> HUD disappears (module toggled itself off, not disabled)
        runtime.keybinds().firePress(MODULE_ID + ":toggle");
        canvas.reset();
        runtime.render(canvas, 0f);
        assertTrue(canvas.drawn.isEmpty(), "HUD must be hidden after the toggle keybind fires: " + canvas.drawn);

        // disable -> HUD stays gone (element unregistered entirely)
        runtime.modules().disable(MODULE_ID);
        canvas.reset();
        runtime.render(canvas, 0f);
        assertTrue(canvas.drawn.isEmpty());

        // re-enable, then hot-update to v2 while "Minecraft keeps running" (no restart happens here)
        runtime.modules().enable(MODULE_ID);
        runtime.modules().update(MODULE_ID, new JarModulePackage(jarPath("aero.test.v2Jar")));

        assertEquals(ModuleState.ENABLED, runtime.modules().getModule(MODULE_ID).orElseThrow().state(),
                "the module was enabled before update(), so v2 must come up enabled too");
        assertEquals("2.0.0", runtime.modules().getModule(MODULE_ID).orElseThrow().manifest().version());

        runtime.tick();
        canvas.reset();
        runtime.render(canvas, 0f);
        assertTrue(canvas.anyContains("v2, hot-updated"), "v2 must now be the one rendering: " + canvas.drawn);

        // uninstall -> gone entirely, "Minecraft" (this JVM) keeps running regardless
        runtime.modules().uninstall(MODULE_ID);
        assertTrue(runtime.modules().getModule(MODULE_ID).isEmpty());
        canvas.reset();
        runtime.render(canvas, 0f);
        assertTrue(canvas.drawn.isEmpty());
    }

    private static final class RecordingCanvas implements HudCanvas {
        final List<String> drawn = new ArrayList<>();

        void reset() {
            drawn.clear();
        }

        boolean anyContains(String substring) {
            return drawn.stream().anyMatch(s -> s.contains(substring));
        }

        @Override
        public void drawText(String text, int x, int y, int argbColor) {
            drawn.add(text);
        }

        @Override
        public int screenWidth() {
            return 1920;
        }

        @Override
        public int screenHeight() {
            return 1080;
        }
    }
}
