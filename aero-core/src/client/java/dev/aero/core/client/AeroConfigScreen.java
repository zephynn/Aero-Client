package dev.aero.core.client;

import dev.aero.api.InstalledModule;
import dev.aero.api.ModuleManifest;
import dev.aero.api.ModuleState;
import dev.aero.api.exception.ModuleException;
import dev.aero.core.Aero;
import dev.aero.core.LogBuffer;
import dev.aero.runtime.ModuleRuntime;
import dev.aero.runtime.pkg.JarModulePackage;
import dev.aero.runtime.pkg.ModuleDiscovery;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A deliberately plain, vanilla-styled screen: the list of installed
 * modules with enable/disable/uninstall buttons, a reload button, and a
 * scroll-free log panel showing recent Aero activity. Reachable from the
 * Mod Menu mods list (see {@link AeroModMenuApi}) when Mod Menu is
 * installed - Aero has no hard dependency on it.
 *
 * <p>This is a basic management screen, not the eventual Community Modules
 * browser/marketplace UI (out of scope for this phase) - no icons, no
 * search, no scrolling list widget, just rows of buttons and text.
 */
public final class AeroConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int LOG_PANEL_HEIGHT = 80;
    private static final int LOG_LINES_SHOWN = 6;

    private final Screen parent;
    private final ModuleRuntime runtime;
    private String statusLine = "";

    public AeroConfigScreen(Screen parent) {
        super(Component.literal("Aero"));
        this.parent = parent;
        this.runtime = Aero.runtime;
    }

    @Override
    protected void init() {
        if (runtime == null) {
            // Aero's client init hasn't run yet (shouldn't normally happen -
            // this screen is only reachable once the game has loaded).
            addRenderableWidget(Button.builder(Component.literal("Aero is not ready yet"), b -> {
            }).bounds(width / 2 - 100, height / 2, 200, 20).build());
            addRenderableWidget(doneButton());
            return;
        }

        List<InstalledModule> modules = new ArrayList<>(runtime.modules().getModules());
        int listTop = 34;
        int listBottom = height - LOG_PANEL_HEIGHT - 34;
        int maxRows = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);

        for (int i = 0; i < Math.min(modules.size(), maxRows); i++) {
            InstalledModule module = modules.get(i);
            int rowY = listTop + i * ROW_HEIGHT;

            addRenderableWidget(Button.builder(toggleLabel(module), b -> onToggle(module))
                    .bounds(width - 190, rowY, 85, 18)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Uninstall"), b -> onUninstall(module))
                    .bounds(width - 100, rowY, 85, 18)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("Reload Modules Folder"), b -> onReload())
                .bounds(10, height - LOG_PANEL_HEIGHT - 26, 180, 20)
                .build());

        addRenderableWidget(doneButton());
    }

    private Button doneButton() {
        return Button.builder(Component.literal("Done"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 60, height - 24, 120, 20)
                .build();
    }

    private static Component toggleLabel(InstalledModule module) {
        return Component.literal(module.state() == ModuleState.ENABLED ? "Disable" : "Enable");
    }

    private void onToggle(InstalledModule module) {
        try {
            if (module.state() == ModuleState.ENABLED) {
                runtime.modules().disable(module.id());
                statusLine = "Disabled '" + module.id() + "'";
            } else {
                runtime.modules().enable(module.id());
                statusLine = "Enabled '" + module.id() + "'";
            }
        } catch (ModuleException e) {
            statusLine = "Error: " + e.getMessage();
        }
        refresh();
    }

    private void onUninstall(InstalledModule module) {
        try {
            runtime.modules().uninstall(module.id());
            statusLine = "Uninstalled '" + module.id() + "'";
        } catch (ModuleException e) {
            statusLine = "Error: " + e.getMessage();
        }
        refresh();
    }

    private void onReload() {
        Path modulesDir = Aero.modulesDir;
        if (modulesDir == null) {
            statusLine = "No modules directory known";
            return;
        }
        int loaded = 0;
        try {
            for (JarModulePackage pkg : ModuleDiscovery.scan(modulesDir)) {
                try {
                    ModuleManifest manifest = pkg.manifest();
                    if (runtime.modules().getModule(manifest.id()).isPresent()) {
                        continue;
                    }
                    runtime.modules().install(pkg);
                    runtime.modules().enable(manifest.id());
                    Aero.info("Loaded and enabled module '{}' v{}", manifest.id(), manifest.version());
                    loaded++;
                } catch (ModuleException e) {
                    Aero.error("Failed to load module from " + pkg.jarFile().getName(), e);
                }
            }
            statusLine = loaded == 0 ? "No new modules found" : "Loaded " + loaded + " new module(s)";
        } catch (IOException e) {
            statusLine = "Error scanning modules directory: " + e.getMessage();
        }
        refresh();
    }

    private void refresh() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // This screen is opened from Mod Menu's mods list, which is already
        // blurred - the full renderBackground() would blur a second time in
        // the same frame and Minecraft throws for that. renderTransparentBackground
        // is the same "flat dim, no blur" call vanilla sub-screens use (e.g.
        // Options sub-screens opened over the main Options screen).
        renderTransparentBackground(graphics);
        graphics.drawCenteredString(font, "Aero - Community Modules", width / 2, 12, 0xFFFFFFFF);

        if (runtime != null) {
            List<InstalledModule> modules = new ArrayList<>(runtime.modules().getModules());
            int listTop = 34;
            for (int i = 0; i < modules.size(); i++) {
                int rowY = listTop + i * ROW_HEIGHT;
                if (rowY > height - LOG_PANEL_HEIGHT - 40) {
                    graphics.drawString(font, "+" + (modules.size() - i) + " more not shown", 10, rowY + 5, 0xFFAAAAAA);
                    break;
                }
                InstalledModule module = modules.get(i);
                graphics.drawString(font, moduleLine(module), 10, rowY + 5, stateColor(module.state()));
            }
            if (modules.isEmpty()) {
                graphics.drawString(font, "No modules installed - drop a .jar into aero/modules", 10, listTop + 5, 0xFFAAAAAA);
            }
        }

        renderLogPanel(graphics);

        if (!statusLine.isEmpty()) {
            graphics.drawCenteredString(font, statusLine, width / 2, height - LOG_PANEL_HEIGHT - 46, 0xFFFFFF55);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLogPanel(GuiGraphics graphics) {
        int top = height - LOG_PANEL_HEIGHT;
        int bottom = height - 30;
        graphics.fill(8, top, width - 8, bottom, 0x80000000);
        List<String> lines = LogBuffer.INSTANCE.recent(LOG_LINES_SHOWN);
        int y = top + 4;
        for (String line : lines) {
            graphics.drawString(font, line, 12, y, 0xFFCCCCCC);
            y += 10;
        }
    }

    private static String moduleLine(InstalledModule module) {
        return module.id() + "  v" + module.manifest().version() + "  [" + module.state() + "]";
    }

    private static int stateColor(ModuleState state) {
        return switch (state) {
            case ENABLED -> 0xFF55FF55;
            case FAILED -> 0xFFFF5555;
            case DISABLED, LOADED -> 0xFFFFFF55;
            default -> 0xFFAAAAAA;
        };
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
