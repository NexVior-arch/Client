package com.nexvior.client.config;

/**
 * Per-module HUD configuration: whether it's enabled, its free-form screen
 * position, and its color settings. One instance exists per HUD module
 * (FPS counter, Keystrokes, Coords, Ping, CPS, Armor HUD, Potion Timer,
 * Cooldown Indicator, Crosshair, Hurt Indicator, Held Item Info, Zoom,
 * etc.) keyed by module id in NexViorConfig.modules.
 *
 * Every module defaults to DISABLED (enabled = false). NexVior never
 * turns a module on by default — the user opts in explicitly via the
 * HUD Editor or config menu. This is a deliberate product decision, not
 * an oversight.
 *
 * Position is stored as normalized screen-space anchoring (x/y in pixels
 * from top-left at a reference resolution is intentionally NOT used here —
 * instead we store raw pixel offsets AND which corner they're anchored to,
 * so the HUD stays sensibly placed across different game window sizes).
 */
public class ModuleConfig {

	private boolean enabled = false;

	/** X offset in pixels from the anchor corner. */
	private int x = 10;

	/** Y offset in pixels from the anchor corner. */
	private int y = 10;

	/**
	 * Which screen corner x/y are measured from. Using anchors (rather than
	 * absolute coordinates from top-left only) means a module dragged near
	 * the bottom-right stays sensibly placed if the user later plays at a
	 * different resolution/GUI scale.
	 */
	private Anchor anchor = Anchor.TOP_LEFT;

	private ColorConfig color = new ColorConfig();

	private StyleConfig style = new StyleConfig();

	public ModuleConfig() {
		// Default no-arg constructor required for Gson deserialization.
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Anchor getAnchor() {
		return anchor == null ? Anchor.TOP_LEFT : anchor;
	}

	public void setAnchor(Anchor anchor) {
		this.anchor = anchor;
	}

	public ColorConfig getColor() {
		if (color == null) {
			// Defensive: guards against a hand-edited config that omitted
			// the "color" object entirely — Gson would leave this null
			// rather than throwing, so we lazily repair it here instead of
			// letting a NullPointerException surface later in a render path.
			color = new ColorConfig();
		}
		return color;
	}

	public void setColor(ColorConfig color) {
		this.color = color;
	}

	public StyleConfig getStyle() {
		if (style == null) {
			// Defensive: guards against a hand-edited config that omitted
			// the "style" object entirely.
			style = new StyleConfig();
		}
		return style;
	}

	public void setStyle(StyleConfig style) {
		this.style = style;
	}

	public enum Anchor {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT
	}
}
