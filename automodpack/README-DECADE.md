# AutoModpack（德卡修改版）

德卡（Decade）Minecraft 服务器使用的 [AutoModpack](https://github.com/Skidamek/AutoModpack) 修改版：玩家每次启动游戏、加载 mod 之前，先把整合包与服务器的版本对齐。

| 项 | 值 |
|---|---|
| 上游 | https://github.com/Skidamek/AutoModpack |
| 基于 | 标签 `v4.0.6`（`git subtree` 引入，保留上游历史） |
| 版本号 | `4.0.6-decade.N`，每次修改递增 N |
| 许可证 | LGPL-3.0（见 `LICENSE`），修改版同样是 LGPL-3.0；随附的 zstd 与 zstd-jni 许可证见 `LICENSES/` |
| 只用到 | Forge 1.20.1（`versions/1.20.1-forge` 加 `loader/forge/fml47`）。其余加载器与游戏版本的代码原样保留、不构建，也不保证能用 |
| 构建 | 在本目录执行 `./gradlew 1.20.1-forge:build`，产物是 `merged/automodpack-mc1.20.1-forge-<版本>.jar`。需要 JDK 21；第一次构建要下载 Gradle、Minecraft 与 Forge，约十分钟。**改了版本号先执行 `./gradlew 1.20.1-forge:clean loader-forge-fml47:clean loader-core:clean :core:clean`**：合并 jar 的任务按文件名在 `build/libs/` 里找产物，新旧两个版本同时在就会失败（`Array contains more than one matching element`），只改版本号时它又会被判为已是最新而跳过。单元测试：`./gradlew :core:test` |

## 源码

公开仓库 https://github.com/ryuka-dev/Decade-Open 的 `automodpack/` 目录。每个发给玩家的版本都有标签 `automodpack/<版本>`，标签里的内容就是那个 jar 的完整源码。

## 构建可复现

改动前后要能逐条目比对 jar，确认只改了打算改的地方，所以构建结果必须与上游的发布一致。在 `4.0.6-decade.1` 上核对过，与 Modrinth 上的官方 `4.0.6` 相比：

- 外层 jar 的 2817 个条目里，不同的只有 `META-INF/mods.toml`（版本号与 `displayURL`）和内嵌的 `automodpack-mod.jar`
- 内嵌 jar 的 125 个条目里，不同的只有 `mods.toml` 与 `neoforge.mods.toml`（版本号，前者还有 `displayURL`），以及构建时生成的 `automodpack-main.mixins.json` 与它的 refmap：这两个按 JSON 解析后完全相同，只是在 Windows 上生成的格式不同

为此做了两件事：

- `.gitattributes` 让文件按上游仓库里的字节原样检出。上游在 Linux 上构建，资源文件是 LF；在 Windows 上检出成 CRLF，语言文件等 25 个条目就会与发布不同
- 上游的 `settings.gradle.kts` 写的是 Stonecutter `0.9+`，构建时拉到哪个版本就用哪个。固定为官方 `4.0.6` 发布时用的 `0.9.7`

## 我们改了什么

| 版本 | 问题 | 原因 | 修改 |
|---|---|---|---|
| decade.1 | — | — | 版本号改为 `4.0.6-decade.1`；Stonecutter 固定为 `0.9.7`、文件按原样检出（见「构建可复现」）；`mods.toml` 的 `displayURL` 指向公开仓库，玩家能在模组列表里找到修改版的源码。功能与上游 `4.0.6` 相同 |
| decade.2 | 服务器被攻破时，可以给所有进服的玩家装任意 jar；而我们自己的 jar 在平台上查不到，每次更新都弹警告页、要手动重启，玩家早已习惯直接确认 | 上游信任服务器列出的一切，查不到的 jar 只给一个可以跳过的警告 | 见「签名清单」。服务器的整份更新先与离线签名的清单核对，不符就整份拒绝、没有"继续"；相符则视同已验证，启动时静默更新，首次安装也不再弹确认页。下载失败后服务器发来的刷新列表同样要核对；顺带修掉刷新失败时失败的文件从失败名单里消失、更新被当成成功的问题 |
| decade.3 | 客户端会按服务器报的版本，从 Modrinth 下载上游的 AutoModpack 把自己换掉，签名核对随之消失。实测过：服务器还是上游 `4.0.6` 时，`4.0.6-decade.2` 的客户端启动时就换成了上游版 | 上游让客户端跟服务器的版本走（`syncAutoModpackVersion`，默认开）；`4.0.6-decade.N` 按语义化版本还算作 `4.0.6` 的预发布版，上游"不降级"的保护也拦不住 | 客户端永远不替换自己：启动时（`SelfUpdater.update`）与进服握手时（`HandshakeC2SPacket`）都直接返回，版本不同只记日志。本体的新版本随整合包走。上游的代码原样保留、不再执行 |
| decade.4 | 只信任测试公钥，不能发给玩家 | — | 写入正式主钥 `c9226e7155a7e1af` 与应急钥 `f2e571382b234725`，不再信任测试公钥 `793e534344f11a65`；单元测试守住"只有这两把、没有测试公钥" |
| decade.5 | 进服时要更新，屏幕停在原版的"连接中断"页好几秒，玩家以为出错就重连；重连换发了下载凭证，正在进行的下载被服务器断开（实测 20 个文件失败，整合包下了两遍） | 断开后要先取签名清单再核对，这段时间没有界面；原版流程在确认页点了按钮才开始，不会被盖住 | 进服更新一开始就显示下载进度页；约半秒后若屏幕是原版的连接中断页（`DisconnectedScreen`），再放回进度页。只替换这一种界面，不碰错误页与重启页。另外：服务器升级到下一个修改版，所有旧客户端都会在握手时被踢（`4.0.6-decade.N` 按语义化版本算预发布版，上游只让正式版 4.0.x 互通，其余要求完全相同）；上游客户端也能进服、装我们的整合包而不核对签名。改为德卡的服务端接受任何修改版客户端、拒绝其余一切，拒绝时用中英文说明去固定下载入口（`AutoModpackProtocol.isDecadeVersion`，有单元测试） |
| decade.6 | 同一个玩家从第二个安装（另一台电脑、另一个启动器实例）进服后，第一个安装启动时就取不到更新；正在进行的下载也会被断开 | 上游每个玩家只存一个下载凭证，每次登录都换新的、旧的作废 | 每个玩家保留最新的 4 个未过期凭证（`SecretsStore.keysToDrop`，有单元测试）；白名单与封禁检查照旧。上游格式的凭证文件照样能读 |
| decade.6 | 下载链接先试 CurseForge | 先查 CurseForge、后查 Modrinth，链接按查询顺序排 | 先查 Modrinth：国内测速里 CurseForge 的下载最差 |
| decade.6 | 加载 Minecraft 之前弹出的重启提示是英文，像外挂 | — | 改成中文，按钮写明"关闭游戏"；字体改用 `Dialog`：原来的 Segoe UI 没有中文字形，会显示成方框（核对过 `canDisplayUpTo`） |
| decade.6 | 进服更新时下载页先显示"没有要下载的文件！"，随后才开始下载，有误导 | 下载页在还没有任务时显示这句；`decade.5` 起下载页一开始就显示，正好停在这个状态 | 改为"正在准备下载…" |
| decade.7 | 重启提示窗口的按钮显示成"…" | 按钮宽 60、高 25 像素，是给英文 "OK" 定的；"关闭游戏"在 Metal 外观下要 92×29 | 按钮改为 120×30，仍然居中 |
| decade.8 | 第一次进服先弹证书指纹核对页，劝退新手；同一台服务器换个地址进（域名与 IP）又要核对一次；启动阶段遇到没见过的证书则直接放弃更新 | 上游靠核对证书指纹认出服务器，按地址字符串记住（`automodpack-known-hosts.json`） | 任何证书都直接信任，只在日志里记下地址与指纹（`ModpackUtils.userValidationCallback`），不再弹核对页、不再读写指纹表。理由：能装什么由签名清单决定（decade.2），服务器本身被攻破都签不出清单，冒充服务器的人更签不出；证书只剩"是不是那台机器"这一层。冒充者能做到的是让更新失败（掌握线路本来就能做到），以及拿到玩家的下载凭证——它只能用来从我们的服务器下载整合包本身。连接照旧走 TLS 1.3，传输仍加密。上游的询问代码原样保留、不再执行 |

## 签名清单

`pl.skidam.automodpack_core.decade`（核对，纯函数，有单元测试）与 `pl.skidam.automodpack_loader_core.client.DecadeGate`（下载签名文件、保存状态）。

- 服务器在整合包里放 `/decade/pack-manifest.json` 与 `.sig`：列出每个下发文件的路径与 SHA-1、整合包版本号、签名用的密钥，Ed25519 签名，私钥不在服务器上
- 客户端核对：签名来自 jar 里写死的公钥（`DecadeKeys`）；服务器提供的每个文件都以相同的 SHA-1 出现在清单里，清单里的每个文件服务器也都提供；服务器要求删除的文件也在清单里；整合包版本号不低于本机见过的最高值（防止拿旧清单回放）；没有文件写进 `automodpack/`
- 公钥写在 jar 里而不是配置文件里，因为配置目录会被同步，服务器能改它
- 两种密钥：主钥签清单；应急钥只能签"吊销某把主钥、换成新主钥"的声明（`/decade/key-rotation.json` 与 `.sig`），序号更大才生效。见过的最高版本号、换钥结果存在 `automodpack/.private/decade-trust.json`
- `DecadeKeys.TEST` 从 `decade.4` 起为空：测试环境与玩家用同一个 jar，清单同样由主钥签名。它只留作开发时的入口，由它签的清单会在日志里警告，带着测试公钥的构建不发给玩家

## 跟进上游

上游主分支正在做 v5 重写。合并上游新标签后，逐条检查上表的修改是否仍然需要、是否已被上游修掉；上游的安全修复要及时合并。
