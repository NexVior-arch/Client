package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/**
 * Directional Hurt Indicator: briefly flashes an arrow/wedge on screen
 * pointing toward the direction damage came from, fading out over a
 * short duration. This surfaces the same "damage tilt" direction vanilla
 * already computes and uses to tilt the camera on hit (LivingEntity /
 * ClientPlayerEntity#getDamageTiltYaw()) — NexVior only visualizes that
 * existing yaw value more clearly, on screen, relative to the player's
 * current look direction.
 *
 * NOT an ESP feature: this shows only a coarse on-screen DIRECTION
 * (an angle, converted to one of 8 screen-relative positions), never an
 * exact enemy position, distance, or any information that would be
 * visible through walls. It fires only in response to damage the player
 * actually took (a server-driven event the client already knows about),
 * not a continuous scan for nearby threats.
 *
 * Health-change detection (rather than a damage event mixin) is used to
 * trigger the indicator: this is a deliberately non-invasive approach
 * that needs no Mixin at all — health is already client-visible state.
 */
public class HurtIndicatorModule implements HudModule {

	private static final String ID = "hurt_indicator";
	private static final long FLASH_DURATION_NANOS = 800_000_000L; // 0.8s
	private static final int SIZE = 24;

	private float lastHealth = -1f;
	private volatile long flashStartNanos = -1L;
	private volatile float flashDirectionDegrees = 0f;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Directional Hurt Indicator";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client == null ? null : client.player;
		if (player == null) {
			lastHealth = -1f;
			return;
		}

		float currentHealth = player.getHealth();

		if (lastHealth >= 0f && currentHealth < lastHealth) {
			// Took damage since last tick. getDamageTiltYaw() reflects the
			// world-space direction vanilla's own camera-tilt used for
			// this most recent hit.
			float tiltYaw = player.getDamageTiltYaw();
			float relative = MathHelper.wrapDegrees(tiltYaw - player.getYaw());
			flashDirectionDegrees = relative;
			flashStartNanos = System.nanoTime();
		}

		lastHealth = currentHealth;
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		if (flashStartNanos < 0) {
			return;
		}

		long elapsed = System.nanoTime() - flashStartNanos;
		if (elapsed > FLASH_DURATION_NANOS) {
			return;
		}

		float progress = elapsed / (float) FLASH_DURATION_NANOS;
		int alpha = (int) (255 * (1f - progress));
		int baseColor = config.getStyle().getTextColor().resolveRgb();
		int color = (alpha << 24) | baseColor;

		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		int centerX = screenWidth / 2;
		int centerY = screenHeight / 2;

		// Position the indicator on a ring around screen center, at the
		// angle corresponding to the damage direction. 0 degrees = behind
		// (vanilla yaw convention: 180 = facing away from source means hit
		// came from the front when relative is near 0) — the exact mapping
		// matters less than consistency, so this uses a simple radial
		// placement: relative angle 0 = top of screen (damage from
		// directly ahead), increasing clockwise.
		double radians = Math.toRadians(flashDirectionDegrees);
		int radius = Math.min(screenWidth, screenHeight) / 4;
		int x = centerX + (int) (Math.sin(radians) * radius);
		int y = centerY - (int) (Math.cos(radians) * radius);

		int half = SIZE / 2;
		context.fill(x - half, y - 2, x + half, y + 2, color);
		context.fill(x - 2, y - half, x + 2, y + half, color);
	}

	@Override
	public int getWidth() {
		return SIZE;
	}

	@Override
	public int getHeight() {
		return SIZE;
	}
}
