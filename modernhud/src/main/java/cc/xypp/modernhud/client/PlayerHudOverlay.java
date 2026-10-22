package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.AbsorptionColorMode;
import cc.xypp.modernhud.config.BarTextPosition;
import cc.xypp.modernhud.config.HpTextFormat;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.config.NamePosition;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Personal HUD, MW2022 / Warzone / DMZ style: clean, seamless, compact. Fully code-drawn.
 * <pre>
 *   PlayerName                            (name ABOVE or BELOW the bars - namePosition)
 *   [plate] [plate] [plate]               (Absorption = shield plates, gaps, reserved row)
 *   [====== thin health rect ======] 20/20
 *   ARMOR 20/20                           (armor is TEXT ONLY now, no bar)
 *   ^ HEAD is left of the BAR BLOCK only, vertically centred on it (not the name line)
 * </pre>
 * Health is a thin rectangle that glides on a time-based eased animation (see
 * {@link EasedAnimator}) — a fixed real-world glide duration with a quintic ease-out curve, rather
 * than the mod's older per-frame exponential decay, so the motion feels the same regardless of
 * framerate and settles with a deliberate "premium" curve instead of trailing off asymptotically.
 * A brief bright glow flashes behind the bar when you take damage, and a slow breathing glow pulses
 * while health is critically low (see {@link HudConfig#showVitalsGlow}, drawn via
 * {@link GlowEffect}) — both purely visual accents layered behind the bar itself, which still
 * changes colour at the low-health threshold regardless. Armor has no bar at all any
 * more — it's a plain text readout, like the HP text, since a second thin rectangle right under
 * the health bar read as visual clutter rather than useful information.
 *
 * <p>The background panel (see {@link HudPanel}) is a single flat colour at an adjustable opacity
 * — no gradient/fade, no continuous border — with an optional small corner-bracket accent (four
 * short "L" marks, one per corner, modelled on the referenced MW4 HUD) layered on top when
 * {@link HudConfig#borderStyle} is CORNERS (or a full thin outline when it's FULL). Its size is
 * measured from whatever is
 * ACTUALLY going to be drawn this frame (bar width, any end-of-bar text, the shared info line,
 * the absorption overflow "+N") rather than a fixed guess, specifically so it never has an empty
 * chunk hanging off the end when some of those aren't in use — {@link #layoutPass} is run once to
 * measure (which also fills in {@link #lastContentWidth}) before it's run again to actually draw.
 */
public class PlayerHudOverlay implements IGuiOverlay {

    private static final int HEAD_SIZE = 18;
    private static final int ROW_GAP = 2;
    /** Duration of the bright damage-flash glow behind the health bar - see {@link HudConfig#showVitalsGlow}. */
    private static final long DAMAGE_FLASH_MS = 350L;

    /** Time-based eased glide toward the current health fraction (replaces the old frame-rate-
     *  dependent exponential smoothing - see {@link EasedAnimator}'s class javadoc for why). */
    private final EasedAnimator healthAnimator = new EasedAnimator(280f, EasedAnimator.Easing.QUINTIC_OUT);
    /** Health fraction as of the last frame, so a DECREASE (taking damage) can be detected to
     *  trigger {@link #damageFlashStartMs} - a plain "did it go down" comparison, not a duplicate
     *  animator, since the flash is a one-shot pulse, not something that itself glides. */
    private float lastHealthTarget = -1f;
    private long damageFlashStartMs = Long.MIN_VALUE / 2;
    /** Same glide treatment as {@link #healthAnimator}, applied to the Absorption plate row so a
     *  shield hit (or a shield being applied) drains/fills smoothly instead of jumping instantly -
     *  the plates used to redraw straight off the raw entity value with no animation at all. */
    private final EasedAnimator absorptionAnimator = new EasedAnimator(220f, EasedAnimator.Easing.QUINTIC_OUT);
    /** Same "did it go down" detection as {@link #lastHealthTarget}, for the Absorption row's own
     *  brief hit-flash. */
    private float lastAbsorptionTarget = -1f;
    private long absorptionFlashStartMs = Long.MIN_VALUE / 2;
    /** Width of the widest row actually laid out this frame (bar, end-of-bar text, shared info
     *  line, own-row armor text, absorption overflow) — set by {@link #layoutPass} on its
     *  measure-only call in {@link #render}, then used to size the panel precisely. Includes the
     *  head column already. */
    private int lastContentWidth;

    /** This frame's resolved screen-space bounds of the whole widget (post-scale), so
     *  {@code TeamHudOverlay} can stack the team list directly above it without duplicating the
     *  position-resolution math. Package-private static, updated at the top of every
     *  {@link #render} call — relies on {@code PlayerHudOverlay} rendering before
     *  {@code TeamHudOverlay} each frame (see the registration order in {@code ClientEvents}); if
     *  this overlay is disabled/didn't run this frame these simply hold last frame's values, which
     *  is harmless since {@code TeamHudOverlay} also bails out when {@code HudConfig.enabled} is
     *  off. Defaults describe a reasonable bottom-left corner box before the first real frame runs. */
    static int lastFrameX;
    static int lastFrameY = Integer.MAX_VALUE / 2; // "far down/offscreen" until the first real frame sets it
    static int lastFrameWidth;
    static int lastFrameHeight;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!HudConfig.enabled) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (player.isSpectator()) {
            // Only this mod's own personal-status bar is skipped here - vanilla's HUD rendering is
            // a completely separate code path we never touch, so it's unaffected either way.
            return;
        }
        if (!gui.shouldDrawSurvivalElements()) {
            // Decade: creative has no health to watch, as vanilla's hearts are hidden there too.
            return;
        }
        if (SuperbWarfareCompat.isInVehicle(player)) {
            // Riding an SBW vehicle - it draws its own vehicle/weapon HUD over roughly the same
            // screen area (bottom corners), so ours steps aside entirely rather than overlap it.
            return;
        }
        Font font = Minecraft.getInstance().font;
        double scale = HudConfig.barScale;
        float alpha = chatFade();
        if (alpha <= 0.01f) {
            return;
        }
        boolean decade = PlateSource.present();

        int panelPad = HudConfig.showPanel ? HudPanel.padding() : 0;
        int measuredHeight = decade ? decadeLayout(player, font, false, null, HudConfig.barWidth)
                : layoutPass(player, font, false, null, HudConfig.barWidth);
        int totalWidth = lastContentWidth + panelPad * 2;
        int totalHeight = measuredHeight + panelPad * 2;

        int[] pos = resolvePosition(screenWidth, screenHeight, totalWidth, totalHeight, scale);

        lastFrameX = pos[0];
        lastFrameY = pos[1];
        lastFrameWidth = (int) Math.round(totalWidth * scale);
        lastFrameHeight = (int) Math.round(totalHeight * scale);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        pose.translate(pos[0], pos[1], 0);
        pose.scale((float) scale, (float) scale, 1.0f);

        if (HudConfig.showPanel) {
            HudPanel.draw(graphics, 0, 0, totalWidth, totalHeight, HudConfig.panelColor, HudConfig.panelOpacity);
            HudPanel.drawBorder(graphics, 0, 0, totalWidth, totalHeight,
                    HudConfig.borderColor, HudConfig.borderOpacity, HudConfig.borderStyle, HudConfig.borderLength, HudConfig.borderOutset);
        }

        pose.translate(panelPad, panelPad, 0);
        try {
            if (decade) {
                decadeLayout(player, font, true, graphics, HudConfig.barWidth);
                // Decade: a worn device's charge, a row over the panel, lined up with the bars (RigSource)
                Object[] rig = RigSource.read();
                if (rig != null) {
                    int headWidth = HudConfig.showHead ? HEAD_SIZE + 6 : 0;
                    RigSource.draw(graphics, font, rig, headWidth, HudConfig.barWidth, headWidth + HudConfig.barWidth + 4,
                            -panelPad - 4);
                }
            } else {
                layoutPass(player, font, true, graphics, HudConfig.barWidth);
            }
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            pose.popPose();
        }
    }

    /** Decade: how far the panel has faded under an open chat, whose box and channel strip cover it. */
    private float chatFadeValue = 1f;
    private long chatFadeAtMs = -1L;
    private static final float CHAT_FADE_MS = 150f;

    /**
     * Decade: 1 normally, easing to 0 over {@link #CHAT_FADE_MS} while the chat screen is open and
     * back when it closes - the box and channel strip rise over this corner and the bars would show
     * through them.
     */
    private float chatFade() {
        long now = System.currentTimeMillis();
        float step = chatFadeAtMs < 0 ? 1f : (now - chatFadeAtMs) / CHAT_FADE_MS;
        chatFadeAtMs = now;
        boolean chatOpen = Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ChatScreen;
        chatFadeValue = chatOpen ? Math.max(0f, chatFadeValue - step) : Math.min(1f, chatFadeValue + step);
        return chatFadeValue;
    }

    /**
     * Runs the whole vertical layout once. With {@code draw=false} it only measures (no GuiGraphics
     * needed) so {@link #render} can size the background panel before anything is actually drawn —
     * and, as part of that measuring, sets {@link #lastContentWidth} to however wide the content
     * actually needs to be this frame (never a fixed guess), which is what keeps the panel snug
     * around the real content instead of leaving an unused chunk hanging off the end.
     */
    private int layoutPass(LocalPlayer player, Font font, boolean draw, GuiGraphics graphics, int barWidth) {
        int lineH = font.lineHeight;
        int healthH = Math.max(2, Math.round(HudConfig.barHeight * 0.5f));
        int plateH = Math.max(2, HudConfig.absorptionHeight);
        int headWidth = HudConfig.showHead ? HEAD_SIZE + 6 : 0;
        int barX = headWidth;
        int contentWidth = barWidth; // grows to fit whatever text rows actually need beyond the bar

        boolean nameAbove = HudConfig.namePosition != NamePosition.BELOW;
        int nameH = HudConfig.showName ? lineH + ROW_GAP : 0;

        // "Below name/head" is a single shared info line (HP and/or Armor readouts routed there).
        boolean hpBelowNameHead = HudConfig.showHpText && HudConfig.hpTextPosition == BarTextPosition.BELOW_NAME_HEAD;
        boolean armorBelowNameHead = HudConfig.showArmor && HudConfig.armorTextPosition == BarTextPosition.BELOW_NAME_HEAD;
        int infoLineH = (hpBelowNameHead || armorBelowNameHead) ? lineH + ROW_GAP : 0;
        if (hpBelowNameHead || armorBelowNameHead) {
            contentWidth = Math.max(contentWidth, sharedInfoLineWidth(font, player, hpBelowNameHead, armorBelowNameHead));
        }

        boolean hpBelowBar = HudConfig.showHealthBar && HudConfig.showHpText
                && HudConfig.hpTextPosition == BarTextPosition.BELOW_BAR;
        boolean hpEndOfBar = HudConfig.showHealthBar && HudConfig.showHpText
                && HudConfig.hpTextPosition == BarTextPosition.END_OF_BAR;
        // Armor is text-only now (no bar) - END_OF_BAR and BELOW_BAR both just mean "own row
        // directly under the health bar", there's no bar left for either to be relative to.
        boolean armorOwnRow = HudConfig.showArmor && HudConfig.armorTextPosition != BarTextPosition.BELOW_NAME_HEAD;

        if (hpBelowBar) {
            contentWidth = Math.max(contentWidth, font.width(hpString(player)));
        }
        if (hpEndOfBar) {
            contentWidth = Math.max(contentWidth, barWidth + 4 + font.width(hpString(player)));
        }
        if (armorOwnRow) {
            contentWidth = Math.max(contentWidth, font.width(armorString(player)));
        }
        if (HudConfig.showOverflowText && player.getAbsorptionAmount() > 0.0f) {
            float overflow = BarRender.overflow(player.getAbsorptionAmount(), HudConfig.segmentCount, HudConfig.pointsPerSegment);
            if (overflow > 0.01f) {
                contentWidth = Math.max(contentWidth, barWidth + 4 + font.width("+" + formatOneOrZeroDecimals(overflow)));
            }
        }

        int barBlockH = plateH + ROW_GAP;
        if (HudConfig.showHealthBar) {
            barBlockH += healthH;
            if (hpBelowBar) barBlockH += lineH + ROW_GAP;
        }
        if (armorOwnRow) {
            barBlockH += ROW_GAP + lineH;
        }

        int totalBlockH = nameH + infoLineH + barBlockH;
        int rowH = Math.max(totalBlockH, HudConfig.showHead ? HEAD_SIZE : 0);

        // HEAD ALIGNMENT FIX: centre the head on the bar block itself (plates/health/armor),
        // not on the whole block including the name line above/below it - that's what made the
        // head look vertically off relative to the bars.
        int barBlockY = nameAbove ? nameH + infoLineH : 0;
        if (draw && HudConfig.showHead) {
            int headY = barBlockY + (barBlockH - HEAD_SIZE) / 2;
            headY = Math.max(0, Math.min(headY, rowH - HEAD_SIZE));
            renderHead(graphics, player, 0, headY, HEAD_SIZE);
        }

        int cursorY = Math.max(0, (rowH - totalBlockH) / 2);

        if (HudConfig.showName && nameAbove) {
            if (draw) {
                graphics.drawString(font, player.getName(), barX, cursorY, 0xFFFFFFFF, true);
            }
            cursorY += nameH;
        }

        if (infoLineH > 0) {
            if (draw) {
                drawSharedInfoLine(graphics, font, player, barX, cursorY, hpBelowNameHead, armorBelowNameHead);
            }
            cursorY += infoLineH;
        }

        // Absorption plates (the "shield" row) — always reserved, glides smoothly (same eased
        // technique as the health bar below it) rather than jumping straight to the raw value, and
        // stays drawn while the glide is still easing down to zero even the instant the real
        // value hits zero (e.g. the shield being fully broken in one hit).
        if (draw) {
            float absorptionRaw = Math.max(0f, player.getAbsorptionAmount());
            if (lastAbsorptionTarget < 0f) {
                absorptionAnimator.snapTo(absorptionRaw);
            } else if (absorptionRaw < lastAbsorptionTarget - 0.0005f) {
                absorptionFlashStartMs = System.currentTimeMillis();
            }
            lastAbsorptionTarget = absorptionRaw;
            absorptionAnimator.setTarget(absorptionRaw);
            float animatedAbsorption = absorptionAnimator.update();

            if (animatedAbsorption > 0.01f) {
                boolean customColor = HudConfig.absorptionColorMode == AbsorptionColorMode.CUSTOM;

                if (HudConfig.showVitalsGlow) {
                    long flashElapsed = System.currentTimeMillis() - absorptionFlashStartMs;
                    float flashAlpha = (flashElapsed >= 0 && flashElapsed < DAMAGE_FLASH_MS)
                            ? (1f - (float) flashElapsed / DAMAGE_FLASH_MS) : 0f;
                    flashAlpha *= flashAlpha; // squared for a punchier "hit" falloff than linear
                    if (flashAlpha > 0.01f) {
                        int glowColor = customColor ? HudConfig.absorptionCustomColor : BarRender.TIER_BLUE;
                        GlowEffect.drawGlowRect(graphics, barX, cursorY, barWidth, plateH, glowColor, flashAlpha, 3f);
                    }
                }

                BarRender.plates(graphics, barX, cursorY, barWidth, plateH,
                        animatedAbsorption, HudConfig.segmentCount, HudConfig.pointsPerSegment,
                        customColor, HudConfig.absorptionCustomColor);
                if (HudConfig.showOverflowText) {
                    float overflow = BarRender.overflow(absorptionRaw, HudConfig.segmentCount, HudConfig.pointsPerSegment);
                    if (overflow > 0.01f) {
                        String txt = "+" + formatOneOrZeroDecimals(overflow);
                        graphics.drawString(font, txt, barX + barWidth + 4, cursorY + (plateH - lineH) / 2, 0xFFCFE8FF, true);
                    }
                }
            }
        }
        cursorY += plateH + ROW_GAP;

        if (HudConfig.showHealthBar) {
            float maxHealth = Math.max(1.0f, player.getMaxHealth());
            float target = Math.max(0.0f, Math.min(1.0f, player.getHealth() / maxHealth));
            if (draw) {
                if (lastHealthTarget < 0f) {
                    healthAnimator.snapTo(target);
                } else if (target < lastHealthTarget - 0.0005f) {
                    damageFlashStartMs = System.currentTimeMillis();
                }
                lastHealthTarget = target;
                healthAnimator.setTarget(target);
                float animatedHealth = healthAnimator.update();

                double percent = 100.0 * target;
                boolean isLow = percent <= HudConfig.lowHealthThresholdPercent;
                int color = BarRender.healthColor(player, isLow);

                if (HudConfig.showVitalsGlow) {
                    long flashElapsed = System.currentTimeMillis() - damageFlashStartMs;
                    float flashAlpha = (flashElapsed >= 0 && flashElapsed < DAMAGE_FLASH_MS)
                            ? (1f - (float) flashElapsed / DAMAGE_FLASH_MS) : 0f;
                    flashAlpha *= flashAlpha; // squared for a punchier "hit" falloff than linear
                    float pulseAlpha = isLow
                            ? 0.22f + 0.30f * 0.5f * (1f + (float) Math.sin(System.currentTimeMillis() / 260.0))
                            : 0f;
                    float glowAlpha = Math.max(flashAlpha, pulseAlpha);
                    if (glowAlpha > 0.01f) {
                        GlowEffect.drawGlowRect(graphics, barX, cursorY, barWidth, healthH,
                                HudConfig.vitalsGlowColor, glowAlpha, 3f);
                    }
                }

                BarRender.bar(graphics, barX, cursorY, barWidth, healthH, animatedHealth, color);
                if (hpEndOfBar) {
                    drawHpText(graphics, font, player, barX + barWidth + 4, cursorY + (healthH - lineH) / 2);
                }
            }
            cursorY += healthH;
            if (hpBelowBar) {
                if (draw) {
                    drawHpText(graphics, font, player, barX, cursorY);
                }
                cursorY += lineH + ROW_GAP;
            }
            if (armorOwnRow) cursorY += ROW_GAP;
        }

        if (armorOwnRow) {
            if (draw) {
                drawArmorText(graphics, font, player, barX, cursorY);
            }
            cursorY += lineH;
        }

        if (HudConfig.showName && !nameAbove) {
            if (draw) {
                graphics.drawString(font, player.getName(), barX, cursorY, 0xFFFFFFFF, true);
            }
            cursorY += lineH;
        }

        lastContentWidth = headWidth + contentWidth;
        return Math.max(rowH, cursorY);
    }

    private int sharedInfoLineWidth(Font font, LocalPlayer player, boolean hp, boolean armor) {
        int w = 0;
        if (hp) {
            w += font.width(hpString(player));
            if (armor) w += 6;
        }
        if (armor) {
            w += font.width(armorString(player));
        }
        return w;
    }

    private void drawSharedInfoLine(GuiGraphics graphics, Font font, LocalPlayer player, int x, int y,
                                     boolean hp, boolean armor) {
        int cursorX = x;
        if (hp) {
            String text = hpString(player);
            int color = hpColor(player);
            graphics.drawString(font, text, cursorX, y, color, true);
            cursorX += font.width(text) + 6;
        }
        if (armor) {
            String text = armorString(player);
            graphics.drawString(font, text, cursorX, y, HudConfig.armorTextColor | 0xFF000000, true);
        }
    }

    private void drawHpText(GuiGraphics graphics, Font font, LocalPlayer player, int x, int y) {
        graphics.drawString(font, hpString(player), x, y, hpColor(player), true);
    }

    private void drawArmorText(GuiGraphics graphics, Font font, LocalPlayer player, int x, int y) {
        graphics.drawString(font, armorString(player), x, y, HudConfig.armorTextColor | 0xFF000000, true);
    }

    private String hpString(LocalPlayer player) {
        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = Math.max(0.0f, player.getHealth());
        double percent = 100.0 * health / maxHealth;
        if (HudConfig.hpTextFormat == HpTextFormat.WHOLE) {
            // a sliver of health left still reads as 1, never as 0 while alive
            int whole = health > 0 ? Math.max(1, Math.round(health)) : 0;
            return whole + "/" + Math.round(maxHealth);
        }
        return (HudConfig.hpTextFormat == HpTextFormat.PERCENT)
                ? Math.round(percent) + "%"
                : String.format("%.1f/%.1f", health, maxHealth);
    }

    private int hpColor(LocalPlayer player) {
        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = Math.max(0.0f, player.getHealth());
        double percent = 100.0 * health / maxHealth;
        return (percent <= HudConfig.lowHealthThresholdPercent)
                ? HudConfig.hpTextLowColor : HudConfig.hpTextNormalColor;
    }

    private String armorString(LocalPlayer player) {
        int armor = player.getArmorValue();
        return (HudConfig.armorTextFormat == HpTextFormat.PERCENT)
                ? Math.round(100.0 * armor / 20.0) + "%"
                : armor + "/20";
    }

    /** Decade: between the plate row and the health bar, so the two rows are exactly as tall as a digit. */
    private static final int DECADE_ROW_GAP = 1;
    /** Decade: a pouch slot's width, and the space between the HP number and the first one. */
    private static final int POUCH_SLOT = 3;
    private static final int POUCH_GAP = 2;
    /** Decade: a refilling slot's fill, faint so a half-refilled slot never reads as one ready to use. */
    private static final int POUCH_REFILL_ALPHA = 0x73;
    private int lastPouchLeft = -1;
    private int pouchFlashFrom;
    private int pouchFlashTo;
    private long pouchFlashStartMs = Long.MIN_VALUE / 2;

    /**
     * Decade: the layout when {@link PlateSource} supplies the plates.
     * <pre>
     *   [head]  [plate][plate][plate][    ]  48/60 [#][#][ ]
     *           [====== thin health =====]
     * </pre>
     * The plate row (one segment per plate the wearer can carry, empty ones still drawn) sits one
     * pixel over the health bar, so the two are seven pixels tall together - a digit's height: the
     * HP number covers both, and after it the pouch, one slot per plate it holds for this fight,
     * the used ones left as empty slots like the plates'. Outside a fight there is no pouch to show.
     * Same glide, hit flash and low-health breathing as upstream's rows. Measures only with
     * {@code draw=false}, setting {@link #lastContentWidth} as {@link #layoutPass} does.
     */
    private int decadeLayout(LocalPlayer player, Font font, boolean draw, GuiGraphics graphics, int barWidth) {
        float[] plates = PlateSource.read();
        int plateH = Math.max(2, HudConfig.absorptionHeight);
        int healthH = Math.max(2, Math.round(HudConfig.barHeight * 0.5f));
        int block = plateH + DECADE_ROW_GAP + healthH;
        int headWidth = HudConfig.showHead ? HEAD_SIZE + 6 : 0;
        int rowH = Math.max(block, HudConfig.showHead ? HEAD_SIZE : 0);
        String hp = hpString(player);
        int numberX = headWidth + barWidth + 4;
        int pouchX = numberX + font.width(hp) + POUCH_GAP;
        int pouchSlots = plates != null && plates[3] >= 0 ? Math.max(0, Math.round(plates[4])) : 0;
        lastContentWidth = pouchX + (pouchSlots > 0 ? pouchSlots * (POUCH_SLOT + 1) - 1 : 0);
        if (!draw) {
            return rowH;
        }

        if (HudConfig.showHead) {
            renderHead(graphics, player, 0, (rowH - HEAD_SIZE) / 2, HEAD_SIZE);
        }
        int barX = headWidth;
        int plateY = (rowH - block) / 2;
        int healthY = plateY + plateH + DECADE_ROW_GAP;
        long now = System.currentTimeMillis();

        // plates
        float raw = plates == null ? 0f : Math.max(0f, plates[0]);
        if (lastAbsorptionTarget < 0f) {
            absorptionAnimator.snapTo(raw);
        } else if (raw < lastAbsorptionTarget - 0.0005f) {
            absorptionFlashStartMs = now;
        }
        lastAbsorptionTarget = raw;
        absorptionAnimator.setTarget(raw);
        float animatedPlates = absorptionAnimator.update();
        if (plates != null) {
            if (HudConfig.showVitalsGlow) {
                float flash = flash(now - absorptionFlashStartMs);
                if (flash > 0.01f) {
                    GlowEffect.drawGlowRect(graphics, barX, plateY, barWidth, plateH, HudConfig.absorptionCustomColor, flash, 3f);
                }
            }
            int segments = Math.max(1, Math.round(plates[1] / plates[2]));
            BarRender.plates(graphics, barX, plateY, barWidth, plateH, animatedPlates, segments, plates[2],
                    HudConfig.absorptionColorMode == AbsorptionColorMode.CUSTOM, HudConfig.absorptionCustomColor);
        }

        // health
        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float target = Math.max(0.0f, Math.min(1.0f, player.getHealth() / maxHealth));
        if (lastHealthTarget < 0f) {
            healthAnimator.snapTo(target);
        } else if (target < lastHealthTarget - 0.0005f) {
            damageFlashStartMs = now;
        }
        lastHealthTarget = target;
        healthAnimator.setTarget(target);
        float animatedHealth = healthAnimator.update();
        boolean isLow = 100.0 * target <= HudConfig.lowHealthThresholdPercent;
        if (HudConfig.showVitalsGlow) {
            float pulse = isLow ? 0.22f + 0.30f * 0.5f * (1f + (float) Math.sin(now / 260.0)) : 0f;
            float glow = Math.max(flash(now - damageFlashStartMs), pulse);
            if (glow > 0.01f) {
                GlowEffect.drawGlowRect(graphics, barX, healthY, barWidth, healthH, HudConfig.vitalsGlowColor, glow, 3f);
            }
        }
        BarRender.bar(graphics, barX, healthY, barWidth, healthH, animatedHealth, BarRender.healthColor(player, isLow));

        // the number over both rows, then the pouch
        graphics.drawString(font, hp, numberX, plateY, hpColor(player), true);
        int left = plates != null && plates[3] >= 0 ? Math.round(plates[3]) : -1;
        if (left > lastPouchLeft && lastPouchLeft >= 0) {
            // the pouch filled again: those slots flash once (the one moment of this corner)
            pouchFlashFrom = lastPouchLeft;
            pouchFlashTo = left;
            pouchFlashStartMs = now;
        }
        lastPouchLeft = left;
        float refill = plates != null && plates.length > 5 ? plates[5] : -1f;
        int color = HudConfig.absorptionCustomColor | 0xFF000000;
        float flash = flash(now - pouchFlashStartMs);
        for (int i = 0; i < pouchSlots; i++) {
            int x = pouchX + i * (POUCH_SLOT + 1);
            graphics.fill(x, plateY, x + POUCH_SLOT, plateY + block, BarRender.TRACK);
            if (i < left) {
                graphics.fill(x, plateY, x + POUCH_SLOT, plateY + block, color);
                graphics.fill(x, plateY, x + POUCH_SLOT, plateY + 1, BarRender.lighten(color, 0.30f));
                if (flash > 0.01f && i >= pouchFlashFrom && i < pouchFlashTo) {
                    graphics.fill(x, plateY, x + POUCH_SLOT, plateY + block, Math.round(flash * 0xE0) << 24 | 0xFFFFFF);
                }
            } else if (refill > 0f) {
                // a used slot, refilling: a faint fill rising from the bottom, solid only once it is full
                int h = Math.round(block * refill);
                graphics.fill(x, plateY + block - h, x + POUCH_SLOT, plateY + block, POUCH_REFILL_ALPHA << 24 | (color & 0xFFFFFF));
            }
        }
        return rowH;
    }

    /** Decade: the hit flash's strength {@code elapsed} ms after a hit - squared, as upstream's. */
    private static float flash(long elapsed) {
        float f = (elapsed >= 0 && elapsed < DAMAGE_FLASH_MS) ? (1f - (float) elapsed / DAMAGE_FLASH_MS) : 0f;
        return f * f;
    }

    private String formatOneOrZeroDecimals(float value) {
        long rounded = Math.round(value);
        return Math.abs(value - rounded) < 0.05f ? String.valueOf(rounded) : String.format("%.1f", value);
    }

    static void renderHead(GuiGraphics graphics, AbstractClientPlayer player, int x, int y, int size) {
        RenderSystem.setShaderTexture(0, player.getSkinTextureLocation());
        RenderSystem.enableBlend();
        graphics.blit(player.getSkinTextureLocation(), x, y, size, size, 8, 8, 8, 8, 64, 64);
        graphics.blit(player.getSkinTextureLocation(), x, y, size, size, 40, 8, 8, 8, 64, 64);
        RenderSystem.disableBlend();
    }

    private int[] resolvePosition(int screenWidth, int screenHeight, int width, int height, double scale) {
        int scaledWidth = (int) Math.round(width * scale);
        int scaledHeight = (int) Math.round(height * scale);
        int x;
        int y;
        switch (HudConfig.position) {
            case TOP_LEFT -> { x = HudConfig.offsetX; y = HudConfig.offsetY; }
            case TOP_RIGHT -> { x = screenWidth - scaledWidth - HudConfig.offsetX; y = HudConfig.offsetY; }
            case BOTTOM_RIGHT -> { x = screenWidth - scaledWidth - HudConfig.offsetX; y = screenHeight - scaledHeight - HudConfig.offsetY; }
            case CUSTOM -> { x = HudConfig.offsetX; y = HudConfig.offsetY; }
            default -> { x = HudConfig.offsetX; y = screenHeight - scaledHeight - HudConfig.offsetY; }
        }
        return new int[]{x, y};
    }
}
