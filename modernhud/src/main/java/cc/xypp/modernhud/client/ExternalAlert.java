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
 * Decade: a centre-screen alert another mod raises, drawn by {@link AmmoAlertOverlay} in the style of "NO AMMO" and
 * ahead of the ammo alerts (for instance: a plate refused because the pouch is spent for this fight). One message
 * {@code alert} to {@code modernhud}, carrying a supplier of a plain {@code Supplier<String>} read every frame: the
 * words to show now, or {@code null} for none. The supplying mod decides the words and how long they stay.
 */
@Mod.EventBusSubscriber(modid = ModernHud.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExternalAlert {
    public static final String MESSAGE = "alert";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Supplier<String> source;

    private ExternalAlert() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onInterModProcess(InterModProcessEvent event) {
        InterModComms.getMessages(ModernHud.MOD_ID, MESSAGE::equals).forEach(message -> {
            Object payload = message.messageSupplier().get();
            if (payload instanceof Supplier<?> s) {
                source = (Supplier<String>) s;
                LOGGER.info("Centre alert source registered by {}", message.senderModId());
            } else {
                LOGGER.error("{} from {} is not a Supplier, ignored", MESSAGE, message.senderModId());
            }
        });
    }

    /** The words to show this frame, or {@code null}. */
    public static String read() {
        Supplier<String> s = source;
        if (s == null) {
            return null;
        }
        try {
            String text = s.get();
            return text == null || text.isEmpty() ? null : text;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
