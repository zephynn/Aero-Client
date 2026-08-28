package dev.aero.api.event;

import dev.aero.api.ui.HudCanvas;

/**
 * Fired once per rendered frame, after the game HUD has drawn. {@code canvas}
 * is Aero's own drawing surface abstraction - modules never receive a raw
 * Minecraft render context.
 */
public record RenderEvent(HudCanvas canvas, float partialTick) implements AeroEvent {
}
