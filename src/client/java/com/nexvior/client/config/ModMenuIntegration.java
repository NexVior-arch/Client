package com.nexvior.client.config;

import com.nexvior.client.NexViorClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration entrypoint.
 *
 * IMPORTANT: This class is only ever loaded by Fabric Loader if Mod Menu
 * itself is present and declares this mod's "modmenu" entrypoint (see
 * fabric.mod.json). If Mod Menu is absent, this class is never
 * instantiated — Fabric Loader does not even attempt to resolve it.
 *
 * Risk note: Mod Menu 17.0.0-beta.2 (the version verified as supporting
 * 1.21.11) is a BETA release. Its API surface may change before a
 * stable release. The screen factory below is wrapped so that if the
 * NexVior config screen fails to construct for any reason, Mod Menu
 * shows Minecraft's default "no config" state rather than crashing.
 */
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			try {
				return new com.nexvior.client.hud.HudEditorScreen(parent);
			} catch (Throwable t) {
				NexViorClient.LOGGER.error(
					"[NexVior] Failed to open config screen via Mod Menu integration.", t
				);
				return parent;
			}
		};
	}
}
