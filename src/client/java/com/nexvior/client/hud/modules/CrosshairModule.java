package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Crosshair Styling: draws a custom crosshair accent (a colored dot or
 * small ring) centered on screen, independently of vanilla's own
 * crosshair rather than replacing it.
 *
 * Design choice: this module does NOT call HudElementRegistry's
 * replaceElement() on vanilla's crosshair layer. Rationale:
 *   1. Replacing a vanilla element is a more invasive operation than
 *      adding a new independent layer, and increases the chance of
 *      visual conflict with resource packs or other mods that also
 *      touch the crosshair.
 *   2. addLast() (used here, same as every other NexVior module) has no
 *      dependency on an exact vanilla element identifier name, which
 *      keeps this module isolated from that API surface entirely.
 *   3. The result is additive: vanilla's crosshair still renders
 *      normally underneath/around it; NexVior draws a small centered
 *      accent that can optionally recolor when hovering a live target,
 *      similar to "dynamic crosshair" features in comparable clients.
 *
 * The "turns color when looking at an entity" behavior reads
 * MinecraftClient.crosshairTarget, which is the same hit-test result
 * vanilla itself already computes every frame for interaction purposes —
 * this module does not perform any additional raycast of its own.
 */
public class CrosshairModule implements HudModule {

	private static final String ID = "crosshair";
	private static final int SIZE = 6;

	private volatile boolean hoveringEntity = false;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Crosshair Styling";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			hoveringEntity = false;
			return;
		}

		HitResult target = client.crosshairTarget;
		hoveringEntity = target instanceof EntityHitResult entityHit
			&& entityHit.getEntity() instanceof Entity;
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		int centerX = screenWidth / 2;
		int centerY = screenHeight / 2;

		int baseColor = config.getStyle().getTextColor().resolveRgb() | 0xFF000000;
		// A distinct highlight color (not user-configurable in this pass)
		// signals "hovering a target", mirroring the convention several
		// comparable clients use for dynamic crosshairs.
		int color = hoveringEntity ? (0xFF3333 | 0xFF000000) : baseColor;

		// Small centered dot accent. Deliberately minimal per the brief's
		// "every HUD element should feel necessary" visual philosophy —
		// this is a subtle addition to vanilla's crosshair, not a bulky
		// replacement reticle.
		int half = SIZE / 2;
		context.fill(centerX - half, centerY - 1, centerX + half, centerY + 1, color);
		context.fill(centerX - 1, centerY - half, centerX + 1, centerY + half, color);
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
