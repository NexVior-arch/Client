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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Potion Effect Timer: lists the player's active status effects with
 * remaining duration, as a persistent HUD element instead of requiring
 * the inventory screen to check. Purely informational — active effects
 * and their remaining duration are data the client already has locally
 * for its own player (this is the same data vanilla's inventory screen
 * effect icons already display).
 *
 * Uses StatusEffectUtil.getDurationText(), the same formatter vanilla's
 * own UI uses, rather than a hand-rolled MM:SS formatter, so displayed
 * durations always match vanilla convention (including its handling of
 * very long/infinite durations).
 */
public class PotionTimerModule implements HudModule {

	private static final String ID = "potion_timer";

	private record EffectLine(Text name, Text duration) {
	}

	private final List<EffectLine> cachedLines = new ArrayList<>();

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Potion Effect Timer";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client == null ? null : client.player;

		cachedLines.clear();
		if (player == null) {
			return;
		}

		for (StatusEffectInstance instance : player.getStatusEffects()) {
			try {
				Text name = instance.getEffectType().value().getName();
				// getDurationText replaced the older durationToString as of
				// Yarn 1.21.4 (confirmed via javadoc + CI compile error).
				// tickRate is 1f for normal game speed (not affected by
				// /tick rate commands, which is an acceptable simplification
				// for a HUD display).
				Text duration = StatusEffectUtil.getDurationText(instance, 1f, 1f);
				cachedLines.add(new EffectLine(name, duration));
			} catch (Throwable t) {
				// A single malformed/unusual effect instance (e.g. from a
				// datapack-added effect with missing translation data)
				// should not prevent the rest of the list from showing.
			}
		}
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null || cachedLines.isEmpty()) {
			return;
		}

		int lineHeight = client.textRenderer.fontHeight + 2;
		int contentWidth = getWidth();
		int contentHeight = cachedLines.size() * lineHeight - 2;

		int x = PositionResolver.resolveX(config, contentWidth + config.getStyle().getPadding() * 2);
		int y = PositionResolver.resolveY(config, contentHeight + config.getStyle().getPadding() * 2);

		int[] origin = ModuleStyleRenderer.drawBackground(context, config.getStyle(), x, y, contentWidth, contentHeight);

		int lineY = origin[1];
		for (EffectLine line : cachedLines) {
			String text = line.name().getString() + " " + line.duration().getString();
			TextColorRenderer.draw(context, client.textRenderer, text, origin[0], lineY, config.getStyle().getTextColor());
			lineY += lineHeight;
		}
	}

	@Override
	public int getWidth() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null || cachedLines.isEmpty()) {
			return 80;
		}
		int max = 0;
		for (EffectLine line : cachedLines) {
			String text = line.name().getString() + " " + line.duration().getString();
			max = Math.max(max, client.textRenderer.getWidth(text));
		}
		return max;
	}

	@Override
	public int getHeight() {
		MinecraftClient client = MinecraftClient.getInstance();
		int lineHeight = (client == null || client.textRenderer == null) ? 10 : client.textRenderer.fontHeight + 2;
		return Math.max(cachedLines.size(), 1) * lineHeight - 2;
	}
}
