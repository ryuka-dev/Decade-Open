package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.other.GunTextureOverride;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

import java.util.function.BiFunction;

/**
 * Decade: a {@link GunTextureOverride} can also be handed over as an inter-mod message, so a mod that must not link
 * against TaCZ's classes can still register one. Two messages to {@code tacz}, each carrying a supplier of a plain
 * {@code BiFunction<ItemStack, ResourceLocation, ResourceLocation>} (the gun, the texture the display would use, the
 * texture to use instead or null): {@code gun_texture_override} for the full-detail model, and
 * {@code gun_lod_texture_override} for the low-detail one. The last message of each kind wins.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class GunTextureOverrideMessages {
    public static final String MODEL = "gun_texture_override";
    public static final String LOD = "gun_lod_texture_override";

    private GunTextureOverrideMessages() {
    }

    @SubscribeEvent
    public static void onInterModProcess(InterModProcessEvent event) {
        BiFunction<ItemStack, ResourceLocation, ResourceLocation>[] found = read();
        if (found[0] == null && found[1] == null) {
            return;
        }
        BiFunction<ItemStack, ResourceLocation, ResourceLocation> model = found[0];
        BiFunction<ItemStack, ResourceLocation, ResourceLocation> lod = found[1];
        GunTextureOverride.register((stack, original, isLod) -> {
            BiFunction<ItemStack, ResourceLocation, ResourceLocation> f = isLod ? lod : model;
            return f == null ? null : f.apply(stack, original);
        });
        GunMod.LOGGER.info("Gun texture override registered by inter-mod message (model: {}, lod: {})", model != null, lod != null);
    }

    @SuppressWarnings("unchecked")
    private static BiFunction<ItemStack, ResourceLocation, ResourceLocation>[] read() {
        BiFunction<ItemStack, ResourceLocation, ResourceLocation>[] out = new BiFunction[2];
        InterModComms.getMessages(GunMod.MOD_ID, m -> m.equals(MODEL) || m.equals(LOD)).forEach(message -> {
            Object payload = message.messageSupplier().get();
            if (payload instanceof BiFunction<?, ?, ?> f) {
                out[message.method().equals(MODEL) ? 0 : 1] = (BiFunction<ItemStack, ResourceLocation, ResourceLocation>) f;
            } else {
                GunMod.LOGGER.error("{} from {} is not a BiFunction, ignored", message.method(), message.senderModId());
            }
        });
        return out;
    }
}
