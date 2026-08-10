package com.nexvior.client.config;

/**
 * Color configuration for a single HUD module.
 *
 * Stored as plain RGB (0xRRGGBB), never RGBA — this mod intentionally does
 * not expose an alpha/transparency slider per the project's requirements.
 * When rendering, callers are expected to OR in their own fixed alpha
 * (typically 0xFF) rather than reading one from here.
 */
public class ColorConfig {

	/** Default mode: solid, default color: white (0xFFFFFF). */
	private ColorMode mode = ColorMode.SOLID;

	/** RGB int, e.g. 0xFFAA00. Only meaningful when mode == SOLID, and
	 *  acts as the gradient's start color when mode == GRADIENT. */
	private int solidColor = 0xFFFFFF;

	/**
	 * Rainbow cycle speed, in hue-degrees per tick. Small values (e.g. 1-2)
	 * give a slow, smooth cycle; larger values cycle faster. Clamped at
	 * apply-time to a sane range so a corrupted/edited config can't produce
	 * a strobe-like effect.
	 */
	private float rainbowSpeed = 1.5f;

	/**
	 * Second color for GRADIENT mode (0xRRGGBB). solidColor acts as the
	 * gradient's start color; this is its end color. Only meaningful when
	 * mode == GRADIENT.
	 */
	private int gradientEndColor = 0x00AAFF;

	/** Internal accumulator for rainbow hue; NOT persisted (transient). */
	private transient float currentHue = 0f;

	public ColorConfig() {
		// Default no-arg constructor required for Gson deserialization.
	}

	public ColorConfig(ColorMode mode, int solidColor, float rainbowSpeed) {
		this.mode = mode;
		this.solidColor = solidColor;
		this.rainbowSpeed = rainbowSpeed;
	}

	public ColorMode getMode() {
		return mode == null ? ColorMode.SOLID : mode;
	}

	public void setMode(ColorMode mode) {
		this.mode = mode;
	}

	public int getSolidColor() {
		return solidColor;
	}

	public void setSolidColor(int solidColor) {
		// Mask to 24-bit RGB defensively — guards against a hand-edited
		// config file containing an out-of-range or negative value.
		this.solidColor = solidColor & 0xFFFFFF;
	}

	public float getRainbowSpeed() {
		return rainbowSpeed;
	}

	public void setRainbowSpeed(float rainbowSpeed) {
		// Clamp to a safe range: too high would look like a strobe light,
		// which is both an accessibility concern and visual noise (against
		// the "minimal visual clutter" design goal for PvP).
		this.rainbowSpeed = Math.max(0.1f, Math.min(rainbowSpeed, 10f));
	}

	public int getGradientEndColor() {
		return gradientEndColor;
	}

	public void setGradientEndColor(int gradientEndColor) {
		this.gradientEndColor = gradientEndColor & 0xFFFFFF;
	}

	/**
	 * Advances the rainbow hue by one tick's worth of rotation. Called from
	 * the client tick handler (never from the render callback — see
	 * HudRenderer in Stage 3), so this is safe to call unconditionally even
	 * if mode != RAINBOW (it's a cheap float add either way).
	 */
	public void tickRainbow() {
		currentHue = (currentHue + rainbowSpeed) % 360f;
	}

	/**
	 * Resolves this config's current effective RGB color. For SOLID this is
	 * just the stored value; for RAINBOW this converts the current tick's
	 * hue into RGB; for GRADIENT this returns the START color only — use
	 * GradientTextRenderer for the full per-character gradient effect, this
	 * method exists so any caller that only wants ONE color (e.g. a small
	 * icon or bar) still gets a sensible single-color fallback.
	 */
	public int resolveRgb() {
		if (getMode() == ColorMode.RAINBOW) {
			int argb = java.awt.Color.HSBtoRGB(currentHue / 360f, 1.0f, 1.0f);
			return argb & 0xFFFFFF;
		}
		return solidColor & 0xFFFFFF;
	}
}
