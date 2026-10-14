# Modern HUD (Forge 1.20.1)

A lightweight, FPS-shooter-inspired HUD overhaul built entirely on vanilla
mechanics - no new items, blocks, or ores.

## Features

### 1. Personal status widget (client-side)
Head icon, name, a segmented Absorption bar, a health bar, an optional
Armor bar, and an HP readout (`CLASSIC` "19.1/20.0" or `PERCENT` "91%",
true decimal precision). Anchored to any screen corner, fully scalable.

### 2. Private hit/break feedback
A hit that lands on active Absorption plays a sound for the attacker and
victim only (never broadcast to anyone else nearby); fully depleting it
also flashes a positionable icon for the attacker. Needs the mod on the
server too, to work for entities other than yourself.

### 3. Team HUD (client-side)
One compact row per vanilla scoreboard teammate - head, name, and a
smoothly-animated health bar - stacked above the personal widget. Capped
at a configurable row count.

### 4. TACZ weapon widget (client-side, soft dependency)
Only active if TACZ is installed. A fixed-position Call-of-Duty-style
icon, fire-mode icons, and mag/reserve ammo readout for your held gun,
plus a small secondary-weapon slot for the last different gun you were
holding, and a pulsing centre-screen RELOAD/NO AMMO/LOW AMMO alert.
TACZ's own built-in gun HUD is turned off automatically so the two never
render on top of each other. A keybind (unbound by default) lets you
cycle live between this widget and TACZ's own instead.

The weapon widget and the personal widget both skip their own rendering
while in Spectator mode (each is a separate, independent overlay - this
doesn't touch vanilla's own HUD/hotbar rendering at all, and the team
HUD / feedback icon are unaffected).

## Project layout

```
src/main/java/cc/xypp/modernhud/
  ModernHud.java                 - mod entry point (registers configs + networking)
  CommonEvents.java               - server-side hit/break detection
  SoundRegistry.java               - registers hit/break sounds
  network/
    ModNetworking.java
    ShieldFeedbackPacket.java
  config/
    HudConfig.java                - CLIENT: personal widget + team HUD + feedback icon
    WeaponConfig.java              - CLIENT: TACZ weapon widget + ammo alerts
    CommonConfig.java              - COMMON: hit/break detection toggles
    HudPosition.java, NamePosition.java, BarTextPosition.java, HpTextFormat.java,
    PanelBorderStyle.java, AbsorptionColorMode.java, WeaponHudSource.java, ColorConfig.java
  client/
    ClientEvents.java              - registers overlays + client setup
    ModKeybinds.java                - HUD-source cycle keybind
    PlayerHudOverlay.java           - head/name/absorption/health/armor bars + readouts
    TeamHudOverlay.java             - teammate health rows
    WeaponHudOverlay.java           - TACZ ammo counter widget
    AmmoAlertOverlay.java           - centre-screen RELOAD/LOW AMMO text
    FeedbackIconOverlay.java, ClientFeedback.java, BarTextures.java, GlowEffect.java,
    EasedAnimator.java, HudPanel.java
    compat/
      TaczCompat.java, SuperbWarfareCompat.java
    gui/
      HudEditorScreen.java          - drag-to-reposition editor for draggable elements

src/main/resources/
  META-INF/mods.toml
  pack.mcmeta
  assets/modernhud/
    lang/en_us.json
    textures/gui/  (bar_*.png, health_*.png, icon_*.png)
    textures/hud/  (fire_mode_*.png)
```

## Config

Three `.toml` files, generated on first launch in `config/`:

- `modernhud-hud.toml` - personal widget, team HUD, feedback icon.
- `modernhud-weapon.toml` - TACZ weapon widget + ammo alerts.
- `modernhud-common.toml` - server-side hit/break detection toggles.

This mod doesn't ship its own settings browser; a config-screen mod
(e.g. Forge Config API Port) will render one for it automatically if
installed. Draggable elements (feedback icon, personal widget where
enabled) can also be repositioned via Controls > Modern HUD > Open HUD
Editor.

## Building

Requires **JDK 17** (`java -version` must show `17.x`). The Gradle
Wrapper is included:

```
./gradlew build      # Linux/macOS
.\gradlew.bat build  # Windows
```

First run downloads Gradle, ForgeGradle, and Forge for 1.20.1. Output:
`build/libs/modernhud-<version>.jar`. Drop it into `mods` - client-side
for the personal/team/weapon widgets, server-side too if you want
hit/break feedback to work against other players.
