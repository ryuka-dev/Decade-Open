package cc.xypp.modernhud.client;

import cc.xypp.modernhud.ModernHud;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Decade: where the plate row's numbers come from when another mod supplies them, instead of vanilla
 * Absorption. Handed over as an inter-mod message so the supplying mod never links against this one:
 * one message {@code plates} to {@code modernhud}, carrying a supplier of a plain
 * {@code Supplier<float[]>} that returns, each frame, {@code {value, max, perPlate, pouch, pouchMax}} -
 * the plate points now, the most the wearer can carry, the points one plate holds, how many plates
 * the pouch still allows in this fight ({@code < 0} outside one) and how many it holds for one, and optionally a
 * sixth: how far the pouch's refill is, 0 to 1, or {@code < 0} when nothing is being refilled. It
 * returns {@code null} or a {@code max <= 0} when the player carries no plates. Without the message the widget reads Absorption as upstream.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PlateSource {
    public static final String MESSAGE = "plates";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Supplier<float[]> source;

    private PlateSource() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onInterModProcess(InterModProcessEvent event) {
        InterModComms.getMessages(ModernHud.MOD_ID, MESSAGE::equals).forEach(message -> {
            Object payload = message.messageSupplier().get();
            if (payload instanceof Supplier<?> s) {
                source = (Supplier<float[]>) s;
                LOGGER.info("Plate source registered by {}", message.senderModId());
            } else {
                LOGGER.error("{} from {} is not a Supplier, ignored", MESSAGE, message.senderModId());
            }
        });
    }

    /** True when another mod supplies the plates, so Absorption is not what the row shows. */
    public static boolean present() {
        return source != null;
    }

    /** This frame's {@code {value, max, perPlate, pouch, pouchMax}}, or {@code null} with no plates. */
    public static float[] read() {
        Supplier<float[]> s = source;
        if (s == null) {
            return null;
        }
        float[] v;
        try {
            v = s.get();
        } catch (RuntimeException e) {
            return null;
        }
        if (v == null || v.length < 5 || !(v[1] > 0) || !(v[2] > 0)) {
            return null;
        }
        return v;
    }
}
