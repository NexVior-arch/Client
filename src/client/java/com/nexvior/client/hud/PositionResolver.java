package com.nexvior.client.hud;

import com.nexvior.client.config.ModuleConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Resolves a module's configured (anchor + offset) position into actual
 * top-left screen coordinates. Centralized here so HudEditorScreen and
 * every HudModule's own render() compute the exact same position — if
 * this logic were duplicated in each module, editor and in-game position
 * could silently drift apart.
 */
public final class PositionResolver {

	private PositionResolver() {
	}

	public static int resolveX(ModuleConfig config, int moduleWidth) {
		int screenWidth = getScreenWidth();
		return switch (config.getAnchor()) {
			case TOP_LEFT, BOTTOM_LEFT -> config.getX();
			case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - config.getX() - moduleWidth;
		};
	}

	public static int resolveY(ModuleConfig config, int moduleHeight) {
		int screenHeight = getScreenHeight();
		return switch (config.getAnchor()) {
			case TOP_LEFT, TOP_RIGHT -> config.getY();
			case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - config.getY() - moduleHeight;
		};
	}

	private static int getScreenWidth() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			// Sane fallback so a module never divides-by-zero or draws at
			// a nonsensical position if called before the window exists.
			return 1920;
		}
		return client.getWindow().getScaledWidth();
	}

	private static int getScreenHeight() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return 1080;
		}
		return client.getWindow().getScaledHeight();
	}
}
