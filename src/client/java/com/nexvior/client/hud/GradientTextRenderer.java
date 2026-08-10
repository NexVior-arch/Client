package com.nexvior.client.hud;

import com.nexvior.client.config.ColorConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Draws text with a smooth left-to-right color gradient between two RGB
 * colors, one character at a time. Minecraft's TextRenderer/DrawContext
 * has no built-in multi-color gradient text API, so this composes the
 * effect from ordinary single-color drawTextWithShadow() calls — no
 * shader, no custom GL state, nothing that could conflict with Iris's
 * shader pipeline.
 *
 * Cost is linear in string length (one draw call per character); for
 * HUD-length labels (a handful to a few dozen characters) this is
 * negligible next to vanilla's own per-frame HUD drawing.
 */
public final class GradientTextRenderer {

	private GradientTextRenderer() {
	}

	/**
	 * Draws text starting at (x, y) with each character's color linearly
	 * interpolated between colorConfig's start (solidColor) and end
	 * (gradientEndColor) colors. Returns the total pixel width drawn, so
	 * callers can lay out subsequent content.
	 */
	public static int draw(DrawContext context, TextRenderer textRenderer, String text, int x, int y, ColorConfig colorConfig) {
		if (text.isEmpty()) {
			return 0;
		}

		int startColor = colorConfig.getSolidColor();
		int endColor = colorConfig.getGradientEndColor();

		int startR = (startColor >> 16) & 0xFF;
		int startG = (startColor >> 8) & 0xFF;
		int startB = startColor & 0xFF;
		int endR = (endColor >> 16) & 0xFF;
		int endG = (endColor >> 8) & 0xFF;
		int endB = endColor & 0xFF;

		int length = text.length();
		int cursorX = x;

		for (int i = 0; i < length; i++) {
			char c = text.charAt(i);
			// Denominator uses (length - 1) so a single-character string
			// doesn't divide by zero, and the LAST character always lands
			// exactly on the end color rather than falling just short.
			float t = length <= 1 ? 0f : (float) i / (length - 1);

			int r = (int) (startR + (endR - startR) * t);
			int g = (int) (startG + (endG - startG) * t);
			int b = (int) (startB + (endB - startB) * t);
			int color = 0xFF000000 | (r << 16) | (g << 8) | b;

			String charStr = String.valueOf(c);
			context.drawTextWithShadow(textRenderer, charStr, cursorX, y, color);
			cursorX += textRenderer.getWidth(charStr);
		}

		return cursorX - x;
	}

	/** Computes total rendered width without drawing, for layout purposes. */
	public static int measureWidth(TextRenderer textRenderer, String text) {
		return textRenderer.getWidth(text);
	}
}
