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

/**
 * The client half of Aero: this is where the Aero Module Runtime (which
 * knows nothing about Minecraft) is wired up to real Minecraft/Fabric hooks.
 * Community Modules only ever see the runtime side, never this class.
 */
@Environment(EnvType.CLIENT)
public class AeroClientMod implements ClientModInitializer {

    private static final String MODULES_SUBDIR = "aero/modules";

    @Override
    public void onInitializeClient() {
        Path modulesDir = FabricLoader.getInstance().getGameDir().resolve(MODULES_SUBDIR);
        ensureModulesDirExists(modulesDir);

        ModuleRuntime runtime = new ModuleRuntime(
                AeroClientMod.class.getClassLoader(), new Slf4jRuntimeLog(), new GameStateProviderImpl());
        Aero.runtime = runtime;

        KeybindBridge keybindBridge = new KeybindBridge(runtime.keybinds());

        loadModulesFrom(runtime, modulesDir);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            runtime.tick();
            keybindBridge.poll();
        });

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            runtime.render(new GuiGraphicsHudCanvas(guiGraphics, net.minecraft.client.Minecraft.getInstance()), partialTick);
        });

        AeroCommands.register(runtime, modulesDir);

        Aero.LOGGER.info("Aero client runtime ready - modules directory: {}", modulesDir);
    }

    private static void ensureModulesDirExists(Path modulesDir) {
        try {
            Files.createDirectories(modulesDir);
        } catch (IOException e) {
            Aero.LOGGER.error("Could not create Aero modules directory at " + modulesDir, e);
        }
    }

    /**
     * Startup discovery: every {@code .jar} already sitting in the modules
     * directory is installed and enabled immediately. A module dropped in
     * (or removed/updated) while Minecraft keeps running is handled through
     * {@code /aero reload|enable|disable|update|uninstall} - see
     * {@link AeroCommands}.
     */
    private static void loadModulesFrom(ModuleRuntime runtime, Path modulesDir) {
        try {
            for (JarModulePackage pkg : ModuleDiscovery.scan(modulesDir)) {
                try {
                    ModuleManifest manifest = pkg.manifest();
                    runtime.modules().install(pkg);
                    runtime.modules().enable(manifest.id());
                    Aero.LOGGER.info("Loaded and enabled module '{}' v{}", manifest.id(), manifest.version());
                } catch (ModuleException e) {
                    // One broken module must never stop the others from loading.
                    Aero.LOGGER.error("Failed to load module from " + pkg.jarFile().getName(), e);
                }
            }
        } catch (IOException e) {
            Aero.LOGGER.error("Could not scan Aero modules directory at " + modulesDir, e);
        }
    }
}
