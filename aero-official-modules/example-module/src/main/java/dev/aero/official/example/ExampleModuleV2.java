package dev.aero.official.example;

import dev.aero.api.Module;
import dev.aero.api.ModuleContext;
import dev.aero.api.event.TickEvent;
import dev.aero.api.ui.Keybind;

/**
 * Version 2 of the example module - deliberately visibly different from v1
 * (different HUD text, different toggle key) so a hot update via
 * {@code /aero update example-module example-module-2.0.0.jar} is obviously
 * v2 taking over, not v1 continuing to run.
 */
public class ExampleModuleV2 implements Module {

    private static final int TOGGLE_KEY_H = 72;

    private volatile boolean visible = true;
    private volatile long lastTick;

    @Override
    public void onLoad(ModuleContext context) {
        context.log("Example Module v2 loaded");
    }

    @Override
    public void onEnable(ModuleContext context) {
        context.log("Example Module v2 enabled");

        context.events().subscribe(TickEvent.class, event -> lastTick = event.tickCount());

        context.hud().register(canvas -> {
            if (!visible) {
                return;
            }
            canvas.drawText("Aero Module Loaded! (v2, hot-updated)", 8, 8, 0xFF55FF55);
            canvas.drawText("tick " + lastTick, 8, 20, 0xFFAAAAAA);
        });

        context.keybinds().register(new Keybind(
                "toggle",
                "Toggle Example Module HUD",
                TOGGLE_KEY_H,
                () -> {
                    visible = !visible;
                    context.log("HUD toggled " + (visible ? "on" : "off"));
                }));
    }

    @Override
    public void onDisable(ModuleContext context) {
        context.log("Example Module v2 disabled");
    }
}
