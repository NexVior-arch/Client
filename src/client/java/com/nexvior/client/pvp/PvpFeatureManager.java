package com.nexvior.client.pvp;

import com.nexvior.client.NexViorClient;
import com.nexvior.client.config.ConfigManager;
import com.nexvior.client.config.FreeLookConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and drives PvP-support features that need a persistent
 * per-tick handler: currently just Free Look. Kept separate from
 * KeybindManager (which owns the HUD Editor keybind) so PvP feature
 * keybinds and HUD keybinds can evolve independently as more PvP
 * utilities are added.
 */
public final class PvpFeatureManager {

	private static KeyBinding freeLookKey;

	private PvpFeatureManager() {
	}

	public static void register() {
		// Default: LEFT_ALT — chosen because it's rarely bound to anything
		// else during normal PvP play (WASD, mouse buttons, hotbar 1-9,
		// space/shift are all in heavy use), and matches convention from
		// comparable legit clients. Fully rebindable via vanilla Controls.
		freeLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.nexvior.free_look",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_ALT,
			"key.category.nexvior"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			try {
				tickFreeLook();
			} catch (Throwable t) {
				NexViorClient.LOGGER.error("[NexVior] Free Look tick handling failed.", t);
			}

			// Always poll input (even if Free Look is off) so the cursor
			// baseline stays fresh and re-engaging doesn't jump.
			FreeLookInputHandler.poll();

			// Mouse button state, shared by CPS and Keystrokes HUD modules.
			InputPollHandler.poll();
		});
	}

	private static void tickFreeLook() {
		FreeLookConfig config = ConfigManager.get().getFreeLook();
		if (!config.isEnabled()) {
			// Feature toggled off entirely in config/menu — never engage,
			// regardless of key state.
			if (FreeLookHandler.isActive()) {
				FreeLookHandler.setActive(false);
			}
			return;
		}

		if (config.getMode() == FreeLookConfig.Mode.HOLD) {
			boolean shouldBeActive = freeLookKey.isPressed();
			if (shouldBeActive != FreeLookHandler.isActive()) {
				if (shouldBeActive) {
					FreeLookInputHandler.resetBaseline();
				}
				FreeLookHandler.setActive(shouldBeActive);
			}
		} else {
			// TOGGLE mode: wasPressed() is edge-triggered, safe to poll
			// every tick without double-toggling on a held key.
			while (freeLookKey.wasPressed()) {
				boolean newState = !FreeLookHandler.isActive();
				if (newState) {
					FreeLookInputHandler.resetBaseline();
				}
				FreeLookHandler.setActive(newState);
			}
		}
	}
}
