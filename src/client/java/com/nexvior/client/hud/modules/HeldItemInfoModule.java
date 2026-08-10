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
import net.minecraft.item.ItemStack;

/**
 * Held Item Info: shows the main-hand item's name and remaining
 * durability as a persistent HUD line. Purely informational — the same
 * data the vanilla hotbar tooltip briefly flashes when switching items,
 * kept visible instead of fading after ~2 seconds.
 */
public class HeldItemInfoModule implements HudModule {

	private static final String ID = "held_item_info";

	private volatile String cachedText = "";
	private volatile boolean hasItem = false;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Held Item Info";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client == null ? null : client.player;
		if (player == null) {
			hasItem = false;
			return;
		}

		ItemStack stack = player.getMainHandStack();
		if (stack == null || stack.isEmpty()) {
			hasItem = false;
			return;
		}

		hasItem = true;
		String name = stack.getName().getString();

		if (stack.isDamageable() && stack.getMaxDamage() > 0) {
			int remaining = stack.getMaxDamage() - stack.getDamage();
			cachedText = name + " (" + remaining + "/" + stack.getMaxDamage() + ")";
		} else {
			cachedText = name;
		}
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		if (!hasItem) {
			return;
		}

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
		return (client == null || client.textRenderer == null || cachedText.isEmpty())
			? 60 : client.textRenderer.getWidth(cachedText);
	}

	@Override
	public int getHeight() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (client == null || client.textRenderer == null) ? 10 : client.textRenderer.fontHeight;
	}
}
