package dev.aero.official.example;

import dev.aero.api.Module;
import dev.aero.api.ModuleContext;
import dev.aero.api.event.TickEvent;
import dev.aero.api.state.PlayerInfo;
import dev.aero.api.ui.Keybind;

/**
 * Aero's first-party development module: the one used in the Phase 1
 * demonstration script (install -> HUD appears -> disable -> HUD disappears
 * -> uninstall -> replace with v2 -> update -> v2 runs, all without
 * restarting Minecraft). See {@code aero-official-modules/README.md}.
 *
 * <p>Note everything this class touches comes from {@code dev.aero.api} -
 * it never imports a single Minecraft or Fabric class.
 */
public class ExampleModule implements Module {

    private static final int TOGGLE_KEY_G = 71;

    private volatile boolean visible = true;
    private volatile long lastTick;

    @Override
    public void onLoad(ModuleContext context) {
        context.log("Example Module loaded");
    }

    @Override
    public void onEnable(ModuleContext context) {
        context.log("Example Module enabled");

        context.events().subscribe(TickEvent.class, event -> lastTick = event.tickCount());

        context.hud().register(canvas -> {
            if (!visible) {
                return;
            }
            canvas.drawText("Aero Module Loaded!", 8, 8, 0xFFFFFFFF);
            PlayerInfo player = context.gameState().player();
            String position = player == null
                    ? "no player"
                    : String.format("%.1f, %.1f, %.1f", player.x(), player.y(), player.z());
            canvas.drawText("tick " + lastTick + " | " + position, 8, 20, 0xFFAAAAAA);
        });

        context.keybinds().register(new Keybind(
                "toggle",
                "Toggle Example Module HUD",
                TOGGLE_KEY_G,
                () -> {
                    visible = !visible;
                    context.log("HUD toggled " + (visible ? "on" : "off"));
                }));
    }

    @Override
    public void onDisable(ModuleContext context) {
        context.log("Example Module disabled");
    }
}
