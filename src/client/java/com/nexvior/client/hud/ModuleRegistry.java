package com.nexvior.client.hud;

import com.nexvior.client.hud.modules.ArmorHudModule;
import com.nexvior.client.hud.modules.CooldownIndicatorModule;
import com.nexvior.client.hud.modules.CoordsModule;
import com.nexvior.client.hud.modules.CpsModule;
import com.nexvior.client.hud.modules.CrosshairModule;
import com.nexvior.client.hud.modules.FpsModule;
import com.nexvior.client.hud.modules.HeldItemInfoModule;
import com.nexvior.client.hud.modules.HurtIndicatorModule;
import com.nexvior.client.hud.modules.KeystrokesModule;
import com.nexvior.client.hud.modules.PingModule;
import com.nexvior.client.hud.modules.PotionTimerModule;

/**
 * Central place where every HUD module gets registered with HudRenderer.
 * As each module is implemented in this stage, it gets one line added
 * here — this is the only file that needs touching to add a new module
 * to the mod (config defaults are handled automatically by
 * ConfigManager.getModule()'s lazy-creation, per Stage 2's design).
 */
public final class ModuleRegistry {

	private ModuleRegistry() {
	}

	public static void registerAll() {
		HudRenderer.registerModule(new FpsModule());
		HudRenderer.registerModule(new CoordsModule());
		HudRenderer.registerModule(new PingModule());
		HudRenderer.registerModule(new CpsModule());
		HudRenderer.registerModule(new KeystrokesModule());
		HudRenderer.registerModule(new ArmorHudModule());
		HudRenderer.registerModule(new PotionTimerModule());
		HudRenderer.registerModule(new CooldownIndicatorModule());
		HudRenderer.registerModule(new CrosshairModule());
		HudRenderer.registerModule(new HurtIndicatorModule());
		HudRenderer.registerModule(new HeldItemInfoModule());
		// All 11 planned modules now registered. Zoom was intentionally
		// skipped (user has a separate mod for it).
	}
}
