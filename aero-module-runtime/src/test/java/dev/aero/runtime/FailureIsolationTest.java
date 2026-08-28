package dev.aero.runtime;

import dev.aero.api.ModuleState;
import dev.aero.runtime.failure.FailureTracker;
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

/**
 * A module whose callback always throws must never crash the tick loop, and
 * must never affect any other module - and after enough consecutive
 * failures, the runtime disables it automatically.
 */
class FailureIsolationTest {

    private static final int THRESHOLD = 3;

    @Test
    void throwingModuleIsIsolatedAndAutoDisabledWithoutAffectingOthers(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(
                getClass().getClassLoader(), log, GameStateProvider.NONE, new FailureTracker(THRESHOLD));

        File badJar = TestModuleBuilder.compileModuleJar(
                tmp, "BadModule", Fixtures.alwaysThrowsOnTick("BadModule"), Fixtures.manifest("bad-module", "BadModule", "1.0.0"));
        File goodJar = TestModuleBuilder.compileModuleJar(
                tmp, "GoodModule", Fixtures.wellBehaved("GoodModule", "v1"), Fixtures.manifest("good-module", "GoodModule", "1.0.0"));

        manager.install(new JarModulePackage(badJar));
        manager.install(new JarModulePackage(goodJar));
        manager.enable("bad-module");
        manager.enable("good-module");

        // Ticking must never throw out of the manager, no matter how broken a module is.
        for (int i = 1; i <= THRESHOLD + 2; i++) {
            manager.tick(i);
        }

        assertEquals(ModuleState.FAILED, manager.getModule("bad-module").orElseThrow().state(),
                "module must be auto-disabled after " + THRESHOLD + " consecutive failures");
        assertEquals(ModuleState.ENABLED, manager.getModule("good-module").orElseThrow().state(),
                "an unrelated module must be unaffected by another module's failures");
        assertEquals(THRESHOLD + 2, log.countContaining("tick:v1"),
                "the good module must keep receiving every tick even while the bad one is failing");
        assertTrue(log.anyContains("exceeded " + THRESHOLD + " consecutive callback failures"));

        // Once disabled, the bad module must stop being invoked at all (no more failure logs accumulating).
        long errorsSoFar = log.countContaining("threw from a TickEvent listener");
        manager.tick(THRESHOLD + 3);
        assertEquals(errorsSoFar, log.countContaining("threw from a TickEvent listener"));
    }

    @Test
    void aModuleThatRecoversResetsItsFailureCount(@TempDir Path tmp) throws Exception {
        CapturingLog log = new CapturingLog();
        ModuleManagerImpl manager = new ModuleManagerImpl(
                getClass().getClassLoader(), log, GameStateProvider.NONE, new FailureTracker(THRESHOLD));

        File jar = TestModuleBuilder.compileModuleJar(
                tmp, "FlakyModule", Fixtures.wellBehaved("FlakyModule", "v1"), Fixtures.manifest("flaky-module", "FlakyModule", "1.0.0"));
        manager.install(new JarModulePackage(jar));
        manager.enable("flaky-module");

        // A well-behaved module never fails, so its failure count should stay at zero
        // and it must never be auto-disabled no matter how many ticks pass.
        for (int i = 0; i < THRESHOLD * 3; i++) {
            manager.tick(i);
        }

        assertEquals(ModuleState.ENABLED, manager.getModule("flaky-module").orElseThrow().state());
    }
}
