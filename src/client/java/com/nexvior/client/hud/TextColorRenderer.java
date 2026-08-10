package com.nexvior.client.hud;

import com.nexvior.client.config.ColorConfig;
import com.nexvior.client.config.ColorMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Single entry point for drawing a HUD module's text content, dispatching
 * to the right rendering path based on the module's configured ColorMode
 * (SOLID / RAINBOW / GRADIENT). Introduced in Stage 7 so existing modules
 * (FPS, Coords, Ping, etc. from Stage 5) can gain gradient support by
 * switching their single drawTextWithShadow() call to this helper,
 * without each module needing its own mode-dispatch logic.
 */
public final class TextColorRenderer {

	private TextColorRenderer() {
	}

	public static void draw(DrawContext context, TextRenderer textRenderer, String text, int x, int y, ColorConfig colorConfig) {
		if (colorConfig.getMode() == ColorMode.GRADIENT) {
			GradientTextRenderer.draw(context, textRenderer, text, x, y, colorConfig);
		} else {
			// SOLID and RAINBOW both resolve to a single effective color
			// already (RAINBOW's hue is precomputed per-tick in
			// ColorConfig#tickRainbow(), called from HudRenderer's tick
			// loop — see Stage 3/5), so both share this simple path.
			int color = colorConfig.resolveRgb() | 0xFF000000;
			context.drawTextWithShadow(textRenderer, text, x, y, color);
		}
	}

	/** Measures the width text would occupy when drawn via draw() above.
	 *  Gradient rendering draws the same glyphs as normal text, just in
	 *  different colors, so width is identical to a normal measurement. */
	public static int measureWidth(TextRenderer textRenderer, String text) {
		return textRenderer.getWidth(text);
	}
}
