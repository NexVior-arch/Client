package com.nexvior.client.hud;

import com.nexvior.client.config.StyleConfig;
import net.minecraft.client.gui.DrawContext;

/**
 * Centralized rendering helper for HUD module chrome (background box with
 * optional rounded corners and transparency). Every module should call
 * drawBackground() before drawing its own content, so all 12 modules share
 * one consistent visual language instead of each reimplementing box
 * drawing slightly differently.
 *
 * Kept deliberately simple (rectangle fills, no shader/GL state changes)
 * so it never touches the GL pipeline state Iris manages for shader
 * passes — this is pure 2D DrawContext fill calls, identical in kind to
 * what vanilla's own HUD already does every frame.
 */
public final class ModuleStyleRenderer {

	private ModuleStyleRenderer() {
	}

	/**
	 * Draws the module's background box at (x, y) with the given content
	 * size. Returns the content origin (x + padding, y + padding) so the
	 * caller knows where to start drawing its actual content.
	 */
	public static int[] drawBackground(DrawContext context, StyleConfig style, int x, int y, int contentWidth, int contentHeight) {
		if (!style.isBackgroundEnabled()) {
			return new int[] { x, y };
		}

		int padding = style.getPadding();
		int boxWidth = contentWidth + padding * 2;
		int boxHeight = contentHeight + padding * 2;
		int radius = style.getBackgroundRadius();
		int color = style.getBackgroundColor();

		if (radius <= 0) {
			context.fill(x, y, x + boxWidth, y + boxHeight, color);
		} else {
			drawRoundedRect(context, x, y, boxWidth, boxHeight, radius, color);
		}

		return new int[] { x + padding, y + padding };
	}

	/**
	 * Approximates a rounded rectangle using plain axis-aligned fill()
	 * calls: a central cross of two overlapping rectangles, plus small
	 * corner squares inset by the radius. This is a deliberately cheap
	 * approximation (not a true anti-aliased circle) — it looks clean at
	 * the small radii (0-16px) this mod exposes, and costs only a handful
	 * of extra fill() calls per module per frame, which is negligible
	 * next to vanilla's own HUD draw calls.
	 */
	private static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		radius = Math.min(radius, Math.min(width, height) / 2);

		// Center cross.
		context.fill(x + radius, y, x + width - radius, y + height, color);
		context.fill(x, y + radius, x + radius, y + height - radius, color);
		context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

		// Corner squares (kept square rather than circular — an
		// intentional simplification, see method doc above).
		context.fill(x, y, x + radius, y + radius, color);
		context.fill(x + width - radius, y, x + width, y + radius, color);
		context.fill(x, y + height - radius, x + radius, y + height, color);
		context.fill(x + width - radius, y + height - radius, x + width, y + height, color);
	}
}
