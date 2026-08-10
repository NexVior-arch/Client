package com.nexvior.client.hud;

import com.nexvior.client.config.ModuleConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Contract every HUD module (FPS counter, Keystrokes, Coords, Ping, CPS,
 * Armor HUD, Potion Timer, Cooldown Indicator, Crosshair, Hurt Indicator,
 * Held Item Info, Zoom overlay, etc.) implements.
 *
 * Split into tick() and render() deliberately (Instruction 7): tick() is
 * where any non-trivial computation happens (reading game state, computing
 * derived values like FPS averages), and render() must do nothing more
 * than read already-computed fields and issue draw calls. This keeps the
 * render path itself cheap regardless of how many modules are enabled.
 */
public interface HudModule {

	/** Stable id used as the config key (e.g. "fps", "keystrokes", "zoom"). */
	String getId();

	/** Human-readable name shown in the HUD Editor / config menu. */
	String getDisplayName();

	/**
	 * Called once per client tick (via ClientTickEvents.END_CLIENT_TICK),
	 * never from the render thread's per-frame path. Safe to do moderately
	 * expensive work here (it runs at most 20 times/sec, not up to hundreds
	 * of times/sec like rendering).
	 */
	void tick();

	/**
	 * Called once per frame from NexVior's single HUD canvas layer. Must
	 * only read cached state prepared in tick() and issue draw calls —
	 * no game-state queries, no allocation-heavy computation here.
	 */
	void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config);

	/**
	 * Rough width/height of the module's rendered content at its current
	 * state, used by the HUD Editor for drag-and-drop hit-testing and
	 * snapping guides. Does not need to be pixel-perfect.
	 */
	int getWidth();

	int getHeight();
}
