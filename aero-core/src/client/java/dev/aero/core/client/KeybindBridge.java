package dev.aero.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aero.runtime.ui.KeybindRegistryImpl;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Aero's Minecraft-agnostic {@link KeybindRegistryImpl} to real
 * Fabric {@code KeyMapping}s. This is the only place aero-core creates a
 * keybinding - a module never sees a {@code KeyMapping}, only the
 * {@code Keybind} it registered through {@code ModuleContext}.
 *
 * <p>Fabric API supports registering a {@code KeyMapping} after client init
 * (that's what {@code fabric-key-binding-api-v1} exists for), so a module
 * enabled while the game is already running gets a real, working binding
 * immediately. There is, however, no matching "unregister" - when a module
 * disables, its {@code KeyMapping} is simply stopped being polled (made
 * inert) rather than removed from the controls screen. That is a known,
 * documented Phase 1 limitation, not an oversight.
 */
public final class KeybindBridge {

    private static final KeyMapping.Category AERO_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aero", "modules"));

    private final Map<String, KeyMapping> polled = new ConcurrentHashMap<>();
    private final KeybindRegistryImpl registry;

    public KeybindBridge(KeybindRegistryImpl registry) {
        this.registry = registry;
        registry.addListener(new KeybindRegistryImpl.Listener() {
            @Override
            public void onRegistered(KeybindRegistryImpl.Entry entry) {
                String translationKey = "key.aero." + entry.qualifiedId().replace(':', '.');
                KeyMapping mapping = new KeyMapping(
                        translationKey, InputConstants.Type.KEYSYM, entry.keybind().defaultKey(), AERO_CATEGORY);
                KeyBindingHelper.registerKeyBinding(mapping);
                polled.put(entry.qualifiedId(), mapping);
            }

            @Override
            public void onUnregistered(KeybindRegistryImpl.Entry entry) {
                polled.remove(entry.qualifiedId());
            }
        });
    }

    /** Call once per client tick, after the module runtime's own tick(). */
    public void poll() {
        for (Map.Entry<String, KeyMapping> entry : polled.entrySet()) {
            while (entry.getValue().consumeClick()) {
                registry.firePress(entry.getKey());
            }
        }
    }
}
