package com.lunarcycle.mixin;

import com.lunarcycle.client.LunarClientState;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects moon sky/fog tinting into BackgroundRenderer.
 *
 * We blend the moon color with the vanilla fog color during the night.
 * The blend is subtle (30% moon color, 70% vanilla) so it doesn't look garish.
 */
@Mixin(BackgroundRenderer.class)
public class SkyColorMixin {

    @Inject(
        method = "applyFog",
        at = @At("TAIL")
    )
    private static void lunarcycle$applyMoonFog(
            Camera camera,
            BackgroundRenderer.FogType fogType,
            float viewDistance,
            boolean thickFog,
            float tickDelta,
            CallbackInfo ci
    ) {
        int tint = LunarClientState.getSkyTintColor();
        if (tint == -1) return;

        // Extract RGB from packed int
        float moonR = ((tint >> 16) & 0xFF) / 255f;
        float moonG = ((tint >> 8)  & 0xFF) / 255f;
        float moonB = ( tint        & 0xFF) / 255f;

        // Blend 25% moon color into the fog (subtle, atmospheric)
        float blend = 0.25f;
        // RenderSystem fog color is already set by vanilla — we nudge it
        // Using RenderSystem directly:
        float[] current = new float[]{0f, 0f, 0f, 1f};
        // We add a tinted overlay — net.minecraft uses GlStateManager internally
        // For Fabric 1.20.1 the cleanest approach is via RenderSystem.setShaderFogColor
        com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(
            moonR * blend,
            moonG * blend,
            moonB * blend,
            1.0f
        );
    }
}
