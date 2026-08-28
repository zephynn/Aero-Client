package dev.aero.runtime;

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

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class ClassloaderIsolationTest {

    @Test
    void eachModuleGetsItsOwnClassloader(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        File jarA = TestModuleBuilder.compileModuleJar(
                tmp, "ModA", Fixtures.wellBehaved("ModA", "v1"), Fixtures.manifest("mod-a", "ModA", "1.0.0"));
        File jarB = TestModuleBuilder.compileModuleJar(
                tmp, "ModB", Fixtures.wellBehaved("ModB", "v1"), Fixtures.manifest("mod-b", "ModB", "1.0.0"));

        manager.install(new JarModulePackage(jarA));
        manager.install(new JarModulePackage(jarB));

        ModuleClassLoader loaderA = handleOf(manager, "mod-a").classLoader();
        ModuleClassLoader loaderB = handleOf(manager, "mod-b").classLoader();

        assertNotSame(loaderA, loaderB, "two modules must not share a classloader");
    }

    @Test
    void uninstallingAModuleLetsItsClassloaderBeCollected(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        File jar = TestModuleBuilder.compileModuleJar(
                tmp, "GcModule", Fixtures.wellBehaved("GcModule", "v1"), Fixtures.manifest("gc-module", "GcModule", "1.0.0"));

        manager.install(new JarModulePackage(jar));
        manager.enable("gc-module");

        WeakReference<ClassLoader> classLoaderRef = new WeakReference<>(handleOf(manager, "gc-module").classLoader());

        manager.uninstall("gc-module");

        assertBecomesUnreachable(classLoaderRef);
    }

    @Test
    void repeatedLoadAndUnloadDoesNotAccumulateClassloaders(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(getClass().getClassLoader(), log, GameStateProvider.NONE);

        WeakReference<ClassLoader> lastRef = null;
        for (int i = 0; i < 5; i++) {
            File jar = TestModuleBuilder.compileModuleJar(
                    tmp, "ReloadModule" + i, Fixtures.wellBehaved("ReloadModule" + i, "v1"),
                    Fixtures.manifest("reload-module", "ReloadModule" + i, "1.0.0"));
            manager.install(new JarModulePackage(jar));
            manager.enable("reload-module");
            manager.tick(i);
            lastRef = new WeakReference<>(handleOf(manager, "reload-module").classLoader());
            manager.uninstall("reload-module");
        }

        assertBecomesUnreachable(lastRef);
    }

    private static ModuleHandleImpl handleOf(ModuleManagerImpl manager, String id) {
        return manager.installedHandles().stream().filter(h -> h.id().equals(id)).findFirst().orElseThrow();
    }

    private static void assertBecomesUnreachable(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            System.gc();
            if (ref.get() == null) {
                return;
            }
            Thread.sleep(50);
        }
        if (ref.get() != null) {
            fail("Classloader was still reachable after repeated GC - likely a leak (static ref, live listener, thread, etc.)");
        }
        assertNull(ref.get());
    }
}
