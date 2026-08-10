package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import com.nexvior.client.hud.ModuleStyleRenderer;
import com.nexvior.client.hud.PositionResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;

/**
 * Item Cooldown Indicator: shows a progress bar/overlay for the
 * currently-held item's cooldown (e.g. ender pearl, totem-adjacent
 * items, chorus fruit) whenever it is on cooldown. This mirrors the
 * greyscale sweep vanilla already draws over the hotbar slot — NexVior
 * just adds a clearer, larger, repositionable version of the same
 * server-driven cooldown state.
 *
 * Reads ItemCooldownManager#isCoolingDown(ItemStack)/getCooldownProgress
 * (ItemStack, float), the ItemStack-based overload introduced around
 * 1.21.5 and current as of 1.21.11 — NOT the older Item-based overload
 * from earlier versions.
 */
public class CooldownIndicatorModule implements HudModule {

	private static final String ID = "cooldown_indicator";
	private static final int BAR_WIDTH = 60;
	private static final int BAR_HEIGHT = 6;

	private volatile boolean onCooldown = false;
	private volatile float progress = 0f;
	private volatile ItemStack cachedStack = ItemStack.EMPTY;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Item Cooldown Indicator";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client == null ? null : client.player;
		if (player == null) {
			onCooldown = false;
			return;
		}

		ItemStack mainHand = player.getMainHandStack();
		if (mainHand == null || mainHand.isEmpty()) {
			onCooldown = false;
			return;
		}

		ItemCooldownManager cooldownManager = player.getItemCooldownManager();
		if (cooldownManager == null) {
			onCooldown = false;
			return;
		}

		boolean cooling = cooldownManager.isCoolingDown(mainHand);
		onCooldown = cooling;
		if (cooling) {
			// getCooldownProgress returns 1.0 at the start of a cooldown,
			// decreasing to 0.0 as it finishes — matches vanilla's own
			// hotbar overlay sweep direction.
			progress = cooldownManager.getCooldownProgress(mainHand, 1f);
			cachedStack = mainHand;
		}
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		if (!onCooldown) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		int contentWidth = BAR_WIDTH;
		int contentHeight = BAR_HEIGHT;

		int x = PositionResolver.resolveX(config, contentWidth + config.getStyle().getPadding() * 2);
		int y = PositionResolver.resolveY(config, contentHeight + config.getStyle().getPadding() * 2);

		int[] origin = ModuleStyleRenderer.drawBackground(context, config.getStyle(), x, y, contentWidth, contentHeight);
		int barX = origin[0];
		int barY = origin[1];

		context.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0x60000000);

		int filled = Math.round(BAR_WIDTH * progress);
		int accentColor = config.getStyle().getTextColor().resolveRgb() | 0xFF000000;
		context.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, accentColor);
	}

	@Override
	public int getWidth() {
		return BAR_WIDTH;
	}

	@Override
	public int getHeight() {
		return BAR_HEIGHT;
	}
}
