package com.nexvior.client.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root config object, serialized as-is to nexvior.json.
 *
 * Modules are stored in a Map keyed by a stable string id (e.g. "fps",
 * "keystrokes", "coords", "ping", "cps", "zoom") rather than as fixed
 * fields. This means adding a new HUD module in a later stage never
 * requires a schema/version migration — ConfigManager.getModule() below
 * lazily creates a default entry the first time an unknown id is
 * requested, which also means an OLDER config file (missing a module
 * added in a later mod version) loads safely with sane defaults for the
 * new module rather than failing.
 */
public class NexViorConfig {

	/** Bumped only if a breaking, non-lazily-repairable schema change is made. */
	private int configVersion = 1;

	private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

	private FreeLookConfig freeLook = new FreeLookConfig();

	public int getConfigVersion() {
		return configVersion;
	}

	public Map<String, ModuleConfig> getModules() {
		if (modules == null) {
			// Defensive: a hand-edited config with "modules": null would
			// otherwise NPE on first access.
			modules = new LinkedHashMap<>();
		}
		return modules;
	}

	public FreeLookConfig getFreeLook() {
		if (freeLook == null) {
			freeLook = new FreeLookConfig();
		}
		return freeLook;
	}
}
