package com.nexvior.client.pvp;

import com.nexvior.client.NexViorClient;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Centralized GLFW mouse-button polling, shared by the CPS counter and
 * Keystrokes HUD modules so both read from one place instead of each
 * polling GLFW independently.
 *
 * Kept in the pvp package alongside FreeLookInputHandler since both are
 * "read raw input state without mixin-ing into Mouse" utilities, but this
 * class is independent of Free Look — it can be polled regardless of
 * whether Free Look is active.
 */
public final class InputPollHandler {

	private static boolean leftDown = false;
	private static boolean rightDown = false;

	private InputPollHandler() {
	}

	/**
	 * Called once per client tick. Updates cached button states. Wrapped
	 * defensively so a windowing quirk disables this polling for the
	 * session rather than crashing.
	 */
	public static void poll() {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.getWindow() == null) {
				return;
			}
			long handle = client.getWindow().getHandle();

			leftDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
			rightDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		} catch (Throwable t) {
			NexViorClient.LOGGER.error(
				"[NexVior] Mouse button polling failed; CPS/Keystrokes modules may show stale state.", t
			);
		}
	}

	public static boolean isLeftDown() {
		return leftDown;
	}

	public static boolean isRightDown() {
		return rightDown;
	}
}
