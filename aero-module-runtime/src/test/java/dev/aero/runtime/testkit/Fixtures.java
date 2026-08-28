package dev.aero.runtime.testkit;

/** Source templates for fixture modules compiled on the fly by {@link TestModuleBuilder}. */
public final class Fixtures {

    private Fixtures() {
    }

    public static String manifest(String id, String className, String version) {
        return """
                {
                  "id": "%s",
                  "name": "Test Module",
                  "version": "%s",
                  "entrypoint": "%s",
                  "apiVersion": "1.0"
                }
                """.formatted(id, version, className);
    }

    /** A module that logs a lifecycle trail and registers one tick listener, one HUD element, one keybind. */
    public static String wellBehaved(String className, String version) {
        return """
                import dev.aero.api.Module;
                import dev.aero.api.ModuleContext;
                import dev.aero.api.event.TickEvent;
                import dev.aero.api.ui.Keybind;

                public class %s implements Module {
                    public static final String VERSION = "%s";

                    @Override
                    public void onLoad(ModuleContext context) {
                        context.log("onLoad:" + VERSION);
                    }

                    @Override
                    public void onEnable(ModuleContext context) {
                        context.log("onEnable:" + VERSION);
                        context.events().subscribe(TickEvent.class, e -> context.log("tick:" + VERSION));
                        context.hud().register(canvas -> canvas.drawText("Aero Module Loaded! " + VERSION, 10, 10, 0xFFFFFFFF));
                        context.keybinds().register(new Keybind("toggle", "Toggle", 71, () -> context.log("press:" + VERSION)));
                    }

                    @Override
                    public void onDisable(ModuleContext context) {
                        context.log("onDisable:" + VERSION);
                    }
                }
                """.formatted(className, version);
    }

    /** A module whose tick listener always throws, to exercise failure isolation / auto-disable. */
    public static String alwaysThrowsOnTick(String className) {
        return """
                import dev.aero.api.Module;
                import dev.aero.api.ModuleContext;
                import dev.aero.api.event.TickEvent;

                public class %s implements Module {
                    @Override
                    public void onLoad(ModuleContext context) {
                    }

                    @Override
                    public void onEnable(ModuleContext context) {
                        context.log("onEnable");
                        context.events().subscribe(TickEvent.class, e -> {
                            throw new RuntimeException("boom");
                        });
                    }

                    @Override
                    public void onDisable(ModuleContext context) {
                        context.log("onDisable");
                    }
                }
                """.formatted(className);
    }

    /** A module whose entrypoint does not implement {@code Module} - manifest points at a plain class. */
    public static String notAModule(String className) {
        return """
                public class %s {
                }
                """.formatted(className);
    }
}
