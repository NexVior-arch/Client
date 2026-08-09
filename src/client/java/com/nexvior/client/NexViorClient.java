package com.nexvior.client;

import com.nexvior.client.config.ConfigManager;
import com.nexvior.client.hud.HudRenderer;
import com.nexvior.client.hud.KeybindManager;
import com.nexvior.client.hud.ModuleRegistry;
import com.nexvior.client.pvp.PvpFeatureManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NexVior Client — main client entrypoint.
 *
 * Architecture note: this mod is CLIENT-ONLY (see fabric.mod.json
 * "environment": "client"). It never touches server-side game state,
 * ships no server logic, and therefore cannot desync or crash a
 * dedicated server.
 *
 * Fault tolerance: every subsystem is initialized behind its own
 * try/catch. If one subsystem fails (e.g. because of an unexpected
 * class/hook removal in a future MC patch, or a conflicting mod),
 * the failure is logged and NexVior continues loading its remaining
 * features rather than crashing the whole game (Instruction 7:
 * graceful degradation).
 */
public class NexViorClient implements ClientModInitializer {

	public static final String MOD_ID = "nexvior";
	public static final Logger LOGGER = LoggerFactory.getLogger("NexVior Client");

	@Override
	public void onInitializeClient() {
		LOGGER.info("[NexVior] Initializing client mod...");

		// Config must load first; every other subsystem depends on it.
		// ConfigManager itself is internally fault-tolerant (regenerates
		// safe defaults on missing/corrupt file — see Stage: Config System).
		safeInit("Config", ConfigManager::load);

		safeInit("HUD Rendering System", HudRenderer::register);

		safeInit("HUD Modules", ModuleRegistry::registerAll);

		safeInit("PvP Features", PvpFeatureManager::register);

		safeInit("Keybindings", KeybindManager::register);

		// Persist config on clean client shutdown. Wrapped the same way as
		// every other subsystem: if this registration itself fails (e.g.
		// the event class is missing due to an unexpected Fabric API
		// version mismatch), NexVior logs it and continues rather than
		// crashing — the config simply won't auto-save on exit, but
		// explicit saves (e.g. from the HUD Editor) still work.
		safeInit("Shutdown Save Hook", () ->
			ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save())
		);

		LOGGER.info("[NexVior] Client initialization complete.");
	}

	/**
	 * Runs a subsystem initializer defensively. A failure here is logged
	 * with full context and does NOT propagate — this is the core
	 * mechanism backing Instruction 7's "no unhandled exception at
	 * startup" requirement.
	 */
	private void safeInit(String subsystemName, Runnable initializer) {
		try {
			initializer.run();
			LOGGER.info("[NexVior] {} initialized successfully.", subsystemName);
		} catch (Throwable t) {
			LOGGER.error(
				"[NexVior] Failed to initialize subsystem '{}'. This feature will be disabled, " +
				"but NexVior will continue loading. This usually means a hook or class NexVior " +
				"depends on was missing or changed — often due to a Minecraft/Fabric API update " +
				"or a conflicting mod.",
				subsystemName,
				t
			);
		}
	}
}
