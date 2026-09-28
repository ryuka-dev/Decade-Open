# Decade Items（`decade`）

德卡（Decade）Minecraft 服务器的自制物品与「德卡」创造页，目前有护甲插板 `decade:armor_plate`，以及管理员用的废墟勘测杖 `decade:ruin_wand`。

| 项 | 值 |
|---|---|
| modid | `decade` |
| 版本号 | `0.1.N` |
| 许可证 | GPL-3.0-only（见 `LICENSE`）。物品继承 [lrtactical](https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements) 的消耗品类，所以同为 GPL-3.0 |
| 依赖 | Minecraft 1.20.1、Forge 47；**德卡的 lrtactical 修改版**，锁定到一个版本（见 `src/main/resources/META-INF/mods.toml`） |
| 构建 | JDK 17：`./gradlew build`，产物在 `build/libs/` |

## 这个 mod 里有什么，没有什么

物品只是外壳：注册物品、放进创造页、提供名字与模型。**用起来的效果由服务器的服务端 mod 决定**，那部分不随客户端分发，也不在这里。外观与使用动画来自服务器的枪包，同样不在这里。

## 源码

公开仓库 https://github.com/ryuka-dev/Decade-Open 的 `decade-items/` 目录。每个发给玩家的版本都有标签 `decade-items/<版本>`，标签里的内容就是那个 jar 的完整源码。
