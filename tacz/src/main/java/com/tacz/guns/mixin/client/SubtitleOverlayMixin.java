package com.tacz.guns.mixin.client;

import com.tacz.guns.client.gui.overlay.WeaponSlotHudOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Decade: subtitles move up out of the way of the weapon slot panel, which sits in the same
 * bottom-right corner (README-DECADE.md). Vanilla puts the lowest subtitle line 35 above the bottom
 * of the screen, 5 each way, and stacks the rest upwards; the whole block moves up just enough to
 * leave {@link #tacz$GAP} above the panel, and stays where it was while the panel is not drawn.
 */
@Mixin(SubtitleOverlay.class)
public abstract class SubtitleOverlayMixin {
    /** How far up from the bottom of the screen vanilla's lowest subtitle line reaches down to. */
    @Unique
    private static final int tacz$VANILLA_BOTTOM = 30;
    @Unique
    private static final int tacz$GAP = 4;

    @Unique
    private boolean tacz$moved;

    @Inject(method = "render", at = @At("HEAD"))
    private void tacz$moveAbovePanel(GuiGraphics graphics, CallbackInfo ci) {
        int shift = WeaponSlotHudOverlay.heightFromBottom(Minecraft.getInstance()) + tacz$GAP - tacz$VANILLA_BOTTOM;
        tacz$moved = shift > 0;
        if (tacz$moved) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, -shift, 0);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void tacz$restore(GuiGraphics graphics, CallbackInfo ci) {
        if (tacz$moved) {
            graphics.pose().popPose();
            tacz$moved = false;
        }
    }
}
