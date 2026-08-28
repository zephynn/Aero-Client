package dev.aero.runtime;

import dev.aero.runtime.pkg.JarModulePackage;
import dev.aero.runtime.testkit.CapturingLog;
import dev.aero.runtime.testkit.Fixtures;
import dev.aero.runtime.testkit.TestModuleBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that disabling a module tears down every registration it made (events, keybinds, HUD elements). */
class DependencyCleanupTest {

    @Test
    void disableUnregistersEverythingTheModuleRegistered(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        File jar = TestModuleBuilder.compileModuleJar(
                tmp, "CleanupModule", Fixtures.wellBehaved("CleanupModule", "v1"),
                Fixtures.manifest("cleanup-module", "CleanupModule", "1.0.0"));

        manager.install(new JarModulePackage(jar));
        manager.enable("cleanup-module");

        ModuleHandleImpl handle = manager.installedHandles().stream()
                .filter(h -> h.id().equals("cleanup-module"))
                .findFirst().orElseThrow();

        // The fixture registers one tick listener, one HUD element, one keybind.
        assertEquals(3, handle.registrations().activeCount());
        assertEquals(1, manager.hudRegistry().active().size());
        assertEquals(1, manager.keybindRegistry().active().size());

        manager.disable("cleanup-module");

        assertEquals(0, handle.registrations().activeCount());
        assertTrue(manager.hudRegistry().active().isEmpty(), "HUD element must be removed on disable");
        assertTrue(manager.keybindRegistry().active().isEmpty(), "keybind must be removed on disable");

        // A render pass after disable must not touch the module's (torn-down) HUD element.
        manager.tick(1);
    }
}
