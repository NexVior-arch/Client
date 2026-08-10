package com.nexvior.client.pvp;

import com.nexvior.client.NexViorClient;
import com.nexvior.client.hud.KeybindManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and drives PvP-support features that need a persistent
 * per-tick handler.
 *
 * FREE LOOK DISABLED: Free Look (and its underlying FreeLookInputHandler,
 * which polled GLFW.glfwGetCursorPos every tick) has been intentionally
 * disabled. It was found to trigger a native SIGSEGV crash (not a
 * catchable Java exception — segfaults happen below the JVM, so no
 * try/catch in this codebase could ever have caught it) specifically on
 * Android via PojavLauncher/Zalith Launcher's libpojavexec.so GLFW
 * emulation layer, confirmed via a real crash log:
 *   "C  [libpojavexec.so+0xb548]  Java_org_lwjgl_glfw_GLFW_nglfwGetCursorPos"
 * This is a limitation of that Android GLFW emulation layer, not a bug in
 * NexVior's logic — the same code is a standard, safe pattern on desktop
 * Java Minecraft. Since NexVior cannot distinguish "real desktop GLFW" from
 * "emulated Android GLFW" at runtime in a way that would let it safely
 * avoid the crashing call, the safest fix is removing the call entirely.
 * The keybind is left registered (harmless, just unused) so a future
 * reintroduction of this feature has a natural home; FreeLookHandler and
 * FreeLookInputHandler source files are left in place but are no longer
 * invoked.
 */
public final class PvpFeatureManager {

	private static KeyBinding freeLookKey;

	private PvpFeatureManager() {
	}

	public static void register() {
		// Kept registered for potential future re-enablement / rebinding
		// visibility in Controls, but no longer wired to any tick logic.
		freeLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.nexvior.free_look",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_ALT,
			KeybindManager.CATEGORY
		));

		NexViorClient.LOGGER.info(
			"[NexVior] Free Look is currently disabled (see PvpFeatureManager source comments — " +
			"Android/PojavLauncher GLFW cursor-position crash)."
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Mouse BUTTON state (glfwGetMouseButton) is unaffected by the
			// crash above — that's a different native call than
			// glfwGetCursorPos, and CI/user testing has shown no issue
			// with it. CPS and Keystrokes modules still depend on this.
			InputPollHandler.poll();
		});
	}
}
