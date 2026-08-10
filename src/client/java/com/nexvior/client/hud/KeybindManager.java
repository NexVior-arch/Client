package com.nexvior.client.hud;

import com.nexvior.client.NexViorClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Registers NexVior's keybindings. Currently: one keybind to open the HUD
 * Editor directly (works even without Mod Menu installed).
 *
 * Default: GLFW_KEY_RIGHT_BRACKET ("]"), chosen because it's unbound by
 * vanilla and unlikely to collide with common WASD/inventory/hotbar binds
 * used during PvP. Rebindable normally through Minecraft's Controls menu
 * since this is a real KeyBinding, not a raw key listener.
 */
public final class KeybindManager {

	/**
	 * Shared category for all of NexVior's keybinds, so they group
	 * together under one heading in the vanilla Controls screen. Category
	 * changed from a plain String to KeyBinding.Category (an Identifier-
	 * backed record) as of a recent Minecraft version — confirmed via a
	 * real CI compile error, not assumed from stale documentation.
	 */
	public static final KeyBinding.Category CATEGORY =
		KeyBinding.Category.create(Identifier.of(NexViorClient.MOD_ID, "main"));

	private static KeyBinding openHudEditorKey;

	private KeybindManager() {
	}

	public static void register() {
		openHudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.nexvior.open_hud_editor",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_BRACKET,
			CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// wasPressed() is edge-triggered (true once per physical press),
			// so this is safe to poll every tick without opening multiple
			// screens per key-hold.
			while (openHudEditorKey.wasPressed()) {
				openEditorSafely(client);
			}
		});
	}

	private static void openEditorSafely(MinecraftClient client) {
		try {
			if (client.currentScreen == null) {
				client.setScreen(new HudEditorScreen(null));
			}
		} catch (Throwable t) {
			NexViorClient.LOGGER.error(
				"[NexVior] Failed to open the HUD Editor screen from keybind.", t
			);
		}
	}
}
