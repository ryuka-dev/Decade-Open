# tooltipoverhaul（德卡修改版）

**English summary.** A fork of [Tooltip Overhaul](https://github.com/Xylonity/Tooltip-Overhaul) 2.0.5 (Forge 1.20.1, GPL-3.0, by Xylonity and ModderG) used by the Decade server so that any tooltip can be pinned and followed into Decade's in-game handbook. Pinning is allowed on every screen (upstream: container-like screens only), text tooltips without an item can be pinned, the pin key defaults to F in screens only and stays quiet with Ctrl held or while one of Decade's own text fields has the keys, a click on a pinned tooltip's body (pressed and released in place) opens its handbook entry while pressing and moving drags it, and a tooltip that has an entry shows a small book in its top-right corner until it is pinned. Decade's client mod hands the fork three hooks by Forge inter-mod messages of plain Java and Minecraft types (it does not link these classes). Every edit to an upstream file is marked `// Decade:`; the change table below is in Chinese.

德卡服务器使用的 Tooltip Overhaul 修改版。它管所有悬浮窗的样式，德卡改它是为了「任何悬浮窗按键固定、点击跳到手册」。

| 项 | 值 |
|---|---|
| 上游 | https://github.com/Xylonity/Tooltip-Overhaul （CurseForge / Modrinth `tooltip-overhaul`） |
| 作者 | Xylonity、ModderG |
| 基于 | 上游分支 `1.20.1-4/rewrite` 的 `4d99f9e9`（2.0.5，上游没打标签），`git subtree` 引入、保留历史 |
| 版本号 | `2.0.5-decade.N`，每次修改递增 N |
| 许可证 | **GPL-3.0**（代码）；美术另有 `LICENSE_ASSETS`。源码随公开仓库 Decade-Open 提供（`tools/publish.tsv`） |
| 构建 | 沿用上游的多加载器 Gradle：本目录 `./gradlew :forge:build -x test`，JDK 17，产物 `forge/build/libs/tooltipoverhaul-forge-1.20.1-2.0.5-decade.N.jar`。只用 Forge 那一半 |

## 和德卡的接口

**德卡自己的 mod（`decade_hud`，不是 GPL）不引用这里的类**，否则它也会变成 GPL（同德卡的 Modern Hud 修改版）。`decade_hud` 的 `TooltipBridge` 在 `InterModEnqueueEvent` 里发三样，这里在 `InterModProcessEvent` 里收（`TooltipOverhaulForge`），放进 `client/decade/DecadeHooks`；没有 `decade_hud` 时三样都是空的，行为照上游：

| 消息 | 内容 | 用在哪 |
|---|---|---|
| `entry` | `Function<ItemStack, String>`：这块悬浮窗对应手册的哪一条（有物品按物品；空物品时 hud 按它刚收集到的文字悬浮窗的标题认），没有就 null | 固定时记下（点击打开它）；没固定的悬浮窗有条目就画角上小书 |
| `nouns` | `Function<String, List<String>>`：一行字里哪几段是手册的名词，每段「起,止,条目」 | 固定后名词划线与点名词 |
| `typing` | `BooleanSupplier`：hud 自己的输入框是否在打字（它们不是原版 `EditBox`），以及告示牌、书、铁砧 | 打字时固定键不响应 |
| `open` | `Consumer<String>`：在手册里打开这一条 | 单击固定的悬浮窗 |

## 我们改了什么

| 版本 | 修改 |
|---|---|
| `2.0.5-decade.1` | **所有界面都能固定**（`PinnedTooltipState.allowed`：上游只认容器类、JEI、EMI、FTB；德卡自己的界面都直接继承 `Screen`），自己的设置与编辑界面照旧不行。**没有物品的文字悬浮窗也能固定**：捕获时不再要求物品，固定的那份照存下的组件画，不按物品每帧重建。**固定键默认 F**（上游未绑定），只在界面里生效（`KeyConflictContext.GUI`：世界里 F 是「回应」与 TaCZ 的「交互」），**按着 Ctrl 时不响应**（Ctrl+F 留给 JEI），hud 说在打字时也不响应；老玩家存着的「未绑定」由 `decade_client` 的 `KeyMigration` 挪到 F 一次。**单击跳转、按住拖动**：在固定的悬浮窗正文上原地按下松开算单击，打开它的手册条目；按下后移动 3 像素以上才算拖动（上游只能拖顶上那条标记，正文按下什么也不做）；顶上标记最右那块照旧是取消固定。**角上小书**：没固定的主悬浮窗有手册条目时，右上角里面画原版「书与笔」缩到 8×8，悬浮窗为它加宽 12 像素（`client/decade/CornerBook`）；固定后不画（固定后只划线）。**中文**：原来写在 `decade_client` 里给 TO 补的按键名挪进这里的 `zh_cn.json`（不要两处都有，同名的语言文件谁盖谁看加载顺序），固定标记写「已固定 · 拖动移动 · 单击查手册」。版本号 `2.0.5-decade.1` |
| `2.0.5-decade.2` | 固定标记的中文改成「已固定 · 拖动移动 · 单击查看手册」 |
| `2.0.5-decade.3` | **固定后名词划线**：固定的悬浮窗里，正文每一行画完后经 `nouns` 问 hud 哪几段是手册的名词，名词下面画虚线；鼠标指着的名词变白、线变实；单击名词打开它的条目，点别处照旧打开这件东西的（`client/decade/DecadeNouns`，名词的框随每块固定的悬浮窗记着）。标题行不划 |
| `2.0.5-decade.4` | 角上小书画在悬浮窗所有层之上（decade.1–3 画在 z 400，被背景盖住，看不见） |
| `2.0.5-decade.5` | **原地固定**（负责人 2026-10-19：「看起来就像是把鼠标指向的悬浮窗给固定在原位置了」）：固定的悬浮窗出现在原来那块的位置、高度上限相同，去掉了上游顶上的「已固定」标记条（取消固定：指着它再按一次 F；拖动：按住正文移动），角上小书固定后照样画；刚固定的那块原悬浮窗在鼠标还指着同一件东西时不再画（连它的消失动画一起收掉），移开一帧以后恢复。构建按 UTF-8 编译（`buildSrc` 的 `multiloader-common.gradle`；中文 Windows 默认 GBK） |
| `2.0.5-decade.6` | **内容与原来那块一样**（负责人实机：固定后 TaCZ 枪械那一整块不见了）：上游每帧按自己的办法重拼固定悬浮窗的内容，只收文字行、只认原版收纳袋的图块，TaCZ 的枪械块（描述、弹药、数值、改装提示、枪包名）是一个图块，被丢掉；改成经平台钩子 `TooltipPlatform.itemTooltip` 走和悬停时同一条路线（Forge：`ForgeHooksClient.gatherTooltipComponents`，按捕获时的鼠标 x 折行）。**照原界面的缩放画**：市场板把自己的悬浮窗缩小了画，固定那块记下捕获时的缩放与平移，照样画，鼠标换算到它自己的坐标。**顶上标记条恢复**（负责人：去掉后就没法关掉）：右端「x」取消固定，按住拖动；指着按 F 照旧取消固定。**没有可跳转的不能固定**（负责人）：自己没有手册条目、正文里也没有名词的悬浮窗按 F 不固定（键照旧被拿走，免得落到界面上把格子里的东西换到副手）。**物品名划线**：固定后物品本身有条目时标题整行划虚线，指着变白变实线。**「按 F 以固定」**：能固定的主悬浮窗在角上小书同一行、紧挨小书左边写一行灰字（没有小书时靠右角），键名照绑定，悬浮窗为它加宽 |
| `2.0.5-decade.7` | **清掉上游的默认样式**（负责人 2026-10-19）：自带的 `custom_frames.json` 清成空表（上游按物品写死的十条：附魔书、金苹果、下界合金、鞘翅、三叉戟，以及给没装的 mod 的几条；玩家本机没改过的那份照 TO 的合并规矩跟着变空），**不再按原版稀有度给边框上色**（`ColorUtils.getColorsPerRarity` 一律返回普通的灰；附魔书原来是「罕见」的金框）。德卡的外框规则不在这里：精良、杰作的外框写在 `decade_hud` 的资源 `assets/decade_hud/tooltipoverhaul/custom_frames.json`，按物品的 NBT `decade_quality` 与数据包的物品标签 `decade:quality/*` 选，是数据。**档位的图标小框**：跨 mod 消息 `grade`（hud 按物品答民用、军用的颜色），有档位时图标小框的框线用那个颜色、底色往那个颜色偏一点（`ColorUtils.getIconBorderColor`、`getIconBackgroundColor`），和外框是哪条规则无关 |

## 已知但未改

- 上游的 Fabric 一半与 `common` 里给 Fabric 用的代码原样留着，不构建
