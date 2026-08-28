package dev.aero.runtime;

import dev.aero.api.ModuleState;
import dev.aero.runtime.classloader.ModuleClassLoader;
import dev.aero.runtime.pkg.JarModulePackage;
import dev.aero.runtime.testkit.CapturingLog;
import dev.aero.runtime.testkit.Fixtures;
import dev.aero.runtime.testkit.TestModuleBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** update() must swap v1 for v2 without the caller ever restarting anything - "no Minecraft restart". */
class UpdateTest {

    @Test
    void updateReplacesTheRunningInstanceWithoutRestartingAnything(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        // "ExampleModuleV1" / "ExampleModuleV2" mirror what aero-official-modules ships,
        // but compiled here so the test doesn't depend on that module's build output.
        File jarV1 = TestModuleBuilder.compileModuleJar(
                tmp, "UpdModule", Fixtures.wellBehaved("UpdModule", "v1"), Fixtures.manifest("upd-module", "UpdModule", "1.0.0"));

        manager.install(new JarModulePackage(jarV1));
        manager.enable("upd-module");
        manager.tick(1);
        assertEquals(1, log.countContaining("tick:v1"));

        WeakReference<ModuleClassLoader> v1LoaderRef =
                new WeakReference<>(manager.installedHandles().stream()
                        .filter(h -> h.id().equals("upd-module")).findFirst().orElseThrow().classLoader());

        File jarV2 = TestModuleBuilder.compileModuleJar(
                tmp, "UpdModuleV2", Fixtures.wellBehaved("UpdModuleV2", "v2"), Fixtures.manifest("upd-module", "UpdModuleV2", "2.0.0"));

        manager.update("upd-module", new JarModulePackage(jarV2));

        assertEquals(ModuleState.ENABLED, manager.getModule("upd-module").orElseThrow().state(),
                "module was enabled before update, so it must be enabled again after");
        assertEquals("2.0.0", manager.getModule("upd-module").orElseThrow().manifest().version());
        assertTrue(log.anyContains("onDisable:v1"));
        assertTrue(log.anyContains("onLoad:v2"));
        assertTrue(log.anyContains("onEnable:v2"));

        manager.tick(2);
        assertEquals(1, log.countContaining("tick:v1"), "v1's tick listener must be gone after the update");
        assertEquals(1, log.countContaining("tick:v2"), "v2 should now be the one receiving ticks");

        for (int i = 0; i < 20; i++) {
            System.gc();
            if (v1LoaderRef.get() == null) {
                break;
            }
            Thread.sleep(50);
        }
        if (v1LoaderRef.get() != null) {
            fail("v1's classloader is still reachable after update() - the old module was not fully released");
        }
        assertNull(v1LoaderRef.get());
    }

    @Test
    void updatingAModuleThatWasNeverEnabledDoesNotAutoEnableIt(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        File jarV1 = TestModuleBuilder.compileModuleJar(
                tmp, "IdleModule", Fixtures.wellBehaved("IdleModule", "v1"), Fixtures.manifest("idle-module", "IdleModule", "1.0.0"));
        manager.install(new JarModulePackage(jarV1));
        // never enabled

        File jarV2 = TestModuleBuilder.compileModuleJar(
                tmp, "IdleModuleV2", Fixtures.wellBehaved("IdleModuleV2", "v2"), Fixtures.manifest("idle-module", "IdleModuleV2", "2.0.0"));
        manager.update("idle-module", new JarModulePackage(jarV2));

        assertEquals(ModuleState.LOADED, manager.getModule("idle-module").orElseThrow().state());
    }
}
