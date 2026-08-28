package dev.aero.api.event;

/**
 * Marker interface for events dispatched to modules through the
 * {@link EventBus}. Aero controls what implements this - a module cannot
 * fabricate raw Minecraft events, only receive the abstractions Aero chooses
 * to expose (see {@link TickEvent}, {@link RenderEvent}).
 */
public interface AeroEvent {
}
