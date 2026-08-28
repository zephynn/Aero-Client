package dev.aero.runtime;

import dev.aero.api.InstalledModule;
import dev.aero.api.ModuleState;
import dev.aero.api.exception.ModuleException;
import dev.aero.runtime.pkg.JarModulePackage;
import dev.aero.runtime.testkit.CapturingLog;
import dev.aero.runtime.testkit.Fixtures;
import dev.aero.runtime.testkit.TestModuleBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleLifecycleTest {

    @Test
    void fullLifecycleGoesThroughExpectedStates(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        File jar = TestModuleBuilder.compileModuleJar(
                tmp, "LifecycleModule", Fixtures.wellBehaved("LifecycleModule", "v1"),
                Fixtures.manifest("lifecycle-module", "LifecycleModule", "1.0.0"));

        InstalledModule installed = manager.install(new JarModulePackage(jar));
        assertEquals(ModuleState.LOADED, installed.state());
        assertTrue(log.anyContains("onLoad:v1"));

        manager.enable("lifecycle-module");
        assertEquals(ModuleState.ENABLED, manager.getModule("lifecycle-module").orElseThrow().state());
        assertTrue(log.anyContains("onEnable:v1"));

        manager.tick(1);
        manager.tick(2);
        assertEquals(2, log.countContaining("tick:v1"));

        manager.disable("lifecycle-module");
        assertEquals(ModuleState.DISABLED, manager.getModule("lifecycle-module").orElseThrow().state());
        assertTrue(log.anyContains("onDisable:v1"));

        // A tick after disable must not reach the module anymore.
        manager.tick(3);
        assertEquals(2, log.countContaining("tick:v1"));

        manager.uninstall("lifecycle-module");
        assertTrue(manager.getModule("lifecycle-module").isEmpty());
    }

    @Test
    void enableIsRejectedForUnknownModule() {
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), new CapturingLog(), GameStateProvider.NONE);
        assertThrows(ModuleException.class, () -> manager.enable("does-not-exist"));
    }

    @Test
    void installingTheSameIdTwiceIsRejected(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        File jar = TestModuleBuilder.compileModuleJar(
                tmp, "DupModule", Fixtures.wellBehaved("DupModule", "v1"),
                Fixtures.manifest("dup-module", "DupModule", "1.0.0"));

        manager.install(new JarModulePackage(jar));
        assertThrows(ModuleException.class, () -> manager.install(new JarModulePackage(jar)));
    }

    @Test
    void entrypointNotImplementingModuleIsRejected(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        File jar = TestModuleBuilder.compileModuleJar(
                tmp, "NotAModule", Fixtures.notAModule("NotAModule"),
                Fixtures.manifest("not-a-module", "NotAModule", "1.0.0"));

        assertThrows(ModuleException.class, () -> manager.install(new JarModulePackage(jar)));
        assertTrue(manager.getModule("not-a-module").isEmpty(), "a failed install must not leave a dangling handle");
    }
}
