package dev.aero.core.client;

import dev.aero.api.ui.HudCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Wraps a real {@code GuiGraphics} for exactly one frame - never exposed to a module directly. */
public final class GuiGraphicsHudCanvas implements HudCanvas {

    private final GuiGraphics graphics;
    private final Minecraft client;

    public GuiGraphicsHudCanvas(GuiGraphics graphics, Minecraft client) {
        this.graphics = graphics;
        this.client = client;
    }

    @Override
    public void drawText(String text, int x, int y, int argbColor) {
        graphics.drawString(client.font, text, x, y, argbColor);
    }

    @Override
    public int screenWidth() {
        return graphics.guiWidth();
    }

    @Override
    public int screenHeight() {
        return graphics.guiHeight();
    }
}
