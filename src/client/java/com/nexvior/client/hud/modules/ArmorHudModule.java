package com.nexvior.client.hud.modules;

import com.nexvior.client.config.ModuleConfig;
import com.nexvior.client.hud.HudModule;
import com.nexvior.client.hud.ModuleStyleRenderer;
import com.nexvior.client.hud.PositionResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Armor & Durability HUD: shows each equipped armor piece's item icon and
 * a durability readout, so PvP players don't need to open their inventory
 * mid-fight to check gear condition. Purely informational — reads the
 * client's own equipped armor stacks, data the client already has locally.
 */
public class ArmorHudModule implements HudModule {

	private static final String ID = "armor_hud";
	private static final EquipmentSlot[] SLOTS = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private static final int ICON_SIZE = 16;
	private static final int GAP = 2;

	// Cached each tick: one entry per non-empty armor slot, with
	// precomputed durability fraction so render() only draws.
	private final List<ArmorEntry> cachedEntries = new ArrayList<>();

	private record ArmorEntry(ItemStack stack, float durabilityFraction, boolean damageable) {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDisplayName() {
		return "Armor & Durability";
	}

	@Override
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client == null ? null : client.player;
		if (player == null) {
			cachedEntries.clear();
			return;
		}

		List<ArmorEntry> entries = new ArrayList<>(4);
		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = player.getEquippedStack(slot);
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			boolean damageable = stack.isDamageable();
			float fraction = 1f;
			if (damageable && stack.getMaxDamage() > 0) {
				fraction = 1f - ((float) stack.getDamage() / stack.getMaxDamage());
			}
			entries.add(new ArmorEntry(stack, fraction, damageable));
		}

		cachedEntries.clear();
		cachedEntries.addAll(entries);
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter, ModuleConfig config) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null || cachedEntries.isEmpty()) {
			return;
		}

		int count = cachedEntries.size();
		int contentWidth = count * ICON_SIZE + (count - 1) * GAP;
		int contentHeight = ICON_SIZE;

		int x = PositionResolver.resolveX(config, contentWidth + config.getStyle().getPadding() * 2);
		int y = PositionResolver.resolveY(config, contentHeight + config.getStyle().getPadding() * 2);

		int[] origin = ModuleStyleRenderer.drawBackground(context, config.getStyle(), x, y, contentWidth, contentHeight);
		int drawX = origin[0];
		int drawY = origin[1];

		for (ArmorEntry entry : cachedEntries) {
			context.drawItem(entry.stack(), drawX, drawY);

			if (entry.damageable()) {
				// Thin durability bar beneath the icon: green -> yellow ->
				// red as it depletes, matching vanilla's own item
				// durability bar color convention so it reads intuitively.
				int barWidth = ICON_SIZE - 2;
				int filled = Math.round(barWidth * entry.durabilityFraction());
				int barColor = durabilityColor(entry.durabilityFraction());

				context.fill(drawX + 1, drawY + ICON_SIZE - 2, drawX + 1 + barWidth, drawY + ICON_SIZE, 0x80000000);
				context.fill(drawX + 1, drawY + ICON_SIZE - 2, drawX + 1 + filled, drawY + ICON_SIZE, barColor | 0xFF000000);
			}

			drawX += ICON_SIZE + GAP;
		}
	}

	private int durabilityColor(float fraction) {
		if (fraction > 0.5f) {
			return 0x00FF55;
		} else if (fraction > 0.2f) {
			return 0xFFAA00;
		} else {
			return 0xFF3333;
		}
	}

	@Override
	public int getWidth() {
		int count = Math.max(cachedEntries.size(), 1);
		return count * ICON_SIZE + (count - 1) * GAP;
	}

	@Override
	public int getHeight() {
		return ICON_SIZE;
	}
}
