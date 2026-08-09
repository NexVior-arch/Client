package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import com.nexvior.client.hud.ModuleStyleRenderer;
import com.nexvior.client.hud.PositionResolver;
import com.nexvior.client.hud.TextColorRenderer;
import com.nexvior.client.pvp.InputPollHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CPS (clicks per second) counter for the left mouse button — the
 * conventional metric PvP players track. Counts rising edges (press
 * transitions) of InputPollHandler's left-button state, timestamps each
 * click, and reports how many fall within the trailing 1-second window.
 *
 * All click detection and windowing math happens in tick() (20/sec);
 * render() only formats the already-computed count.
 */
public class CpsModule implements HudModule {

	private static final String ID = "cps";
	private static final long WINDOW_NANOS = 1_000_000_000L;

	private boolean wasLeftDownLastTick = false;
	private final Deque<Long> clickTimestamps = new ArrayDeque<>();
	private volatile int displayedCps = 0;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "CPS Counter";
	}

	@Override
	public void tick() {
		boolean leftDown = InputPollHandler.isLeftDown();

		if (leftDown && !wasLeftDownLastTick) {
			// Rising edge: a new click.
			clickTimestamps.addLast(System.nanoTime());
		}
		wasLeftDownLastTick = leftDown;

		long now = System.nanoTime();
		while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > WINDOW_NANOS) {
			clickTimestamps.pollFirst();
		}

		displayedCps = clickTimestamps.size();
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		String text = displayedCps + " CPS";
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
		return (client == null || client.textRenderer == null) ? 50 : client.textRenderer.getWidth(displayedCps + " CPS");
	}

	@Override
	public int getHeight() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (client == null || client.textRenderer == null) ? 10 : client.textRenderer.fontHeight;
	}
}
