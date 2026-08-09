package com.nexvior.client.config;

/**
 * Color mode for a HUD module's text/accent color.
 *
 * SOLID  — a fixed RGB color chosen via an HSV color wheel in the HUD Editor.
 * RAINBOW — a hue that rotates over time (tick-based, deterministic), giving
 *           a smooth cycling color effect without any per-frame overhead
 *           beyond a single HSV->RGB conversion per tick.
 */
public enum ColorMode {
	SOLID,
	RAINBOW,
	GRADIENT
}
