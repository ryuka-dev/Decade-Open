package com.tacz.guns.api.client.other;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Decade: lets another mod draw one particular gun with another texture, chosen from the stack (a finish that differs
 * from gun to gun). The gun renderer asks it for the model texture it is about to bind, the full-detail one and the
 * low-detail (LOD) one; with nothing registered, or when it answers null, the display's own texture is used.
 * Only the texture changes: the model, the animations, the sounds and the gun's data stay as the display has them.
 */
@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface GunTextureOverride {
    /**
     * @param stack    the gun being drawn
     * @param original the texture the gun's display would use
     * @param lod      whether this is the low-detail model's texture
     * @return the texture to bind instead, or null to keep {@code original}
     */
    @Nullable
    ResourceLocation texture(ItemStack stack, ResourceLocation original, boolean lod);

    /** The one override in force; registering again replaces it. */
    static void register(@Nullable GunTextureOverride override) {
        Holder.current = override;
    }

    /** What the renderer binds: the override's answer, or the original. An override that throws keeps the original. */
    static ResourceLocation resolve(ItemStack stack, ResourceLocation original, boolean lod) {
        GunTextureOverride override = Holder.current;
        if (override == null) {
            return original;
        }
        try {
            ResourceLocation texture = override.texture(stack, original, lod);
            return texture == null ? original : texture;
        } catch (RuntimeException e) {
            Holder.failed(e);
            return original;
        }
    }

    final class Holder {
        private static volatile GunTextureOverride current;
        private static boolean reported;

        private Holder() {
        }

        private static void failed(RuntimeException e) {
            if (!reported) {
                reported = true;
                com.tacz.guns.GunMod.LOGGER.error("Gun texture override failed; drawing the guns' own textures", e);
            }
        }
    }
}
