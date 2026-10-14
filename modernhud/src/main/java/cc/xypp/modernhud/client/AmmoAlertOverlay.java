package cc.xypp.modernhud.client;

import cc.xypp.modernhud.client.compat.TaczCompat;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import cc.xypp.modernhud.config.WeaponConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Pulsing centre-screen "RELOAD" / "NO AMMO" / "LOW AMMO" text — the same alert convention
 * TaczPresence uses (down to the sine-wave pulse and roughly matching thresholds), so the two feel
 * consistent to anyone who's used both. Deliberately its own overlay rather than part of
 * {@link WeaponHudOverlay}: unlike the fixed-position ammo panel, this is meant to sit near screen
 * centre where you're actually looking, not tucked in a corner.
 *
 * <p>Skipped for guns that read ammo straight from the inventory (no separate mag to run dry) and
 * while overheat-locked, matching TaczPresence's own gate.
 */
public class AmmoAlertOverlay implements IGuiOverlay {

    /** Decade: Gun Durability tags a jammed gun {@code Jammed}; read by name, none of its classes. */
    private static boolean jammed(ItemStack gun) {
        net.minecraft.nbt.CompoundTag tag = gun.getTag();
        return tag != null && tag.getBoolean("Jammed");
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!WeaponConfig.enabled || !WeaponConfig.showAmmoAlerts || !TaczCompat.loaded()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.hideGui) {
            return;
        }
        if (SuperbWarfareCompat.isInVehicle(player)) {
            return; // same reasoning as PlayerHudOverlay/WeaponHudOverlay - SBW owns this screen area
        }
        String text;
        int color;
        long pulsePeriodMs;

        // Decade: another mod's alert first, whatever is in hand (a plate refused for a spent pouch is in hand)
        String external = ExternalAlert.read();
        if (external != null) {
            draw(graphics, screenWidth, screenHeight, external, WeaponConfig.noAmmoAlertColor, 400L);
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !TaczCompat.isGun(held) || TaczCompat.usesInventoryAmmo(held)) {
            return;
        }

        int mag = TaczCompat.magAmmo(player, held);
        int cap = TaczCompat.magCapacity(held);
        int reserve = TaczCompat.reserveAmmo(player, held);

        if (jammed(held)) {
            // Decade: a jammed gun is cleared on its own (auto-unjam); this says why it is not firing.
            // The jam is Gun Durability's "Jammed" tag on the gun, read by name only.
            text = net.minecraft.client.resources.language.I18n.get("modernhud.hud.jammed");
            color = WeaponConfig.reloadAlertColor;
            pulsePeriodMs = 400L;
        } else if (mag <= 0) {
            if (reserve == TaczCompat.INFINITE || reserve > 0) {
                text = net.minecraft.client.resources.language.I18n.get("modernhud.hud.reload");
                color = WeaponConfig.reloadAlertColor;
                pulsePeriodMs = 400L;
            } else {
                text = net.minecraft.client.resources.language.I18n.get("modernhud.hud.no_ammo");
                color = WeaponConfig.noAmmoAlertColor;
                pulsePeriodMs = 400L;
            }
        } else if (cap > 0 && (100.0 * mag / cap) <= WeaponConfig.lowAmmoThresholdPercent) {
            text = net.minecraft.client.resources.language.I18n.get("modernhud.hud.low_ammo");
            color = WeaponConfig.lowAmmoAlertColor;
            pulsePeriodMs = 600L;
        } else {
            return;
        }
        draw(graphics, screenWidth, screenHeight, text, color, pulsePeriodMs);
    }

    private static void draw(GuiGraphics graphics, int screenWidth, int screenHeight, String text, int color, long pulsePeriodMs) {
        float alpha = 0.45f + 0.55f * 0.5f * (1.0f + (float) Math.sin(System.currentTimeMillis() / (double) pulsePeriodMs));
        int argb = ((int) (alpha * 255f) << 24) | (color & 0xFFFFFF);

        Font font = Minecraft.getInstance().font;
        float scale = (float) WeaponConfig.ammoAlertScale;
        int width = font.width(text);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        float x = (screenWidth / 2.0f) / scale - (width / 2.0f);
        float y = (screenHeight / 2.0f) / scale + (WeaponConfig.ammoAlertYOffset / scale);
        graphics.drawString(font, text, Math.round(x), Math.round(y), argb, true);
        pose.popPose();
    }
}
