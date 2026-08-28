package dev.aero.core.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.aero.api.InstalledModule;
import dev.aero.api.ModuleManifest;
import dev.aero.api.exception.ManifestException;
import dev.aero.api.exception.ModuleException;
import dev.aero.runtime.ModuleRuntime;
import dev.aero.runtime.pkg.JarModulePackage;
import dev.aero.runtime.pkg.ModuleDiscovery;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * The {@code /aero} debug command. {@code /aero gui} opens
 * {@link AeroConfigScreen} directly - the same screen Mod Menu opens - so it
 * is reachable even without Mod Menu installed; the rest exercise the
 * lifecycle from chat for scripting/testing.
 */
public final class AeroCommands {

    private AeroCommands() {
    }

    public static void register(ModuleRuntime runtime, Path modulesDir) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("aero")
                        .then(literal("gui").executes(ctx -> openGui(ctx.getSource())))
                        .then(literal("list").executes(ctx -> list(ctx.getSource(), runtime)))
                        .then(literal("reload").executes(ctx -> reload(ctx.getSource(), runtime, modulesDir)))
                        .then(literal("enable").then(argument("id", StringArgumentType.word())
                                .executes(ctx -> enable(ctx.getSource(), runtime, StringArgumentType.getString(ctx, "id")))))
                        .then(literal("disable").then(argument("id", StringArgumentType.word())
                                .executes(ctx -> disable(ctx.getSource(), runtime, StringArgumentType.getString(ctx, "id")))))
                        .then(literal("uninstall").then(argument("id", StringArgumentType.word())
                                .executes(ctx -> uninstall(ctx.getSource(), runtime, StringArgumentType.getString(ctx, "id")))))
                        .then(literal("update").then(argument("id", StringArgumentType.word())
                                .then(argument("file", StringArgumentType.string())
                                        .executes(ctx -> update(ctx.getSource(), runtime, modulesDir,
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "file"))))))));
    }

    private static int openGui(FabricClientCommandSource source) {
        source.getClient().setScreen(new AeroConfigScreen(source.getClient().screen));
        return 1;
    }

    private static int list(FabricClientCommandSource source, ModuleRuntime runtime) {
        Collection<InstalledModule> modules = runtime.modules().getModules();
        if (modules.isEmpty()) {
            source.sendFeedback(Component.literal("Aero: no modules installed"));
            return 1;
        }
        for (InstalledModule module : modules) {
            source.sendFeedback(Component.literal(
                    " - " + module.id() + " v" + module.manifest().version() + " [" + module.state() + "]"));
        }
        return 1;
    }

    private static int reload(FabricClientCommandSource source, ModuleRuntime runtime, Path modulesDir) {
        try {
            for (JarModulePackage pkg : ModuleDiscovery.scan(modulesDir)) {
                ModuleManifest manifest;
                try {
                    manifest = pkg.manifest();
                } catch (ManifestException e) {
                    source.sendError(Component.literal("Aero: skipping " + pkg.jarFile().getName() + ": " + e.getMessage()));
                    continue;
                }
                if (runtime.modules().getModule(manifest.id()).isPresent()) {
                    continue;
                }
                try {
                    runtime.modules().install(pkg);
                    runtime.modules().enable(manifest.id());
                    source.sendFeedback(Component.literal("Aero: loaded and enabled '" + manifest.id() + "'"));
                } catch (ModuleException e) {
                    source.sendError(Component.literal("Aero: failed to load '" + manifest.id() + "': " + e.getMessage()));
                }
            }
            return 1;
        } catch (IOException e) {
            source.sendError(Component.literal("Aero: could not scan modules directory: " + e.getMessage()));
            return 0;
        }
    }

    private static int enable(FabricClientCommandSource source, ModuleRuntime runtime, String id) {
        try {
            runtime.modules().enable(id);
            source.sendFeedback(Component.literal("Aero: enabled '" + id + "'"));
            return 1;
        } catch (ModuleException e) {
            source.sendError(Component.literal("Aero: " + e.getMessage()));
            return 0;
        }
    }

    private static int disable(FabricClientCommandSource source, ModuleRuntime runtime, String id) {
        try {
            runtime.modules().disable(id);
            source.sendFeedback(Component.literal("Aero: disabled '" + id + "'"));
            return 1;
        } catch (ModuleException e) {
            source.sendError(Component.literal("Aero: " + e.getMessage()));
            return 0;
        }
    }

    private static int uninstall(FabricClientCommandSource source, ModuleRuntime runtime, String id) {
        try {
            runtime.modules().uninstall(id);
            source.sendFeedback(Component.literal("Aero: uninstalled '" + id + "'"));
            return 1;
        } catch (ModuleException e) {
            source.sendError(Component.literal("Aero: " + e.getMessage()));
            return 0;
        }
    }

    private static int update(FabricClientCommandSource source, ModuleRuntime runtime, Path modulesDir, String id, String file) {
        try {
            runtime.modules().update(id, ModuleDiscovery.of(modulesDir.resolve(file).toFile()));
            source.sendFeedback(Component.literal("Aero: updated '" + id + "' from " + file));
            return 1;
        } catch (ModuleException e) {
            source.sendError(Component.literal("Aero: " + e.getMessage()));
            return 0;
        }
    }
}
