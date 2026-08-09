package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import com.nexvior.client.hud.ModuleStyleRenderer;
import com.nexvior.client.hud.PositionResolver;
import com.nexvior.client.hud.TextColorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Ping (connection latency) display module. Reads PlayerListEntry#getLatency()
 * for the client's own player, which is the same latency value already
 * shown in vanilla's player list (Tab menu) — this module just surfaces it
 * as a persistent HUD element instead of requiring the Tab key.
 */
public class PingModule implements HudModule {

	private static final String ID = "ping";

	private volatile String cachedText = "-- ms";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Ping";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null) {
			return;
		}

		ClientPlayNetworkHandler handler = client.getNetworkHandler();
		if (handler == null) {
			cachedText = "-- ms";
			return;
		}

		ClientPlayerEntity player = client.player;
		PlayerListEntry entry = handler.getPlayerListEntry(player.getUuid());
		if (entry == null) {
			cachedText = "-- ms";
			return;
		}

		cachedText = entry.getLatency() + " ms";
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
		return (client == null || client.textRenderer == null) ? 50 : client.textRenderer.getWidth(cachedText);
	}

	@Override
	public int getHeight() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (client == null || client.textRenderer == null) ? 10 : client.textRenderer.fontHeight;
	}
}
