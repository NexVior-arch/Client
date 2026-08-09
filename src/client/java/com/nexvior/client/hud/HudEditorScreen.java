package com.nexvior.client.hud;

import com.nexvior.client.config.ColorMode;
import com.nexvior.client.config.ConfigManager;
import com.nexvior.client.config.ModuleConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * The in-game HUD Editor: lets the user freely drag any enabled HUD module
 * to any screen position (not restricted to fixed slots), with optional
 * snapping guides to screen edges/center.
 *
 * Opened via keybind (wired in a later stage) or from the mod menu config
 * screen. This screen renders NexVior's actual HUD modules live (using the
 * same HudModule.render() each module uses in-game) so the user sees a
 * true preview while editing, not a mockup.
 */
public class HudEditorScreen extends Screen {

	private static final int SNAP_THRESHOLD_PX = 6;
	private static final int COLOR_SWATCH_SIZE = 14;

	private final Screen parent;

	/** Module currently being dragged, or null if nothing is being dragged. */
	private HudModule draggingModule;
	private int dragOffsetX;
	private int dragOffsetY;

	/** Cached per-frame snap guide lines to draw (null = none this frame). */
	private Integer snapGuideX;
	private Integer snapGuideY;

	public HudEditorScreen(Screen parent) {
		super(Text.literal("NexVior — HUD Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.addDrawableChild(
			ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
				.dimensions(this.width / 2 - 50, this.height - 30, 100, 20)
				.build()
		);

		this.addDrawableChild(
			ButtonWidget.builder(Text.literal("Reset Positions"), btn -> resetAllPositions())
				.dimensions(this.width / 2 - 160, this.height - 30, 100, 20)
				.build()
		);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(
			this.textRenderer, "NexVior HUD Editor — drag modules to reposition",
			this.width / 2, 12, 0xFFFFFFFF
		);
		context.drawCenteredTextWithShadow(
			this.textRenderer, "Click a module's color swatch to change its color",
			this.width / 2, 24, 0xFFAAAAAA
		);

		// Render every registered module at its configured (or live-drag)
		// position, using the SAME render() each module uses during normal
		// gameplay, so the editor is a truthful live preview.
		for (HudModule module : HudRenderer.getModules().values()) {
			ModuleConfig moduleConfig = ConfigManager.getModule(module.getId());

			int x = resolveX(module, moduleConfig);
			int y = resolveY(module, moduleConfig);
			int w = Math.max(module.getWidth(), 20);
			int h = Math.max(module.getHeight(), 10);

			try {
				module.render(context, dummyTickCounter(), moduleConfig);
			} catch (Throwable t) {
				// Same graceful-degradation contract as HudRenderer: a
				// broken module must not break the editor screen either.
				context.drawText(
					this.textRenderer,
					"[" + module.getDisplayName() + " failed to render]",
					x, y, 0xFFFF5555, false
				);
			}

			// Draw a subtle bounding box + color swatch as editor-only
			// affordances (never shown during normal gameplay).
			boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
			int boxColor = hovered ? 0x80FFFFFF : 0x40FFFFFF;
			context.drawBorder(x - 2, y - 2, w + 4, h + 4, boxColor);

			drawColorSwatch(context, moduleConfig, x + w + 6, y);
		}

		if (snapGuideX != null) {
			context.fill(snapGuideX, 0, snapGuideX + 1, this.height, 0x80FFFFFF);
		}
		if (snapGuideY != null) {
			context.fill(0, snapGuideY, this.width, snapGuideY + 1, 0x80FFFFFF);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (HudModule module : HudRenderer.getModules().values()) {
				ModuleConfig moduleConfig = ConfigManager.getModule(module.getId());
				int x = resolveX(module, moduleConfig);
				int y = resolveY(module, moduleConfig);
				int w = Math.max(module.getWidth(), 20);
				int h = Math.max(module.getHeight(), 10);

				// Color swatch click takes priority over drag-start.
				int swatchX = x + w + 6;
				if (mouseX >= swatchX && mouseX <= swatchX + COLOR_SWATCH_SIZE
					&& mouseY >= y && mouseY <= y + COLOR_SWATCH_SIZE) {
					cycleModuleColor(moduleConfig);
					return true;
				}

				if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
					draggingModule = module;
					dragOffsetX = (int) mouseX - x;
					dragOffsetY = (int) mouseY - y;
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (draggingModule != null) {
			ModuleConfig moduleConfig = ConfigManager.getModule(draggingModule.getId());

			int rawX = (int) mouseX - dragOffsetX;
			int rawY = (int) mouseY - dragOffsetY;

			int[] snapped = applySnapping(rawX, rawY, draggingModule);

			// Store back in the module's configured anchor space so the
			// anchor (e.g. bottom-right) stays meaningful across window
			// resizes, rather than always writing top-left-relative values.
			writeResolvedPosition(moduleConfig, draggingModule, snapped[0], snapped[1]);

			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggingModule != null) {
			draggingModule = null;
			snapGuideX = null;
			snapGuideY = null;
			// Persist immediately on drop rather than waiting for shutdown,
			// so a crash/alt-F4 after editing doesn't lose position changes.
			ConfigManager.save();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void close() {
		ConfigManager.save();
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}

	// --- Helpers -----------------------------------------------------

	private int resolveX(HudModule module, ModuleConfig config) {
		return PositionResolver.resolveX(config, module.getWidth());
	}

	private int resolveY(HudModule module, ModuleConfig config) {
		return PositionResolver.resolveY(config, module.getHeight());
	}

	/**
	 * Converts a screen-space (top-left relative) drop position back into
	 * the module's currently configured anchor space, preserving whichever
	 * corner the module was already anchored to.
	 */
	private void writeResolvedPosition(ModuleConfig config, HudModule module, int screenX, int screenY) {
		switch (config.getAnchor()) {
			case TOP_LEFT -> {
				config.setX(screenX);
				config.setY(screenY);
			}
			case TOP_RIGHT -> {
				config.setX(this.width - screenX - module.getWidth());
				config.setY(screenY);
			}
			case BOTTOM_LEFT -> {
				config.setX(screenX);
				config.setY(this.height - screenY - module.getHeight());
			}
			case BOTTOM_RIGHT -> {
				config.setX(this.width - screenX - module.getWidth());
				config.setY(this.height - screenY - module.getHeight());
			}
		}
	}

	/**
	 * Snaps a dragged position to the screen's center lines within
	 * SNAP_THRESHOLD_PX, purely as a visual placement aid. Also updates
	 * snapGuideX/Y so render() can draw the guide line. Also clamps fully
	 * off-screen positions back on-screen.
	 */
	private int[] applySnapping(int x, int y, HudModule module) {
		snapGuideX = null;
		snapGuideY = null;

		int w = Math.max(module.getWidth(), 20);
		int h = Math.max(module.getHeight(), 10);
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		int moduleCenterX = x + w / 2;
		int moduleCenterY = y + h / 2;

		if (Math.abs(moduleCenterX - centerX) <= SNAP_THRESHOLD_PX) {
			x = centerX - w / 2;
			snapGuideX = centerX;
		}
		if (Math.abs(moduleCenterY - centerY) <= SNAP_THRESHOLD_PX) {
			y = centerY - h / 2;
			snapGuideY = centerY;
		}

		// Clamp so a dragged module can never end up fully off-screen —
		// this also protects against a corrupted/edited config producing
		// an unreachable off-screen module later.
		x = Math.max(0, Math.min(x, this.width - w));
		y = Math.max(0, Math.min(y, this.height - h));

		return new int[] { x, y };
	}

	private void drawColorSwatch(DrawContext context, ModuleConfig config, int x, int y) {
		int color = config.getColor().resolveRgb() | 0xFF000000;
		context.fill(x, y, x + COLOR_SWATCH_SIZE, y + COLOR_SWATCH_SIZE, color);
		context.drawBorder(x, y, COLOR_SWATCH_SIZE, COLOR_SWATCH_SIZE, 0xFFFFFFFF);
	}

	/**
	 * Cycles color mode/value on click as a lightweight in-editor control.
	 * A full HSV wheel widget is a natural follow-up enhancement; this
	 * gives immediate, crash-safe functionality now (solid presets +
	 * rainbow toggle) without introducing a new complex widget this stage.
	 */
	private void cycleModuleColor(ModuleConfig config) {
		int[] presets = {0xFFFFFF, 0x00FFAA, 0xFF3355, 0x3399FF, 0xFFAA00, 0xAA33FF};

		ColorMode currentMode = config.getColor().getMode();

		if (currentMode == ColorMode.SOLID) {
			int current = config.getColor().getSolidColor();
			int index = -1;
			for (int i = 0; i < presets.length; i++) {
				if (presets[i] == current) {
					index = i;
					break;
				}
			}

			if (index == presets.length - 1 || index == -1) {
				config.getColor().setMode(ColorMode.RAINBOW);
			} else {
				config.getColor().setSolidColor(presets[index + 1]);
			}
		} else if (currentMode == ColorMode.RAINBOW) {
			config.getColor().setMode(ColorMode.GRADIENT);
			config.getColor().setSolidColor(0xFFFFFF);
			config.getColor().setGradientEndColor(0x00AAFF);
		} else {
			// GRADIENT -> back to SOLID, completing the cycle.
			config.getColor().setMode(ColorMode.SOLID);
			config.getColor().setSolidColor(presets[0]);
		}
	}

	private void resetAllPositions() {
		int offset = 10;
		for (ModuleConfig config : ConfigManager.get().getModules().values()) {
			config.setAnchor(ModuleConfig.Anchor.TOP_LEFT);
			config.setX(offset);
			config.setY(offset);
			offset += 12;
		}
		ConfigManager.save();
	}

	/**
	 * The editor calls module.render() outside of the normal per-frame HUD
	 * canvas, so it needs a RenderTickCounter to satisfy the interface.
	 * RenderTickCounter.ONE is a stable, pre-built constant (no partial-
	 * tick interpolation), which keeps the editor's live preview
	 * deterministic frame-to-frame.
	 */
	private RenderTickCounter dummyTickCounter() {
		return RenderTickCounter.ONE;
	}
}
