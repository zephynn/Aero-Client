package dev.aero.api.event;

/**
 * Fired once per client tick. {@code tickCount} is Aero's own running tick
 * counter (starts at 0 when Aero initializes), not Minecraft's internal one.
 */
public record TickEvent(long tickCount) implements AeroEvent {
}
