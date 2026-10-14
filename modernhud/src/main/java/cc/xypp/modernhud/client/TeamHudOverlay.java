package cc.xypp.modernhud.client;

import cc.xypp.modernhud.config.AbsorptionColorMode;
import cc.xypp.modernhud.config.HudConfig;
import cc.xypp.modernhud.client.compat.SuperbWarfareCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Teammate health list (vanilla scoreboard team), Warzone-style: one compact row per teammate —
 * head icon, name (coloured by their vanilla team colour by default), and a smoothly-animated
 * health bar — stacked directly above the player's own widget ({@link PlayerHudOverlay}), sharing
 * ONE background chip/border of its own, separate from the main widget's own panel. Capped at a
 * configurable row count ({@link HudConfig#teamHudMaxPlayers}, default 6); extras beyond that are
 * simply hidden, sorted alphabetically by name so the cutoff is at least stable/predictable rather
 * than shuffling every frame.
 *
 * <p>Reads {@link LocalPlayer#getTeam()} and matches it against every other tracked
 * {@link Player} in the level — the SAME vanilla scoreboard team system {@code /team} itself
 * manages (join/leave/kick, team colour, all of it), not a separate team system of its own. A
 * teammate only appears here while their entity is actually loaded/tracked client-side — the same
 * range vanilla already shows other players' names and health at. There's no way to show a
 * teammate's live stats from across the map, or on a different dimension/server, without a
 * server-side data-sync system, which this doesn't attempt to be: someone out of range simply
 * doesn't appear until they're back in range, the same as any other client-side player info.
 *
 * <p>Positioned off {@link PlayerHudOverlay}'s own last-computed screen bounds (its package-private
 * {@code lastFrameX}/{@code lastFrameY} fields) so it always sits flush above it — which means this
 * MUST render AFTER {@link PlayerHudOverlay} each frame (see the registration order in
 * {@code ClientEvents}); its left edge lines up with the main widget's left edge, which matches the
 * default BOTTOM_LEFT anchor cleanly but won't right-align against a right-anchored main widget of
 * a different width — a reasonable trade-off to keep this first version simple.
 *
 * <p>When {@link HudConfig#showTeamAbsorption} is on, each row also gets its own Absorption
 * (shield) plate bar directly above its health bar — same segmented-plate look, tiered/custom
 * colour mode, and segment/points-per-segment settings as the player's own widget, so the two read
 * as one consistent style (see {@link #drawRow}). A teammate currently in Spectator mode always
 * shows this as empty (glides down to zero, same as any other decrease) rather than a leftover
 * value the server hasn't cleared yet, since spectators aren't meant to be tracked as a combat
 * threat/target.
 */
public class TeamHudOverlay implements IGuiOverlay {

    private static final int HEAD_SIZE = 14;
    private static final int BAR_H = 3;
    private static final int PLATE_GAP = 1;

    /** Per-teammate eased health-glide animator, keyed by UUID, so each teammate's bar glides
     *  independently instead of sharing one smoothing state — same time-based quintic-ease-out
     *  technique as the player's own health bar (see {@link EasedAnimator}), not the mod's older
     *  frame-rate-dependent exponential decay. Entries for players no longer on the team are simply
     *  left to linger rather than actively pruned — team rosters are small, so the memory cost is
     *  negligible, and it avoids having to decide exactly when a teammate should be "forgotten"
     *  (mid-game team swaps, brief disconnects, etc.). */
    private final Map<UUID, EasedAnimator> healthAnimators = new HashMap<>();

    /** Same idea as {@link #healthAnimators}, one independent glide per teammate, for their
     *  Absorption plate row. */
    private final Map<UUID, EasedAnimator> absorptionAnimators = new HashMap<>();

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!HudConfig.enabled || !HudConfig.showTeamHud) {
            return;
        }
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || self.level() == null) {
            return;
        }
        if (SuperbWarfareCompat.isInVehicle(self)) {
            // Same reasoning as PlayerHudOverlay: it stacks directly off that widget's position,
            // which just went stale/hidden this frame, so there's nothing sane to stack above.
            return;
        }
        Team myTeam = self.getTeam();
        if (myTeam == null) {
            return; // not on a vanilla team - nothing to show
        }

        List<AbstractClientPlayer> teammates = new ArrayList<>();
        for (Player p : self.level().players()) {
            if (p == self) {
                continue;
            }
            if (p instanceof AbstractClientPlayer acp && myTeam.equals(p.getTeam())) {
                teammates.add(acp);
            }
        }
        if (teammates.isEmpty()) {
            return;
        }
        teammates.sort(Comparator.comparing(p -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER));
        int count = Math.min(Math.max(1, HudConfig.teamHudMaxPlayers), teammates.size());

        Font font = Minecraft.getInstance().font;
        int lineH = font.lineHeight;
        int headW = HudConfig.teamHudShowHead ? HEAD_SIZE + 5 : 0;
        int barW = HudConfig.teamHudBarWidth;
        int contentW = headW + Math.max(barW, 1);
        int plateH = HudConfig.showTeamAbsorption ? Math.max(2, HudConfig.absorptionHeight) : 0;
        int plateBlockH = plateH > 0 ? plateH + PLATE_GAP : 0;
        int rowContentH = Math.max(HudConfig.teamHudShowHead ? HEAD_SIZE : 0, lineH + 1 + plateBlockH + BAR_H);
        int rowGap = HudConfig.teamHudRowGap;
        int rowStride = rowContentH + rowGap;

        int panelPad = HudConfig.showPanel ? HudPanel.padding() : 0;
        int boxW = contentW + panelPad * 2;
        int boxH = count * rowContentH + (count - 1) * rowGap + panelPad * 2;

        double scale = HudConfig.barScale;
        int scaledBoxH = (int) Math.round(boxH * scale);

        // Stacks directly above the player's own widget - same left edge, its own bottom edge
        // sitting teamHudGroupGap px above the main widget's top edge.
        int x = PlayerHudOverlay.lastFrameX;
        int y = PlayerHudOverlay.lastFrameY - scaledBoxH - HudConfig.teamHudGroupGap;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale((float) scale, (float) scale, 1.0f);

        if (HudConfig.showPanel) {
            HudPanel.draw(graphics, 0, 0, boxW, boxH, HudConfig.panelColor, HudConfig.panelOpacity);
            HudPanel.drawBorder(graphics, 0, 0, boxW, boxH,
                    HudConfig.borderColor, HudConfig.borderOpacity, HudConfig.borderStyle, HudConfig.borderLength, HudConfig.borderOutset);
        }

        int cursorY = panelPad;
        for (int i = 0; i < count; i++) {
            drawRow(graphics, font, teammates.get(i), panelPad, cursorY, headW, barW, plateH, lineH, rowContentH);
            cursorY += rowStride;
        }

        pose.popPose();
    }

    private void drawRow(GuiGraphics g, Font font, AbstractClientPlayer p, int x, int y,
                          int headW, int barW, int plateH, int lineH, int rowH) {
        if (HudConfig.teamHudShowHead) {
            int headY = y + (rowH - HEAD_SIZE) / 2;
            PlayerHudOverlay.renderHead(g, p, x, headY, HEAD_SIZE);
        }

        int textX = x + headW;
        int plateBlockH = plateH > 0 ? plateH + PLATE_GAP : 0;
        int textBlockH = lineH + 1 + plateBlockH + BAR_H;
        int textY = y + (rowH - textBlockH) / 2;
        UUID id = p.getUUID();

        g.drawString(font, p.getGameProfile().getName(), textX, textY, resolveNameColor(p), true);

        int cursorY = textY + lineH + 1;

        if (plateH > 0) {
            // Spectators never show a real (possibly stale/leftover) Absorption reading - the
            // glide target is forced to zero so the bar eases smoothly down to empty exactly like
            // any other Absorption loss, instead of the bar just vanishing or freezing mid-value.
            boolean spectator = isSpectator(p);
            float absorptionRaw = spectator ? 0f : Math.max(0f, p.getAbsorptionAmount());
            EasedAnimator absorbAnimator = absorptionAnimators.computeIfAbsent(id,
                    k -> new EasedAnimator(220f, EasedAnimator.Easing.QUINTIC_OUT));
            absorbAnimator.setTarget(absorptionRaw);
            float animatedAbsorption = absorbAnimator.update();
            if (animatedAbsorption > 0.01f) {
                boolean customColor = HudConfig.absorptionColorMode == AbsorptionColorMode.CUSTOM;
                BarRender.plates(g, textX, cursorY, barW, plateH, animatedAbsorption,
                        HudConfig.segmentCount, HudConfig.pointsPerSegment, customColor, HudConfig.absorptionCustomColor);
            }
            cursorY += plateBlockH;
        }

        float maxHealth = Math.max(1.0f, p.getMaxHealth());
        float target = Math.max(0.0f, Math.min(1.0f, p.getHealth() / maxHealth));
        EasedAnimator animator = healthAnimators.computeIfAbsent(id,
                k -> new EasedAnimator(280f, EasedAnimator.Easing.QUINTIC_OUT));
        animator.setTarget(target);
        float current = animator.update();

        double percent = 100.0 * target;
        int barColor = percent <= HudConfig.lowHealthThresholdPercent ? BarRender.HEALTH_LOW : BarRender.HEALTH;
        BarRender.bar(g, textX, cursorY, barW, BAR_H, current, barColor);
    }

    /**
     * Whether {@code p} is currently in Spectator mode. Unlike {@link Player#isSpectator()} (which
     * only reflects the local player's own state), another player's gamemode is only known
     * client-side through the tab-list's {@link PlayerInfo} — the same source vanilla's own player
     * list overlay uses to italicise spectator names.
     */
    private boolean isSpectator(AbstractClientPlayer p) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        PlayerInfo info = connection.getPlayerInfo(p.getUUID());
        return info != null && info.getGameMode() == GameType.SPECTATOR;
    }

    private int resolveNameColor(AbstractClientPlayer p) {
        if (HudConfig.teamHudUseTeamColor) {
            Team team = p.getTeam();
            ChatFormatting fmt = team != null ? team.getColor() : null;
            if (fmt != null && fmt.getColor() != null) {
                return 0xFF000000 | fmt.getColor();
            }
        }
        return HudConfig.teamHudNameColor | 0xFF000000;
    }
}
