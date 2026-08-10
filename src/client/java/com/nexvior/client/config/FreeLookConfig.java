package com.nexvior.client.config;

/**
 * Settings for the Free Look feature (camera rotates independently of
 * player body/movement direction — purely visual, server never sees a
 * different rotation than normal look input).
 */
public class FreeLookConfig {

	private boolean enabled = false;

	/** HOLD: look around while key is held, snaps back on release.
	 *  TOGGLE: press once to engage, press again to release. */
	private Mode mode = Mode.HOLD;

	public FreeLookConfig() {
		// Default no-arg constructor required for Gson deserialization.
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Mode getMode() {
		return mode == null ? Mode.HOLD : mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public enum Mode {
		HOLD,
		TOGGLE
	}
}
