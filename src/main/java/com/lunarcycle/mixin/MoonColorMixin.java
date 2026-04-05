package com.lunarcycle.mixin;

import com.lunarcycle.client.LunarClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tints the moon disc color based on the active special moon.
 *
 * Minecraft renders: stars → sun (setShaderTexture ordinal 0)
 *                         → moon (setShaderTexture ordinal 1)
 *
 * We set RenderSystem.setShaderColor before the moon texture is bound,
 * which tints the moon quad that follows. We reset to white at the end
 * of renderSky so nothing else is affected.
 *
 * require = 0 on both injections: if Minecraft's internals change and
 * the injection fails, the game still starts normally (moon just stays white).
 */
@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class MoonColorMixin {

    /**
     * Called just before the moon texture is bound — sets shader color
     * to the active moon's RGB tint.
     */
    @Inject(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V",
            ordinal = 1  // ordinal 0 = sun texture, ordinal 1 = moon texture
        ),
        require = 0
    )
    private void lunarcycle$tintMoon(CallbackInfo ci) {
        int tint = LunarClientState.getSkyTintColor();
        if (tint == -1) return; // NORMAL night or daytime — no tint

        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8)  & 0xFF) / 255f;
        float b = ( tint        & 0xFF) / 255f;

        // Slightly brighter than the fog tint so the moon disc is clearly visible
        float boost = 1.4f;
        RenderSystem.setShaderColor(
            Math.min(1f, r * boost),
            Math.min(1f, g * boost),
            Math.min(1f, b * boost),
            1.0f
        );
    }

    /**
     * Reset shader color to white at the end of sky rendering
     * so nothing drawn afterwards (clouds, entities, etc.) is tinted.
     */
    @Inject(
        method = "renderSky",
        at = @At("TAIL"),
        require = 0
    )
    private void lunarcycle$resetMoonColor(CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
