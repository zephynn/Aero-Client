package dev.aero.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aero.core.Aero;
import dev.aero.runtime.ui.KeybindRegistryImpl;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Aero's Minecraft-agnostic {@link KeybindRegistryImpl} to real
 * Fabric {@code KeyMapping}s. This is the only place aero-core creates a
 * keybinding - a module never sees a {@code KeyMapping}, only the
 * {@code Keybind} it registered through {@code ModuleContext}.
 *
 * <p><b>Why a fixed slot pool, not one {@code KeyMapping} per module:</b>
 * {@code KeyBindingHelper.registerKeyBinding} - creating a brand new
 * {@code KeyMapping} - only works before Minecraft's {@code GameOptions} is
 * initialized (i.e. during mod init). Calling it later, when a module is
 * installed and enabled while the game is already running, throws
 * {@code IllegalStateException: GameOptions has already been initialised}
 * (confirmed against a real run - this was a real Phase 1 bug, not a
 * hypothetical). {@code KeyMapping.setKey(...)}, on the other hand - the
 * same call the vanilla Controls screen uses to apply a live rebind - works
 * at any time. So this pre-registers a fixed pool of generic, initially
 * unbound {@code KeyMapping}s once at boot, and dynamically assigns/frees
 * one to a module's requested default key as that module enables/disables.
 */
public final class KeybindBridge {

    private static final KeyMapping.Category AERO_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aero", "modules"));

    /** How many modules can have an active keybind at once. Bump this if Phase 1 testing needs more. */
    private static final int SLOT_COUNT = 16;

    private final KeyMapping[] slots = new KeyMapping[SLOT_COUNT];
    private final Deque<Integer> freeSlots = new ArrayDeque<>();
    private final Map<String, Integer> assignedSlot = new ConcurrentHashMap<>();
    private final KeybindRegistryImpl registry;

    public KeybindBridge(KeybindRegistryImpl registry) {
        this.registry = registry;

        for (int i = 0; i < SLOT_COUNT; i++) {
            KeyMapping mapping = new KeyMapping(
                    "key.aero.slot" + i, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, AERO_CATEGORY);
            KeyBindingHelper.registerKeyBinding(mapping);
            slots[i] = mapping;
            freeSlots.addLast(i);
        }

        registry.addListener(new KeybindRegistryImpl.Listener() {
            @Override
            public void onRegistered(KeybindRegistryImpl.Entry entry) {
                Integer slot = freeSlots.pollFirst();
                if (slot == null) {
                    Aero.warn("No free Aero keybind slot for '{}' (all {} are in use) - "
                            + "its keybind will not respond to input", entry.qualifiedId(), SLOT_COUNT);
                    return;
                }
                slots[slot].setKey(InputConstants.Type.KEYSYM.getOrCreate(entry.keybind().defaultKey()));
                assignedSlot.put(entry.qualifiedId(), slot);
            }

            @Override
            public void onUnregistered(KeybindRegistryImpl.Entry entry) {
                Integer slot = assignedSlot.remove(entry.qualifiedId());
                if (slot != null) {
                    slots[slot].setKey(InputConstants.UNKNOWN);
                    freeSlots.addFirst(slot);
                }
            }
        });
    }

    /** Call once per client tick, after the module runtime's own tick(). */
    public void poll() {
        for (Map.Entry<String, Integer> entry : assignedSlot.entrySet()) {
            KeyMapping mapping = slots[entry.getValue()];
            while (mapping.consumeClick()) {
                registry.firePress(entry.getKey());
            }
        }
    }
}
