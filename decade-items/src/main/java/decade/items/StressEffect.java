package decade.items;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.List;
import java.util.function.Consumer;

/**
 * Stress: a harmful effect that does nothing by itself. A server mod gives it to a player who was just hit and reads it
 * to decide how natural healing goes while it lasts; how long it lasts is the server's to say. Milk does not clear it, and
 * its icon is drawn steady: a short effect would otherwise blink the whole time it shows, as vanilla blinks the last
 * seconds of every effect.
 */
final class StressEffect extends MobEffect {
    StressEffect(int colour) {
        super(MobEffectCategory.HARMFUL, colour);
    }

    /** Nothing cures it: not milk, not any item. */
    @Override
    public List<ItemStack> getCurativeItems() {
        return List.of();
    }

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            /** The top-right icon, fully opaque: returning true keeps vanilla's blinking one from being drawn. */
            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui, GuiGraphics graphics,
                                         int x, int y, float z, float alpha) {
                var sprite = net.minecraft.client.Minecraft.getInstance().getMobEffectTextures().get(instance.getEffect());
                graphics.blit(x + 3, y + 3, 0, 18, 18, sprite);
                return true;
            }
        });
    }
}
