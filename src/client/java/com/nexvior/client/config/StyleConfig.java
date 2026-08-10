package com.nexvior.client.config;

/**
 * Visual styling shared by every HUD module: background box (with
 * optional transparency and rounded corners) and text color. This is
 * intentionally separate from ColorConfig (which is content/text color
 * only, RGB, no alpha) because a background box is a distinct UI element
 * that legitimately needs an alpha channel — unlike module content color,
 * which the project deliberately keeps as solid/rainbow RGB only.
 */
public class StyleConfig {

	private boolean backgroundEnabled = true;

	/** ARGB int, e.g. 0x80000000 for 50% black. Alpha IS used here. */
	private int backgroundColor = 0x80000000;

	/** Corner radius in pixels for the background box. 0 = sharp corners. */
	private int backgroundRadius = 3;

	/** Padding in pixels between the background edge and the module content. */
	private int padding = 4;

	/** Text/content color — reuses the same SOLID/RAINBOW system as elsewhere. */
	private ColorConfig textColor = new ColorConfig();

	public StyleConfig() {
		// Default no-arg constructor required for Gson deserialization.
	}

	public boolean isBackgroundEnabled() {
		return backgroundEnabled;
	}

	public void setBackgroundEnabled(boolean backgroundEnabled) {
		this.backgroundEnabled = backgroundEnabled;
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public int getBackgroundRadius() {
		return backgroundRadius;
	}

	public void setBackgroundRadius(int backgroundRadius) {
		// Clamp defensively: a corrupted/hand-edited config with a huge
		// radius shouldn't be able to produce a degenerate/negative-size
		// draw call downstream.
		this.backgroundRadius = Math.max(0, Math.min(backgroundRadius, 16));
	}

	public int getPadding() {
		return padding;
	}

	public void setPadding(int padding) {
		this.padding = Math.max(0, Math.min(padding, 20));
	}

	public ColorConfig getTextColor() {
		if (textColor == null) {
			// Defensive: hand-edited config missing "textColor" entirely.
			textColor = new ColorConfig();
		}
		return textColor;
	}

	public void setTextColor(ColorConfig textColor) {
		this.textColor = textColor;
	}
}
