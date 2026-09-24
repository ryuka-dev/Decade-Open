package com.tacz.guns.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.pojo.display.gun.AmmoCountStyle;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.restriction.ClientGunUseRestriction;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

/**
 * Decade: the bottom-right gun HUD once the server has said which hotbar slots guns work in
 * ({@link ClientGunUseRestriction}). One row per weapon slot, always in the same place, shown while
 * any of them holds a gun: the selected one bright with its magazine in large digits, the other dim.
 * Replaces {@link GunHudOverlay}, which steps aside while this one is in charge; without word from the
 * server nothing changes. The look is a placeholder. See README-DECADE.md.
 */
public class WeaponSlotHudOverlay implements IGuiOverlay {
    private static final ResourceLocation SEMI = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_semi.png");
    private static final ResourceLocation AUTO = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_auto.png");
    private static final ResourceLocation BURST = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_burst.png");

    private static final DecimalFormat AMMO_FORMAT = new DecimalFormat("000");
    private static final DecimalFormat AMMO_FORMAT_PERCENT = new DecimalFormat("000%");
    private static final DecimalFormat INVENTORY_AMMO_FORMAT = new DecimalFormat("0000");
    private static final int MAX_AMMO_COUNT = 9999;

    private static final int ROW_WIDTH = 140;
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 2;
    private static final int MARGIN = 8;
    private static final int BACKING = 0x80000000;
    private static final int BACKING_SELECTED = 0xB0000000;
    private static final int ACCENT = 0xFFFFFFFF;
    private static final int LOW = 0xFF5555;
    private static final int DIM = 0x999999;

    /** Per hotbar slot: when its counts were last taken, its magazine size and the ammo carried for it. */
    private final long[] checkedAt = new long[Inventory.getSelectionSize()];
    private final int[] maxAmmo = new int[Inventory.getSelectionSize()];
    private final int[] inventoryAmmo = new int[Inventory.getSelectionSize()];

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        int slots = ClientGunUseRestriction.usableSlots();
        if (!RenderConfig.GUN_HUD_ENABLE.get() || player == null || mc.options.hideGui
                || slots >= Inventory.getSelectionSize()) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean anyGun = false;
        for (int slot = 0; slot < slots; slot++) {
            anyGun |= inventory.getItem(slot).getItem() instanceof IGun;
        }
        if (!anyGun) {
            return;
        }
        int left = width - MARGIN - ROW_WIDTH;
        int top = height - MARGIN - slots * ROW_HEIGHT - (slots - 1) * ROW_GAP;
        for (int slot = 0; slot < slots; slot++) {
            int y = top + slot * (ROW_HEIGHT + ROW_GAP);
            drawRow(mc, graphics, player, slot, slot == inventory.selected, left, y);
        }
    }

    private void drawRow(Minecraft mc, GuiGraphics graphics, LocalPlayer player, int slot, boolean selected, int x, int y) {
        Font font = mc.font;
        graphics.fill(x, y, x + ROW_WIDTH, y + ROW_HEIGHT, selected ? BACKING_SELECTED : BACKING);
        if (selected) {
            graphics.fill(x, y, x + 2, y + ROW_HEIGHT, ACCENT);
        }
        // the key that selects this slot, printed where it applies
        graphics.drawString(font, String.valueOf(slot + 1), x + 5, y + 7, selected ? 0xFFFFFF : DIM, false);

        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof IGun iGun)) {
            graphics.drawString(font, "-", x + 16, y + 7, DIM, false);
            return;
        }
        GunData gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).map(ClientGunIndex::getGunData).orElse(null);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (gunData == null || display == null) {
            return;
        }
        boolean useInventoryAmmo = iGun.useInventoryAmmo(stack);
        boolean overheatLocked = gunData.hasHeatData() && iGun.isOverheatLocked(stack);
        updateCounts(player, stack, iGun, gunData, slot, selected && useInventoryAmmo);

        int barrel = iGun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0;
        int ammo = Math.min((useInventoryAmmo ? inventoryAmmo[slot] : iGun.getCurrentAmmoCount(stack)) + barrel, MAX_AMMO_COUNT);
        boolean low = ammo < maxAmmo[slot] * 0.25 && ammo < 10 || overheatLocked;
        String ammoText = display.getAmmoCountStyle() == AmmoCountStyle.PERCENT
                ? AMMO_FORMAT_PERCENT.format((float) ammo / (maxAmmo[slot] == 0 ? 1f : maxAmmo[slot]))
                : AMMO_FORMAT.format(ammo);

        // gun icon, dimmed when not in hand, reddened (or its empty texture) when it cannot fire
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        ResourceLocation icon = display.getHUDTexture();
        @Nullable ResourceLocation emptyIcon = display.getHudEmptyTexture();
        float shade = selected ? 1f : 0.55f;
        if ((ammo <= 0 || overheatLocked) && emptyIcon != null) {
            icon = emptyIcon;
            RenderSystem.setShaderColor(shade, shade, shade, 1);
        } else if (ammo <= 0 || overheatLocked) {
            RenderSystem.setShaderColor(shade, 0.3f * shade, 0.3f * shade, 1);
        } else {
            RenderSystem.setShaderColor(shade, shade, shade, 1);
        }
        graphics.blit(icon, x + 14, y + 5, 0, 0, 39, 13, 39, 13);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        int ammoX = x + 58;
        int ammoColor = low ? LOW : selected ? 0xFFFFFF : DIM;
        int ammoWidth;
        if (selected) {
            // integer scale only: the pixel font blurs at anything else
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.scale(2, 2, 1);
            graphics.drawString(font, ammoText, ammoX / 2, (y + 4) / 2, ammoColor, false);
            pose.popPose();
            ammoWidth = font.width(ammoText) * 2;
        } else {
            graphics.drawString(font, ammoText, ammoX, y + 7, ammoColor, false);
            ammoWidth = font.width(ammoText);
        }
        if (low && selected) {
            // not by colour alone: a low magazine also pulses its row's edge
            int alpha = (int) (160 + 95 * Math.sin(System.currentTimeMillis() / 150.0));
            graphics.fill(x + ROW_WIDTH - 2, y, x + ROW_WIDTH, y + ROW_HEIGHT, alpha << 24 | LOW);
        }

        if (selected) {
            String reserve = useInventoryAmmo ? ""
                    : gunData.getReloadData().isInfinite() ? "∞" : INVENTORY_AMMO_FORMAT.format(inventoryAmmo[slot]);
            graphics.drawString(font, reserve, ammoX + ammoWidth + 3, y + 11, 0xAAAAAA, false);
            FireMode fireMode = iGun.getFireMode(stack);
            ResourceLocation fireModeIcon = switch (fireMode) {
                case AUTO -> AUTO;
                case BURST -> BURST;
                default -> SEMI;
            };
            RenderSystem.enableBlend();
            graphics.blit(fireModeIcon, x + ROW_WIDTH - 15, y + 6, 0, 0, 10, 10, 10, 10);
        }
    }

    /**
     * As {@link GunHudOverlay} does for the gun in hand, at most once a tick per slot. Like it, a gun fed
     * straight from the inventory has that count written back as its magazine, but only the gun in hand.
     */
    private void updateCounts(LocalPlayer player, ItemStack stack, IGun iGun, GunData gunData, int slot, boolean writeBack) {
        long now = System.currentTimeMillis();
        if (now - checkedAt[slot] <= 50) {
            return;
        }
        checkedAt[slot] = now;
        maxAmmo[slot] = AttachmentDataUtils.getAmmoCountWithAttachment(stack, gunData);
        if (!IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
            inventoryAmmo[slot] = MAX_AMMO_COUNT;
        } else if (iGun.useDummyAmmo(stack)) {
            inventoryAmmo[slot] = iGun.getDummyAmmoAmount(stack);
        } else {
            inventoryAmmo[slot] = countInventoryAmmo(stack, player.getInventory());
        }
        if (writeBack) {
            iGun.setCurrentAmmoCount(stack, inventoryAmmo[slot]);
        }
    }

    private static int countInventoryAmmo(ItemStack gun, Inventory inventory) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gun, item)) {
                count += item.getCount();
            }
            if (item.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gun, item)) {
                if (iAmmoBox.isAllTypeCreative(item) || iAmmoBox.isCreative(item)) {
                    return MAX_AMMO_COUNT;
                }
                count += iAmmoBox.getAmmoCount(item);
            }
        }
        return Math.min(count, MAX_AMMO_COUNT);
    }
}
