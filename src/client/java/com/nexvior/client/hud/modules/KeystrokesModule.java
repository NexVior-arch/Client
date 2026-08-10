package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import com.nexvior.client.hud.ModuleStyleRenderer;
import com.nexvior.client.hud.PositionResolver;
import com.nexvior.client.pvp.InputPollHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Keystrokes HUD: a small WASD grid plus left/right mouse button
 * indicators, each highlighting when pressed. A staple of PvP-focused
 * clients (used to verify one's own inputs during clips/streams, or just
 * as a movement-feel sanity check).
 *
 * WASD state reads the user's actual configured KeyBindings
 * (GameOptions.forwardKey/backKey/leftKey/rightKey) rather than hardcoded
 * physical W/A/S/D key codes, so this correctly reflects a remapped
 * control scheme. Mouse button state comes from InputPollHandler (GLFW
 * polling), consistent with the CPS module.
 */
public class KeystrokesModule implements HudModule {

	private static final String ID = "keystrokes";

	private static final int KEY_SIZE = 18;
	private static final int GAP = 2;

	// Cached each tick; render() only reads these booleans.
	private volatile boolean forward, back, left, right;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Keystrokes";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options == null) {
			return;
		}

		forward = isDown(client.options.forwardKey);
		back = isDown(client.options.backKey);
		left = isDown(client.options.leftKey);
		right = isDown(client.options.rightKey);
	}

	private boolean isDown(KeyBinding binding) {
		// isPressed() reflects true "is the key currently held" state
		// (distinct from wasPressed()'s edge-triggered consumption), which
		// is exactly what a keystrokes display needs to show held keys.
		return binding != null && binding.isPressed();
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		int gridWidth = KEY_SIZE * 3 + GAP * 2;
		int gridHeight = KEY_SIZE * 2 + GAP;

		int x = PositionResolver.resolveX(config, gridWidth + config.getStyle().getPadding() * 2);
		int y = PositionResolver.resolveY(config, gridHeight + config.getStyle().getPadding() * 2);

		int[] origin = ModuleStyleRenderer.drawBackground(context, config.getStyle(), x, y, gridWidth, gridHeight);
		int gx = origin[0];
		int gy = origin[1];

		int accentColor = config.getStyle().getTextColor().resolveRgb() | 0xFF000000;
		int idleColor = 0x40FFFFFF;
		int activeBoxColor = accentColor;
		int idleBoxColor = 0x30FFFFFF;

		// Row 1: W centered above A S D.
		drawKey(context, client, gx + KEY_SIZE + GAP, gy, "W", forward, activeBoxColor, idleBoxColor, accentColor);

		// Row 2: A S D, plus mouse buttons appended to the right.
		int row2Y = gy + KEY_SIZE + GAP;
		drawKey(context, client, gx, row2Y, "A", left, activeBoxColor, idleBoxColor, accentColor);
		drawKey(context, client, gx + KEY_SIZE + GAP, row2Y, "S", back, activeBoxColor, idleBoxColor, accentColor);
		drawKey(context, client, gx + (KEY_SIZE + GAP) * 2, row2Y, "D", right, activeBoxColor, idleBoxColor, accentColor);

		// Mouse buttons, drawn as a small pair below the grid.
		boolean leftMouse = InputPollHandler.isLeftDown();
		boolean rightMouse = InputPollHandler.isRightDown();
		int mouseY = row2Y + KEY_SIZE + GAP;
		drawKey(context, client, gx, mouseY, "L", leftMouse, activeBoxColor, idleBoxColor, accentColor);
		drawKey(context, client, gx + KEY_SIZE + GAP, mouseY, "R", rightMouse, activeBoxColor, idleBoxColor, accentColor);
	}

	private void drawKey(DrawContext context, MinecraftClient client, int x, int y, String label,
						  boolean active, int activeColor, int idleColor, int textColor) {
		int boxColor = active ? activeColor : idleColor;
		context.fill(x, y, x + KEY_SIZE, y + KEY_SIZE, boxColor);
		ModuleStyleRenderer.drawBorder(context, x, y, KEY_SIZE, KEY_SIZE, 0x60FFFFFF);

		int textWidth = client.textRenderer.getWidth(label);
		int textX = x + (KEY_SIZE - textWidth) / 2;
		int textY = y + (KEY_SIZE - client.textRenderer.fontHeight) / 2;
		context.drawTextWithShadow(client.textRenderer, label, textX, textY, active ? 0xFF000000 : textColor);
	}

	@Override
	public int getWidth() {
		return KEY_SIZE * 3 + GAP * 2;
	}

	@Override
	public int getHeight() {
		// 3 rows: WASD (2 rows) + mouse button row.
		return KEY_SIZE * 3 + GAP * 2;
	}
}
