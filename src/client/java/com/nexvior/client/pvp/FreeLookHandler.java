package com.nexvior.client.pvp;

import com.nexvior.client.config.ConfigManager;
import com.nexvior.client.config.FreeLookConfig;

/**
 * Holds the current Free Look camera offset (extra yaw/pitch applied ONLY
 * to the rendered camera, never to the player entity's actual rotation).
 *
 * ⚠️ CURRENTLY UNUSED: PvpFeatureManager no longer calls into this class
 * or FreeLookInputHandler, because FreeLookInputHandler's GLFW cursor
 * polling was found to crash natively (SIGSEGV) on Android via
 * PojavLauncher/Zalith Launcher's GLFW emulation layer. See
 * PvpFeatureManager's class doc for the full crash log and rationale
 * before re-wiring this.
 *
 * This class is intentionally "dumb" — it just stores two floats and
 * whether Free Look is currently active. The actual mouse-look input
 * capture and the camera-render override live in FreeLookMixin, kept
 * separate so the mixin stays as small and low-risk as possible.
 *
 * IMPORTANT: this offset is READ ONLY by the render-side Camera mixin. It
 * must never be written to the player's actual yaw/pitch fields, which is
 * what keeps this feature from affecting server-perceived look direction.
 */
public final class FreeLookHandler {

	private static boolean active = false;
	private static float yawOffset = 0f;
	private static float pitchOffset = 0f;

	private FreeLookHandler() {
	}

	public static boolean isActive() {
		return active && ConfigManager.get().getFreeLook().isEnabled();
	}

	public static void setActive(boolean value) {
		active = value;
		if (!value) {
			// Snap back to normal view the instant Free Look disengages —
			// matches the "release and keep moving exactly where you were
			// headed" behavior of comparable legit implementations.
			yawOffset = 0f;
			pitchOffset = 0f;
		}
	}

	public static float getYawOffset() {
		return yawOffset;
	}

	public static float getPitchOffset() {
		return pitchOffset;
	}

	/**
	 * Accumulates mouse delta into the free-look offset. Called from the
	 * mouse input mixin ONLY while Free Look is active, in place of (not
	 * in addition to) the normal player-rotation update.
	 */
	public static void addDelta(float deltaYaw, float deltaPitch) {
		yawOffset += deltaYaw;
		pitchOffset = clampPitch(pitchOffset + deltaPitch);
		yawOffset = wrapYaw(yawOffset);
	}

	private static float clampPitch(float pitch) {
		return Math.max(-90f, Math.min(90f, pitch));
	}

	private static float wrapYaw(float yaw) {
		yaw %= 360f;
		if (yaw < -180f) yaw += 360f;
		if (yaw > 180f) yaw -= 360f;
		return yaw;
	}

	public static FreeLookConfig.Mode getMode() {
		return ConfigManager.get().getFreeLook().getMode();
	}
}
