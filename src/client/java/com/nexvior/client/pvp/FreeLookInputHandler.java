package com.nexvior.client.pvp;

import com.nexvior.client.NexViorClient;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;

/**
 * Polls raw cursor position via GLFW directly (not by mixin-ing into
 * Minecraft's own Mouse class) to compute mouse delta for Free Look.
 *
 * Why this approach instead of a Mouse mixin: GLFW's C API surface
 * (glfwGetCursorPos) is stable and version-independent — it does not
 * change across Minecraft/Yarn updates the way internal Mouse class
 * fields/methods might. This keeps Free Look's input capture completely
 * decoupled from Minecraft's own mouse-handling internals, so it cannot
 * conflict with any mod that touches Mouse or Entity rotation logic.
 *
 * Entirely defensive: every GLFW call is wrapped, and any failure simply
 * leaves Free Look inactive for that tick rather than throwing.
 */
public final class FreeLookInputHandler {

	private static double lastX = Double.NaN;
	private static double lastY = Double.NaN;

	/** Degrees of rotation per pixel of mouse movement — matches vanilla's
	 *  default mouse sensitivity feel closely enough for a QoL feature. */
	private static final float SENSITIVITY = 0.15f;

	private FreeLookInputHandler() {
	}

	/**
	 * Called once per client tick. Only accumulates into FreeLookHandler
	 * while Free Look is active; otherwise just keeps lastX/lastY in sync
	 * so re-engaging doesn't produce a huge first-frame jump.
	 */
	public static void poll() {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.getWindow() == null) {
				return;
			}

			long windowHandle = client.getWindow().getHandle();

			DoubleBuffer xBuf = DoubleBuffer.allocate(1);
			DoubleBuffer yBuf = DoubleBuffer.allocate(1);
			GLFW.glfwGetCursorPos(windowHandle, xBuf, yBuf);
			double x = xBuf.get(0);
			double y = yBuf.get(0);

			if (Double.isNaN(lastX) || Double.isNaN(lastY)) {
				// First poll after (re)enabling — establish baseline only,
				// no delta yet, to avoid a jump from a stale/zeroed value.
				lastX = x;
				lastY = y;
				return;
			}

			double deltaX = x - lastX;
			double deltaY = y - lastY;
			lastX = x;
			lastY = y;

			if (FreeLookHandler.isActive()) {
				FreeLookHandler.addDelta(
					(float) (deltaX * SENSITIVITY),
					(float) (deltaY * SENSITIVITY)
				);
			}
		} catch (Throwable t) {
			// Defensive per Instruction 7: a GLFW/windowing quirk on some
			// platform must disable this feature gracefully, not crash.
			NexViorClient.LOGGER.error(
				"[NexVior] Free Look input polling failed; Free Look will be unavailable this session.", t
			);
			FreeLookHandler.setActive(false);
		}
	}

	/**
	 * Resets the tracked cursor baseline. Called when Free Look toggles on
	 * so the first tick after engaging doesn't apply a large accumulated
	 * delta from while it was disabled.
	 */
	public static void resetBaseline() {
		lastX = Double.NaN;
		lastY = Double.NaN;
	}
}
