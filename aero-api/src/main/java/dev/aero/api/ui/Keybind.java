package dev.aero.api.ui;

/**
 * A keybind requested by a module via {@link dev.aero.api.ModuleContext#keybinds()}.
 * {@code defaultKey} is a GLFW key code (e.g. {@code GLFW_KEY_G} = 71); Aero
 * owns the actual binding (including any user rebinding through Minecraft's
 * controls screen) and simply invokes {@code onPress} when it fires. The
 * binding is removed automatically when the module is disabled.
 */
public record Keybind(String id, String description, int defaultKey, Runnable onPress) {
}
