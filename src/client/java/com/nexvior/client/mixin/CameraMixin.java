package com.nexvior.client.mixin;

import com.nexvior.client.pvp.FreeLookHandler;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Free Look implementation: modifies the yaw/pitch PARAMETERS of
 * Camera#setRotation(float, float) at method entry, adding NexVior's
 * free-look offset on top of whatever vanilla was about to apply.
 *
 * Target: net.minecraft.client.render.Camera#setRotation(float, float)
 * Injection point: @ModifyVariable at HEAD, one handler per parameter
 * (ordinal 0 = yaw, ordinal 1 = pitch). This rewrites the parameter value
 * BEFORE the method body runs — it does not call setRotation again, so
 * there is no re-entrancy/recursion risk.
 *
 * Why Camera and not Entity/PlayerEntity: setRotation here only affects
 * what is fed into the render/view matrix for THIS FRAME's camera.
 * Camera.update() calls this every frame using the player's current
 * yaw/pitch as a starting point, but nothing here writes back to the
 * player entity. Overriding the value at this exact point means the
 * player's real rotation (and therefore whatever is sent to the server)
 * is completely untouched — Free Look is purely a render-time camera
 * effect, never a gameplay-state change.
 *
 * Compatibility: Camera is not a class Sodium, Lithium, or Iris mixin
 * into for chunk/entity culling or shader passes (those target
 * WorldRenderer, chunk builders, and shader pipeline hooks respectively).
 * This is a narrow, single-method @ModifyVariable with no shared
 * injection point, so conflict risk is low. If Camera.setRotation's
 * signature changes in a future Minecraft version, Mixin's apply step
 * fails at startup for this one mixin (logged, not fatal — see
 * nexvior.client.mixins.json "defaultRequire": 1 combined with per-mixin
 * try/catch around mixin initialization is handled at the Loom/Mixin
 * layer itself, which reports a clear error rather than corrupting game
 * state).
 */
@Mixin(Camera.class)
public class CameraMixin {

	@ModifyVariable(method = "setRotation(FF)V", at = @At("HEAD"), ordinal = 0)
	private float nexvior$modifyYaw(float yaw) {
		if (!FreeLookHandler.isActive()) {
			return yaw;
		}
		return yaw + FreeLookHandler.getYawOffset();
	}

	@ModifyVariable(method = "setRotation(FF)V", at = @At("HEAD"), ordinal = 1)
	private float nexvior$modifyPitch(float pitch) {
		if (!FreeLookHandler.isActive()) {
			return pitch;
		}
		return pitch + FreeLookHandler.getPitchOffset();
	}
}
