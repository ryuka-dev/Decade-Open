# TaCZ（德卡修改版）

德卡（Decade）Minecraft 服务器使用的 [Timeless and Classics Zero](https://github.com/MCModderAnchor/TACZ)（永恒枪械工坊：零）修改版。

| 项 | 值 |
|---|---|
| 上游 | https://github.com/MCModderAnchor/TACZ ，`1.20.1` 分支 |
| 基于 | 标签 `1.1.8-hotfix`（提交 `b43eb84c38e9768d8e73c8b14f0b845669704b38`，2026-05-25），保留上游历史 |
| 版本号 | `1.1.8-hotfix-decade.N`，每次修改递增 N |
| 许可证 | 与上游相同：代码 GPL-3.0（见 `LICENSE`），资源 CC BY-NC-ND 4.0 |
| 构建 | 沿用上游的 Gradle 7.5.1。**Gradle 7.5.1 不能在 Java 21 上运行**，要用 JDK 17 启动：`JAVA_HOME=<JDK 17> ./gradlew build`。**JDK 17 的小版本会改变编译结果**：Oracle JDK 17.0.11 构建出的 jar 与前几版逐条目一致；Microsoft Build of OpenJDK 17.0.15 会让二十多个没改过的类字节码不同，逐条目核对就看不出这次改了什么。**不要 `clean`**，它连 `build/libs/` 里的旧版本 jar 一起删掉，核对时就没有对照了。产物在 `build/libs/`：`tacz-1.20.1-<版本>.jar`（带内嵌依赖，发给玩家的就是它）与默认枪包 `tacz_default_gun-1.1.8-hotfix.zip` |

## 源码

公开仓库 https://github.com/ryuka-dev/Decade-Open 的 `tacz/` 目录。每个发给玩家的版本都有标签 `tacz/<版本>`，标签里的内容就是那个 jar 的完整源码。

## 安装

- jar 放进 `mods/`，替换平台上的原版 TaCZ（两个不能同时装）
- **默认枪包 zip 放进游戏目录的 `tacz_default/`，不是 `tacz/`**。TaCZ 启动时把它解压到 `tacz/tacz_default_gun/`；zip 若直接放进 `tacz/`，会作为第二份 `tacz` 命名空间的枪包被加载
- `tacz/tacz-pre.toml` 的 `DefaultPackDebug` 保持默认的 `false`，否则不会解压

## 设计边界

这个 jar 发给玩家，所以**只改机制与表现**：客户端预测、HUD、修 bug、默认枪包的分发方式。服务器的规则与数值不写在这里；需要客户端知道的限制，由服务端经下文的 `GunUseRestriction` 下发，客户端只照着做。

## 我们改了什么

| 版本 | 问题 | 原因 | 修改 |
|---|---|---|---|
| decade.1 | — | — | 只改版本号，并让带内嵌依赖的 jar 用不带 `-all` 后缀的名字 |
| decade.2 | 服务器不允许用枪时（例如不在指定的快捷栏格、倒地），按开火、换弹、枪托，客户端仍先播动作（开火有空仓声，换弹播完整动画），随后被服务端拒绝 | 规则只在服务端，客户端不知道此刻不能用。TaCZ 虽有客户端事件，但要监听就得引用 TaCZ 的类；换弹与枪托还是先上状态锁再发事件，取消后要等约 250 ms 锁才放开 | 新增 `com.tacz.guns.restriction`：服务端 API `GunUseRestriction.sendSlots` / `sendSuspended` 经 TaCZ 自己的通道下发，客户端 `ClientGunUseRestriction` 只存收到的值、登出清空、没收到时不限制。`LocalPlayerShoot.shoot()`、`LocalPlayerReload.reload()`、`LocalPlayerMelee.melee()` 在认出枪之后、上锁之前各问一次，被拒就什么都不播。消息注册在通道最后（上游消息的编号不变），通道版本改为 `1.0.5-decade.1`，混用官方 TaCZ 会在登录时被拒 |
| decade.3 | 右下角的枪械 HUD 只显示手里那把，看不到另一把可用的枪还装着多少发 | 上游 HUD 只跟主手 | 新增 `client/gui/overlay/WeaponSlotHudOverlay`：服务端下发了可用格数（`ClientGunUseRestriction.usableSlots()`）后，这些格里任一格有枪就显示，每格一行、位置固定（空格留位）。选中的一行亮、弹匣数 2 倍字号、带备弹与射击模式，低弹时除了变红行尾还会脉动；另一行暗、只有图标与弹匣数；每行印按键数字。缓存按格子分开；背包供弹的枪把背包弹数写回弹匣的副作用只对手里那把保留，与上游一致。上游 `GunHudOverlay.render()` 开头加一行，在这种情况下让位；`ClientSetupEvent` 加一行注册。没收到格数时与上游完全相同。样式是占位的 |
| decade.4 | 默认枪包占 jar 的 51 MB（共 57 MB），TaCZ 的任何一次代码更新都要每个玩家重下 | 上游把默认枪包放在 jar 里，启动时解压到游戏目录的 `tacz/` | `build.gradle`：jar 排除 `assets/tacz/custom/`（jar 降到 5.7 MB）；新任务 `defaultGunPackZip` 把源码里那份原样打成 `tacz_default_gun-1.1.8-hotfix.zip`，固定时间戳与顺序，内容不变时逐字节相同（核对过：3322 个文件与原先 jar 里的完全一致，重打两次哈希相同）。zip 的版本跟上游枪包走，只有合并上游改了枪包才变。它是 CC BY-NC-ND，**必须原样分发**，不能往里加文件或改内容。这一版直接把 zip 当枪包放进 `tacz/`，有缺陷，由 decade.5 修正，没有对外发布 |
| decade.5 | decade.4 让客户端的排除卡壳动画消失：GunDB（`mod.cdv.gdb.mixin.GunPackLoaderMixin`）挂在 `GunPackLoader.discoverExtensions` 里 `scanExtensions` 之前，每次都把它的 `unjam` 动画并进 `tacz/tacz_default_gun/assets/tacz/animations/` 的 47 个文件（`DefaultPackDebug` 为 `true` 时不做）。枪包成了只读 zip，它写不进去 | 下游 mod 依赖「默认枪包是解压出来、可改的目录」 | zip 改放游戏目录的 `tacz_default/`，启动时照上游从 jar 解压的做法、在同一个位置解压到 `tacz/tacz_default_gun/`：新类 `com.tacz.guns.resource.DefaultPackArchive` 找 `tacz_default_gun-*.zip`，经 `GetJarResources` 新加的 `copyZipDirectory` 走上游原有的导出（`jar:` URL，同样按指纹只在 zip 变了时重新解压、旧目录先备份到 `tacz_backup/`）。`GunPackLoader` 加一行调用，放在 GunDB 注入点之前。核对过：解压出的 3322 个文件里，只有 GunDB 并过 `unjam` 的 47 个与 zip 不同，和拆包之前完全一样；实测枪的模型、贴图、声音、提示正常，卡壳后的排除卡壳动画正常 |
| decade.6 | 只在客户端重建玩家实体后（例如换皮肤的 mod 或插件发来的 respawn 包）不能开火（上游 #720） | 服务端只在实体重新进入世界时全量同步 `SyncedEntityData`，客户端单方面重建的新实体退回默认值（切枪与近战冷却为 -1），基准时间戳也丢了，开火包的时间戳校验失败 | 原样并入上游 `1.20.1-dev-temp` 分支的 `c7818710`（保留原作者）：`RefreshClonePlayerDataEvent` 在 `Clone` 时把旧实体的同步数据与基准时间戳复制给新实体；真正的死亡重生、换维度随后仍由服务端全量同步覆盖。截至 2026-09-24，这是该分支比 `1.1.8-hotfix` 多出的唯一一个提交。实测死亡重生、换维度后都能正常开火（触发 #720 的场景本身没有复现条件） |
| decade.7 | 玩家在模组列表里找不到修改版的源码 | 上游的 `displayURL` 为空 | `mods.toml` 的 `displayURL` 指向公开仓库 |
| decade.8 | 枪卡壳后要自己想起按检视键才能排除；新手不知道，以为枪坏了 | 卡壳是 GunDB 的机制：它给枪打上 `Jammed` 标记，排除靠拦截 `InspectKey` 的两个检视入口。TaCZ 只有自动换弹，没有对应的自动排除 | 新类 `client/input/AutoUnjam`：客户端每 tick 看主手的枪有没有 `Jammed`，有就经 `InspectKey.onInspectControllerPress` 替玩家按一次检视，GunDB 照它自己的流程播排除动画、发包。只按名字读标记，不引用 GunDB 的类（它是 ARR）；没装 GunDB 就没有枪会被标记，什么都不做。换弹、切枪、拉栓、近战、状态锁期间不按（这些时候手动检视也会被拒或被打断）；枪的动画状态机还没建好时也不按：它在枪第一次画到屏幕上时才初始化，比切到这一格晚一帧以上，之前按的会被丢掉，而 GunDB 每按一次都先放一遍排除音效。同一次卡壳只按一次（最慢的 M249 排除动画 9 秒）；GunDB 的排除状态一开始就上状态锁、且就在这次按下里完成，按完锁没上就是被丢掉了（例如正在检视），1 秒后再按；没有排除动画的枪由 GunDB 播检视、服务端计时解除，不上锁，10 秒后才再按；收枪再拿出来算新的一次。（第一版没有等状态机，空手切到卡壳的枪时第一次按被丢掉，要等 10 秒，实测发现）。开关 `KeyConfig.AUTO_UNJAM`（`tacz-client.toml` 的 `[key] AutoUnjam`），**默认开**，与自动换弹并列在 TaCZ 的设置界面里，中英繁三种语言有文字。**排除之后的后摇**另由新类 `client/input/UnjamPrediction` 处理，手动排除与自动排除都适用、不受开关影响：① GunDB 在排除动画播完时发解除包，客户端却要等服务端回包才清掉 `Jammed`，其间开火被它的客户端检查拦成空仓，动画结束后枪要呆一个往返加最多一个 tick（实测 100~150 ms，延迟 40~90 ms）。现在动画一停（GunDB 只在发包的那一步停它）就清掉客户端的标记；服务端不用改：解除包先于之后的任何开火发出、走同一条连接，服务端按到达顺序处理。收枪也会停掉这个动画但不发包，所以换了格子就不预测。② 换弹能在动画结束前开火、开火打断动画尾巴，排除却必须播完。现在在排除动画的最后 200 ms 里按住开火，就给 GunDB 的状态机它自己在结尾给的输入 `unjam_finished`，GunDB 照常发包、退出排除状态，与动画播完相同；不按开火时照常播完。实测按住开火时第一枪比动画结束早 200 ms。没有排除动画、靠检视加服务端计时排除的枪两样都不做 |

## 已知的上游问题，不修

`GunPackLoader` 合并出的包里，`listResources` 是后面的枪包赢、`getResource` 是前面的赢，而顺序来自不排序的 `Files.newDirectoryStream`。我们不让枪包互相覆盖同名文件，所以碰不到它；哪天要让枪包盖枪包，先修这里。

曾怀疑、查证后不成立的：「`AnimateGeoItemRenderer.tryExit` 把时长当成时间点传给 `setExitingTime`」。`setExitingTime(keepTime)` 自己加上当前时间，两个调用方传的都是毫秒时长（`putAwayTime + 50`），读取端比较 `getExitingTime() < 当前时间`，三处一致；这个写法来自上游 `e94ec69b`（2025-03-01），早于 `1.1.8-hotfix`。

## 维护

- 对上游文件的改动都带 `// Decade:` 注释，全文搜索就能找齐；我们自己的类在 `com.tacz.guns.restriction`、`com.tacz.guns.resource.DefaultPackArchive`、`com.tacz.guns.client.input.AutoUnjam` 与 `com.tacz.guns.client.input.UnjamPrediction`
- **引入时的核对**（2026-09-24）：不改任何东西构建一次，与 Modrinth 上的 `tacz-1.20.1-1.1.8-hotfix.jar` 逐个文件比对，4354 个文件一一对应，文本连行尾都相同。只有两处不同，都不影响行为：`GunSoundInstance$TaczSound.class` 多一个编译器生成的桥接方法 `m_213718_`（只是转调父类同名方法）；内嵌的 `simplebedrockmodel-2.2.2` jar 只有 `MANIFEST.MF`（构建时间）不同。因此这份源码就是 1.1.8-hotfix
- **跟进上游**：合并上游新标签后，逐条检查上表的修改是否仍然需要、是否已被上游修掉。lrtactical 与 GunDB 都依赖 TaCZ，跟进之后要确认它们仍然兼容。`AutoUnjam` 与 `UnjamPrediction` 依赖 GunDB 的几个约定：卡壳标记叫 `Jammed`；排除挂在 `InspectKey.onInspectControllerPress` 里对 `inspect()` 的调用上；排除动画叫 `unjam`，排除状态在输入 `unjam_finished` 时发解除包并停掉动画、只有这一个出口；GunDB 升级后先核对这几处
