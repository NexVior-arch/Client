package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import com.nexvior.client.hud.ModuleStyleRenderer;
import com.nexvior.client.hud.PositionResolver;
import com.nexvior.client.hud.TextColorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * FPS Counter module.
 *
 * Implementation note: rather than reading a Minecraft-internal FPS field
 * (unverified for this exact Yarn build, and risky to guess), this module
 * counts frames itself: each render() call increments a counter, and once
 * per second (measured via System.nanoTime(), independent of any
 * Minecraft internal) the counter becomes the displayed value and resets.
 * This is self-contained and version-independent.
 *
 * Per Instruction 7: the only per-frame work is an integer increment and
 * a timestamp comparison the actual FPS math runs at most once/sec.
 */
public class FpsModule implements HudModule {

	private static final String ID = "fps";

	private int frameCounter = 0;
	private long windowStartNanos = System.nanoTime();
	private int displayedFps = 0;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "FPS Counter";
	}

	@Override
	public void tick() {
		// Frame counting happens in render() since it must count actual
		// rendered frames (which can exceed the 20/sec tick rate).
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		frameCounter++;

		long now = System.nanoTime();
		long elapsedNanos = now - windowStartNanos;
		if (elapsedNanos >= 1_000_000_000L) {
			displayedFps = (int) Math.round(frameCounter * (1_000_000_000.0 / elapsedNanos));
			frameCounter = 0;
			windowStartNanos = now;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		String text = displayedFps + " FPS";
		int textWidth = client.textRenderer.getWidth(text);
		int textHeight = client.textRenderer.fontHeight;

		int x = PositionResolver.resolveX(config, textWidth + config.getStyle().getPadding() * 2);
		int y = PositionResolver.resolveY(config, textHeight + config.getStyle().getPadding() * 2);

		int[] origin = ModuleStyleRenderer.drawBackground(context, config.getStyle(), x, y, textWidth, textHeight);

		TextColorRenderer.draw(context, client.textRenderer, text, origin[0], origin[1], config.getStyle().getTextColor());
	}

	@Override
	public int getWidth() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (client == null || client.textRenderer == null)
			? 40 : client.textRenderer.getWidth(displayedFps + " FPS");
	}

	@Override
	public int getHeight() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (client == null || client.textRenderer == null) ? 10 : client.textRenderer.fontHeight;
	}
}
