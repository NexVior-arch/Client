package com.nexvior.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.nexvior.client.NexViorClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads, holds, and persists NexVior's config.
 *
 * Fault tolerance (Instruction 7 — "Fault-tolerant config"):
 *   - A missing config file is not an error: defaults are used and the
 *     file is created on first save.
 *   - A corrupted/malformed config file (JsonSyntaxException) is caught
 *     specifically, logged as a warning (not an error — this is an
 *     expected, recoverable condition, not a bug), and defaults are used
 *     for that session. The corrupted file is backed up alongside the
 *     new one rather than silently overwritten, so the user doesn't lose
 *     the ability to recover/inspect it.
 *   - An IOException (permissions, disk issues, etc.) during load or save
 *     is caught separately and never propagates — NexVior continues
 *     running with an in-memory config either way.
 *
 * This class deliberately does NOT throw checked or unchecked exceptions
 * out of load()/save() under any file-system or JSON-parsing condition.
 */
public final class ConfigManager {

	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.create();

	private static final String CONFIG_FILE_NAME = "nexvior.json";

	private static NexViorConfig config;

	private ConfigManager() {
	}

	/**
	 * Loads the config from disk, or falls back to defaults on any
	 * failure. Safe to call multiple times (e.g. from a "reload config"
	 * button) — always leaves `config` in a valid, non-null state.
	 */
	public static synchronized NexViorConfig load() {
		Path path = getConfigPath();

		if (!Files.exists(path)) {
			NexViorClient.LOGGER.info("[NexVior] No config file found at {}, generating defaults.", path);
			config = new NexViorConfig();
			save();
			return config;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			NexViorConfig loaded = GSON.fromJson(reader, NexViorConfig.class);
			if (loaded == null) {
				// Valid JSON "null" or an empty file — treat the same as
				// missing, rather than assigning a null config.
				NexViorClient.LOGGER.warn(
					"[NexVior] Config file at {} parsed to null (likely empty). Using defaults.", path
				);
				config = new NexViorConfig();
			} else {
				config = loaded;
				NexViorClient.LOGGER.info("[NexVior] Config loaded successfully from {}.", path);
			}
		} catch (JsonSyntaxException e) {
			NexViorClient.LOGGER.warn(
				"[NexVior] Config file at {} is corrupted or malformed JSON. " +
				"Regenerating safe defaults and backing up the corrupted file. Cause: {}",
				path, e.getMessage()
			);
			backupCorruptedFile(path);
			config = new NexViorConfig();
			save();
		} catch (IOException e) {
			NexViorClient.LOGGER.warn(
				"[NexVior] Could not read config file at {} (I/O error). Using in-memory defaults " +
				"for this session; NexVior will keep retrying to save on changes. Cause: {}",
				path, e.getMessage()
			);
			config = new NexViorConfig();
		} catch (Throwable t) {
			// Final catch-all: no matter what goes wrong here (including
			// errors we didn't anticipate), NexVior must never crash the
			// game at startup because of a config problem.
			NexViorClient.LOGGER.error(
				"[NexVior] Unexpected error loading config. Using in-memory defaults for this session.", t
			);
			config = new NexViorConfig();
		}

		return config;
	}

	/**
	 * Saves the current in-memory config to disk. Failures are logged and
	 * swallowed — a failed save never crashes or interrupts gameplay, it
	 * just means settings won't persist until the underlying issue (e.g.
	 * disk full, permissions) is resolved.
	 */
	public static synchronized void save() {
		if (config == null) {
			// Nothing to save yet (save() called before load()) — no-op
			// rather than writing an empty/garbage file.
			return;
		}

		Path path = getConfigPath();
		Path tmpPath = path.resolveSibling(CONFIG_FILE_NAME + ".tmp");

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(tmpPath, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}

			// Atomic-ish move: write to a temp file first, then move into
			// place. This avoids leaving a half-written, corrupted config
			// file behind if the game is killed mid-save.
			Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			NexViorClient.LOGGER.warn(
				"[NexVior] Failed to save config to {}. Settings will not persist until this is " +
				"resolved (check disk space / folder permissions). Cause: {}",
				path, e.getMessage()
			);
		} catch (Throwable t) {
			NexViorClient.LOGGER.error("[NexVior] Unexpected error saving config.", t);
		}
	}

	/**
	 * Returns the current config, loading defaults first if load() hasn't
	 * been called yet this session. Never returns null.
	 */
	public static synchronized NexViorConfig get() {
		if (config == null) {
			return load();
		}
		return config;
	}

	/**
	 * Retrieves (lazily creating, if absent) the ModuleConfig for the given
	 * module id. This is the mechanism that lets new HUD modules be added
	 * in later stages without any config migration step — an id that
	 * doesn't exist yet in an older saved config simply gets sane defaults.
	 */
	public static synchronized ModuleConfig getModule(String moduleId) {
		return get().getModules().computeIfAbsent(moduleId, id -> new ModuleConfig());
	}

	private static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}

	/**
	 * Copies a corrupted config file to a timestamped backup path before
	 * it gets overwritten by fresh defaults, so a technically-inclined
	 * user (or NexVior support) can inspect what went wrong.
	 */
	private static void backupCorruptedFile(Path path) {
		try {
			Path backup = path.resolveSibling(
				CONFIG_FILE_NAME + ".corrupted-" + System.currentTimeMillis() + ".bak"
			);
			Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
			NexViorClient.LOGGER.info("[NexVior] Backed up corrupted config to {}.", backup);
		} catch (IOException e) {
			// Backing up is a best-effort courtesy, not a critical path —
			// if even this fails, just log it and move on.
			NexViorClient.LOGGER.warn("[NexVior] Could not back up corrupted config file.", e);
		}
	}
}
