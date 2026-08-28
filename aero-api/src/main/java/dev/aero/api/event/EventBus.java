package dev.aero.api.event;

import java.util.function.Consumer;

/**
 * The only way a module receives events from Aero. Every subscription made
 * through a given {@link dev.aero.api.ModuleContext}'s bus is scoped to that
 * module and is torn down automatically when the module is disabled, so a
 * module never has to remember to unsubscribe to avoid leaking.
 *
 * <p>A listener that throws is caught and logged at the module runtime
 * boundary; it never propagates into Aero or Minecraft's tick/render loop,
 * and never stops other listeners (of this module or any other) from
 * running.
 */
public interface EventBus {

    <T extends AeroEvent> Subscription subscribe(Class<T> eventType, Consumer<T> listener);
}
