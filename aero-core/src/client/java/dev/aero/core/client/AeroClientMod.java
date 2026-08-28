package dev.aero.core.client;

import dev.aero.api.ModuleManifest;
import dev.aero.api.exception.ModuleException;
import dev.aero.core.Aero;
import dev.aero.runtime.ModuleRuntime;
import dev.aero.runtime.pkg.JarModulePackage;
import dev.aero.runtime.pkg.ModuleDiscovery;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The client half of Aero: this is where the Aero Module Runtime (which
 * knows nothing about Minecraft) is wired up to real Minecraft/Fabric hooks.
 * Community Modules only ever see the runtime side, never this class.
 */
@Environment(EnvType.CLIENT)
public class AeroClientMod implements ClientModInitializer {

    private static final String MODULES_SUBDIR = "aero/modules";

    /** How often (in client ticks) the modules directory is rescanned for newly-dropped jars. 20 ticks/second, so 100 = ~5 seconds. */
    private static final long AUTO_SCAN_INTERVAL_TICKS = 100;

    @Override
    public void onInitializeClient() {
        Path modulesDir = FabricLoader.getInstance().getGameDir().resolve(MODULES_SUBDIR);
        ensureModulesDirExists(modulesDir);
        Aero.modulesDir = modulesDir;

        ModuleRuntime runtime = new ModuleRuntime(
                AeroClientMod.class.getClassLoader(), new Slf4jRuntimeLog(), new GameStateProviderImpl());
        Aero.runtime = runtime;

        KeybindBridge keybindBridge = new KeybindBridge(runtime.keybinds());

        loadModulesFrom(runtime, modulesDir);

        AtomicLong ticksSinceLastScan = new AtomicLong();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            runtime.tick();
            keybindBridge.poll();

            // Auto-detect new module jars dropped into the modules directory
            // while Minecraft is running - no restart, no command needed.
            // loadModulesFrom() already skips ids that are already installed,
            // so this is safe to call repeatedly.
            if (ticksSinceLastScan.incrementAndGet() >= AUTO_SCAN_INTERVAL_TICKS) {
                ticksSinceLastScan.set(0);
                loadModulesFrom(runtime, modulesDir);
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            runtime.render(new GuiGraphicsHudCanvas(guiGraphics, net.minecraft.client.Minecraft.getInstance()), partialTick);
        });

        AeroCommands.register(runtime, modulesDir);

        Aero.info("Aero client runtime ready - modules directory: {}", modulesDir);
    }

    private static void ensureModulesDirExists(Path modulesDir) {
        try {
            Files.createDirectories(modulesDir);
        } catch (IOException e) {
            Aero.error("Could not create Aero modules directory at " + modulesDir, e);
        }
    }

    /**
     * Installs and enables every {@code .jar} in the modules directory whose
     * manifest id isn't already installed - safe to call repeatedly (at
     * startup, and every {@link #AUTO_SCAN_INTERVAL_TICKS} ticks thereafter),
     * since already-installed modules are skipped. Removing, disabling, or
     * updating a module in place while running still goes through
     * {@code /aero disable|enable|update|uninstall} - see {@link AeroCommands}.
     */
    private static void loadModulesFrom(ModuleRuntime runtime, Path modulesDir) {
        try {
            for (JarModulePackage pkg : ModuleDiscovery.scan(modulesDir)) {
                try {
                    ModuleManifest manifest = pkg.manifest();
                    if (runtime.modules().getModule(manifest.id()).isPresent()) {
                        // Already installed (from a previous scan, or this is a
                        // second/updated jar for the same id sitting in the
                        // folder) - use `/aero update` to replace it in place.
                        continue;
                    }
                    runtime.modules().install(pkg);
                    runtime.modules().enable(manifest.id());
                    Aero.info("Loaded and enabled module '{}' v{}", manifest.id(), manifest.version());
                } catch (ModuleException e) {
                    // One broken module must never stop the others from loading.
                    Aero.error("Failed to load module from " + pkg.jarFile().getName(), e);
                }
            }
        } catch (IOException e) {
            Aero.error("Could not scan Aero modules directory at " + modulesDir, e);
        }
    }
}
