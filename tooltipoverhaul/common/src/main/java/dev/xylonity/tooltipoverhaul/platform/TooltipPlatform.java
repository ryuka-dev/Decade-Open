package dev.xylonity.tooltipoverhaul.platform;

import dev.xylonity.tooltipoverhaul.compat.glitchcore.GlitchcoreTooltipProxy;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface TooltipPlatform {

    boolean isModLoaded(String modid);

    Path resolveConfigFile(String configFileName);
    Path getConfigPath();

    ItemStack getHoveredItem(GuiGraphics graphics, List<ClientTooltipComponent> components, int mouseX, int mouseY);

    Optional<String> getModDisplayName(String namespace);

    /**
     * Decade: an item's tooltip as the game itself builds it for the hovered tooltip (every mod's lines, and its image
     * component wherever the loader puts it), so a pinned tooltip shows exactly what the hovered one did. Upstream
     * rebuilt it from the text lines and kept only a bundle's image, losing e.g. TaCZ's whole gun block.
     */
    default List<ClientTooltipComponent> itemTooltip(ItemStack stack, Font font, int mouseX, int screenWidth, int screenHeight) {
        return dev.xylonity.tooltipoverhaul.client.util.TextUtils.getTooltipComponentsFrom(stack, font, screenWidth, 2.2f);
    }

    /**
     * Lets loader-specific tooltip event hooks contribute extra {@link ClientTooltipComponent}s before rendering
     */
    default List<ClientTooltipComponent> gatherTooltipComponents(GuiGraphics graphics, ItemStack stack, List<ClientTooltipComponent> components, Font font, int mouseX, int mouseY, int screenWidth, int screenHeight, ClientTooltipPositioner positioner) {
        if (isModLoaded("glitchcore")) {
            return GlitchcoreTooltipProxy.gather(graphics, stack, components, font, mouseX, mouseY, screenWidth, screenHeight, positioner);
        }

        return components;
    }

    /**
     * Replays the loader-specific pre-render tooltip event, which normally fires inside {@code renderTooltipInternal} but is
     * skipped since I cancel it on the GUI graphics mixin
     */
    default boolean fireRenderTooltipPre(GuiGraphics graphics, ItemStack stack, List<ClientTooltipComponent> components, Font font, int mouseX, int mouseY, int screenWidth, int screenHeight, ClientTooltipPositioner positioner) {
        return false;
    }

}
