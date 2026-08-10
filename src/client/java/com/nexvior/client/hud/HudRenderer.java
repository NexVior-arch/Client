package com.nexvior.client.hud;

import com.nexvior.client.NexViorClient;
import com.nexvior.client.config.ConfigManager;
import com.nexvior.client.config.ModuleConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central HUD rendering coordinator for NexVior.
 *
 * Design: registers exactly ONE HudElement with HudElementRegistry (a
 * single "canvas") rather than one per module. Inside that canvas, this
 * class iterates every registered HudModule and, if enabled, calls its
 * render(). This keeps NexVior's registry footprint minimal and gives us
 * full control over internal render order without depending on how the
 * Fabric registry orders multiple independent registrations.
 *
 * Compatibility: HudElementRegistry.addLast() is a pure Fabric API
 * registry call — it does not touch InGameHud, WorldRenderer, or any
 * class Sodium/Lithium/Iris mixin into. There is no shared injection
 * target, so there is no mixin priority/ordering concern here at all.
 */
public final class HudRenderer {

	private static final Map<String, HudModule> MODULES = new LinkedHashMap<>();

	private HudRenderer() {
	}

	/**
	 * Registers a module with NexVior's HUD system. Modules should call
	 * this once during their own initialization (wired from
	 * NexViorClient's "HUD Modules" safeInit block).
	 */
	public static void registerModule(HudModule module) {
		MODULES.put(module.getId(), module);
		NexViorClient.LOGGER.info("[NexVior] Registered HUD module '{}'.", module.getId());
	}

	public static Map<String, HudModule> getModules() {
		return MODULES;
	}

	/**
	 * Wires up the tick loop and the single HUD canvas layer. Called once
	 * from NexViorClient's "HUD Rendering System" safeInit block.
	 */
	public static void register() {
		// Tick: compute-heavy work happens here, at most 20x/sec, never
		// inside the per-frame render path below.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			for (HudModule module : MODULES.values()) {
				try {
					module.tick();
				} catch (Throwable t) {
					// A single module's tick() failing must not stop other
					// modules from ticking, nor crash the client tick loop.
					NexViorClient.LOGGER.error(
						"[NexVior] HUD module '{}' threw during tick(). Skipping this tick for it.",
						module.getId(), t
					);
				}
			}
		});

		HudElementRegistry.addLast(
			Identifier.of(NexViorClient.MOD_ID, "hud_canvas"),
			(context, tickCounter) -> {
				for (HudModule module : MODULES.values()) {
					ModuleConfig moduleConfig = ConfigManager.getModule(module.getId());
					if (!moduleConfig.isEnabled()) {
						continue;
					}
					try {
						module.render(context, tickCounter, moduleConfig);
					} catch (Throwable t) {
						// Graceful degradation (Instruction 7): one broken
						// module must never take down the rest of the HUD,
						// let alone the game. Logged once per occurrence;
						// a future improvement could rate-limit this if a
						// module fails every single frame, but even
						// unthrottled this is far cheaper than the crash
						// it prevents.
						NexViorClient.LOGGER.error(
							"[NexVior] HUD module '{}' threw during render(). " +
							"This frame's render for it was skipped.",
							module.getId(), t
						);
					}
				}
			}
		);
	}
}
