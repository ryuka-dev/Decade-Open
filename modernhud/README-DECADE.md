# modernhud（德卡修改版）

**English summary.** A fork of [TACZ: Modern Hud](https://github.com/AquaticShelter/ModernHudZero) 1.0.1 (Forge 1.20.1, GPL-3.0, by AS12 / Lanixius) used by the Decade server for its bottom-left status and bottom-right weapon panel. It is client-only here: the upstream server half (hit/break detection, its network channel) is removed. The plate row reads Decade's armor plates handed over by an inter-mod message (`plates`, plain Java types) instead of vanilla Absorption, with the pouch drawn after the HP number; the weapon panel follows the server's weapon slots (from Decade's TaCZ fork) instead of the last gun held, and reports its height to TaCZ so subtitles move above it. HUD words moved to language files with a Chinese translation, and a jammed gun shows its own alert. Every edit to an upstream file is marked `// Decade:`; the change table below is in Chinese.

德卡服务器使用的 TACZ: Modern Hud 修改版。它画常驻 HUD 的两块：左下的个人状态（头像、分段插板条、细血条与「48/60」）与右下的武器面板。规则与数值不在这里，它只画别的 mod 交给它的数：玩家拿到的 jar 里只有表现层。

| 项 | 值 |
|---|---|
| 上游 | https://github.com/AquaticShelter/ModernHudZero （CurseForge `tacz-modern-hud`，项目 1698352） |
| 作者 | CurseForge 上是 AS12，jar 的 mods.toml 写的是 Lanixius；署名两个都写 |
| 基于 | 上游仓库 `a069a8aa`。仓库里只有 LICENSE 与源码包 `modernhud-v1_0_1-release.zip`，`git subtree` 引入后在下一个提交里原样解开。1.0.2 已发布但没公开源码，所以基于 1.0.1（1.0.2 多了枪管过热的显示） |
| 版本号 | `1.0.1-decade.N`，每次修改递增 N |
| 许可证 | **GPL-3.0**：仓库的 LICENSE 与官方 1.0.1 jar 的 mods.toml 都是 GPL3（源码包里的 mods.toml 写 MIT、CurseForge 详情栏写 LGPLv3，按最严的算）。源码随公开仓库 Decade-Open 提供（`tools/publish.tsv`） |
| 构建 | 沿用上游的 Gradle：本目录 `./gradlew build -x test`，JDK 17，产物在 `build/libs/`。`tools/deploy.sh` 会构建它 |

## 引入时的核对

不改任何东西构建了一次，与 CurseForge 上的 `modernhud-1.0.1.jar`（文件 8908914）逐个文件比对：62 个文件一一对应，**全部类文件与资源逐字节相同**，只有 mods.toml 的 `license` 一行不同（官方 jar 是 `GPL3`，源码包是 `MIT`）。因此这份源码就是 1.0.1。

上游带的 `gradle-wrapper.jar` 对不上 Gradle 官方的校验值，换成了与我们的 lrtactical 修改版相同的那份；`gradlew` 两个脚本同样换掉，`gradle-wrapper.properties`（8.1.1）照旧。

## 和德卡的接口

**德卡自己的 mod（`decade_hud`，不是 GPL）不引用这里的类**，否则它也会变成 GPL。数据经 Forge 的跨 mod 消息交过来，只用 Java 自带的类型（同 TaCZ 修改版的 `GunTextureOverrideMessages`）：

| 消息 | 谁发 | 内容 | 在这里 |
|---|---|---|---|
| `plates` | `decade_hud` 的 `ModernHudBridge` | `Supplier<float[]>`：插板值、上限、一块的点数、插板袋这场还能换几块（负数为不限）；没有插板时返回 null | `client/PlateSource`。有它时插板行读它，不读原版的伤害吸收 |
| `rig` | `decade_hud` 的 `ModernHudBridge` | `Supplier<Object[]>`：身上装置的充能、充满所需、充满的时刻（毫秒）、发射键的名字、图标（ItemStack）、颜色（RGB）；没戴装置时返回 null | `client/RigSource`。有值时在左下面板上方画一行：图标、10 格（与血条同宽）、没满写百分比、满了换成装置的颜色并画出按键 |

声音：插板受击与碎板的两个音效（`modernhud:armor_hit`、`modernhud:absorption_break`）只在 `sounds.json` 里，不注册成声音事件；由德卡的服务端 mod 按名字播给玩家。

## 我们改了什么

| 版本 | 修改 |
|---|---|
| `1.0.1-decade.1` | **只装客户端**：删掉上游服务端的那一半（`CommonEvents`、网络通道 `network/`、`CommonConfig`）与 `SoundRegistry`；`ClientFeedback` 改为按名字播放（它与受击图标现在没有人触发）。**插板接德卡的数据**（`PlateSource`，有它时左下改走 `decadeLayout`）：一格一块，空的格子也画；插板与血条之间 1 像素，两行一共 7 像素，正好是原版数字的高度，血量写成整数「48/60」（新格式 `WHOLE`）盖住两行；交战中插板袋画在数字后面，一块一格，用掉的留半透明黑的空格；补袋时（`plates` 的第六个数，0 到 1）空格从下往上涨一层淡色，补满那一刻变回实心并闪一下。插板一排每格的起止按整条宽度算，最后一格和血条对齐（上游按整数等分，4 块时短 2 像素）。血条信号红、残血不换色（数字变红、柔光呼吸），中毒、凋零、冻伤时照原版心的顺序换色；插板骨白；创造模式不画（同原版的心）；按 T 打开聊天时 0.15 秒淡出（聊天的输入框与频道条会盖住这里）。**右下武器面板按武器槽**：TaCZ 是德卡的修改版、服务端说了哪几格是武器槽时（`TaczCompat.weaponSlots`，反射读 `ClientGunUseRestriction.usableSlots`），主框是手里那个武器槽的枪、副框是另一个武器槽里的枪（按格子取，两把同型号也分得开），换槽就算换枪（上滑与边框闪照旧）；手里不是武器槽里的枪时，武器槽里的枪暗着并排；读不到武器槽时照上游的"上一把拿过的枪"。面板占了右下角多高经跨 mod 消息 `hud_bottom_right_height` 告诉 TaCZ，德卡的 TaCZ 修改版把字幕挪到它上面。射击模式与弹药提示的字（FIRE TYPE、RELOAD 这些）从代码里的英文挪进语言文件，加了 `zh_cn.json`；枪卡壳时（Gun Durability 打在枪上的 `Jammed` 标记，只按名字读）提示「卡壳」，排在换弹之前。别的 mod 可以经跨 mod 消息 `alert`（每帧读的 `Supplier<String>`，没有时返回 null）让中央提示显示它给的字，样子同 NO AMMO、排在弹药提示前面、不管手里拿的是什么（`ExternalAlert`；德卡用它提示插板袋用完）。默认值：显示头像、不显示名字、柔光开、残血线三分之一、队伍列表与受击图标关、武器面板用这个 mod 的（`hudSource = MODERNHUD`，上游默认）。源码按 UTF-8 编译 |
| `1.0.1-decade.2` | 左下面板上方加一行装置的充能（跨 mod 消息 `rig`，`client/RigSource`）：充满时闪一下、之后慢慢呼吸，按键名写在键帽里；跟面板一起在打开聊天时淡出。`PlayerHudOverlay.render` 里加一处调用 |
| `1.0.1-decade.3` | 充能一行的条朝新值缓慢爬升（约 0.4 秒追上大半），每一次增长都看得见；放出去清零或换了装置时直接落下，充满时直接补满，和就绪的闪光同一刻。百分比跟着画出来的值走 |

## 已知但未改

- `ClientFeedback`、`FeedbackIconOverlay` 留着但没人调用（上游由服务端的包触发）
- 上游的 `WeaponConfig` 会改 TaCZ 的 `GUN_HUD_ENABLE` 并存进玩家的 `tacz-client.toml`
