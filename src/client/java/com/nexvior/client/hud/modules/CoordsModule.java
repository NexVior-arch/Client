package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import com.nexvior.client.hud.ModuleStyleRenderer;
import com.nexvior.client.hud.PositionResolver;
import com.nexvior.client.hud.TextColorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Coordinate display module (X / Y / Z). Purely informational: this is
 * the client's own player position, which the client already has locally
 * regardless of this mod (it is never hidden data), so this module just
 * formats existing local state for display. Not an ESP-class feature.
 */
public class CoordsModule implements HudModule {

	private static final String ID = "coords";

	// Cached each tick so render() only formats/draws a string already
	// computed, per Instruction 7 (no computation inside render()).
	private volatile String cachedText = "X: 0, Y: 0, Z: 0";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Coordinates";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client == null ? null : client.player;
		if (player == null) {
			return;
		}

		int x = (int) Math.floor(player.getX());
		int y = (int) Math.floor(player.getY());
		int z = (int) Math.floor(player.getZ());
		cachedText = "X: " + x + ", Y: " + y + ", Z: " + z;
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		String text = cachedText;
		int textWidth = client.textRenderer.getWidth(text);
		int textHeight = client.textRenderer.fontHeight;

		int x = PositionResolver.resolveX(config, textWidth + config.getStyle().getPadding() * 2);
		int y = PositionResolver.resolveY(config, textHeight + config.getStyle().getPadding() * 2);

		int[] origin = ModuleStyleRenderer.drawBackground(context, config.getStyle(), x, y, textWidth, textHeight);

		TextColorRenderer.draw(context, client.textRenderer, text, origin[0], origin[1], config.getStyle().getTextColor());
	}

	@Override
	public int getWidth() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (client == null || client.textRenderer == null) ? 90 : client.textRenderer.getWidth(cachedText);
	}

	@Override
	public int getHeight() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (client == null || client.textRenderer == null) ? 10 : client.textRenderer.fontHeight;
	}
}
