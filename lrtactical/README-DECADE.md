# lrtactical（德卡修改版）

德卡（Decade）Minecraft 服务器使用的 [LesRaisins Tactical Equipments](https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements) 修改版。服务器的护甲插板做成它的消耗品：外观与动画来自服务器的枪包，效果在服务端结算，本 mod 里没有服务器的规则或数值。

| 项 | 值 |
|---|---|
| 上游 | https://github.com/LesRaisins-Studios/LesRaisins-Tactical-Equipements |
| 基于 | 标签 `0.4.3`（`git subtree` 引入，保留上游历史） |
| 版本号 | `0.4.3-decade.N`，每次修改递增 N |
| 许可证 | GPL-3.0-only（见 `LICENSE`），修改版同样是 GPL-3.0 |
| 构建 | 沿用上游的 Gradle：在本目录执行 `./gradlew build`，产物在 `build/libs/` |

## 源码

公开仓库 https://github.com/ryuka-dev/Decade-Open 的 `lrtactical/` 目录。每个发给玩家的版本都有标签 `lrtactical/<版本>`，标签里的内容就是那个 jar 的完整源码。

## 我们改了什么

以消耗品为主；手雷与重载的两项是查证后顺带修的同类问题。每一项都能单独提给上游。

| 问题 | 原因 | 修改 |
|---|---|---|
| 读条到一半切到另一种消耗品，会接着读并提前完成；进度条跳变 | 所有消耗品共用同一个物品、靠 NBT 区分，Forge 默认的 `canContinueUsing` 只比较物品类型 | `ConsumableItem.canContinueUsing` 按消耗品 id 比较 |
| 用完时有时会先重新掏出、再收起 | 点按模式只在服务端结束使用，客户端要等「停止使用」与「数量减少」两条同步，先后不定 | 本地玩家的客户端像原版食物一样预测完成与消耗；效果与最终数量仍以服务端为准 |
| 使用中切格子，先闪一下掏出再收起 | 切格子当 tick 就停止了使用，收起信号下一 tick 才到 | 状态机上下文新增 `isHeld()`，默认状态机在物品已不在手上时等待收起 |
| 切到原版物品、枪或空手后，旧消耗品以默认姿势闪现 | simplebedrockmodel 收起期间冻结原版手部渲染，原版之后会把缓存的旧物品再放下一次 | 渲染器在物品已不在手上、且不处于收起过渡时不绘制 |
| 手雷使用中切到另一种手雷，会跳过准备时间、引信被上一颗扣掉、绕过冷却 | 同上：所有投掷物也是同一个物品 | `ThrowableItem.canContinueUsing` 按投掷物 id 比较 |
| 重载资源后，手上物品的动画失效，直到切走再切回 | 重载会用新的、未初始化的状态机替换显示；手上物品的第一人称实例只在拔出时初始化一次，TaCZ 里补初始化的 `FirstPersonRenderEvent` 没有注册 | 三个显示管理器与闪光盾渲染器重建后调用 simplebedrockmodel 的 `FirstPersonRenderHandler.reset()`，手上物品按新拿出处理 |
| `@harmful` 这类按类别清除会顺带清掉别的 mod 只许用专门解药清除的效果（如 The Hordes 的感染） | 类别选择器无差别地清除该类全部效果 | 新增效果标签 `lrtactical:category_removal_immune`，按类别清除时跳过其中的效果；在 `remove_effects` 里点名仍会清除。德卡的标签数据在 `decade_protection` |
| 别的 mod 提供基础物品的消耗品也出现在 lrtactical 的创造页 | 消耗品页列出全部消耗品索引，不看 `base_item` | 只列以 `lrtactical:consumable` 为基础的；其余由提供基础物品的 mod 自己放 |
| 玩家在模组列表里找不到修改版的源码（`decade.5`） | `displayURL` 没有填 | `mods.toml` 的 `displayURL` 指向公开仓库 |
| 燃烧瓶点着的火烧人不算投掷者的伤害：没有击杀归属，别的 mod 也看不出是谁造成的（`decade.6`） | 原版的着火伤害（`on_fire`）不带攻击者；燃烧瓶只是把目标点着 | 新增 `Igniters`：投掷物点火时记下投掷者，火还该烧着时（易燃效果续上的也算）这把火的伤害改由投掷者造成，同样的数值；不附带击退。区域云、一次性喷溅、爆炸的点火三处都经过它 |
| 拿着消耗品、投掷物或 C4 起爆器时，左键和空手一样：打人、挖方块（`decade.7`） | 这几种物品只接了使用键；TaCZ 的枪左右键各有用途，两类物品混着用时左键落空 | 左键也是使用，并且可以在模组列表的配置页（Cloth Config，客户端配置 `clickUseButtons` / `holdUseButtons`）分别设置「点按使用」（切换模式的消耗品、起爆器）和「按住使用」（按住模式的消耗品、投掷物）用哪个键：左右键都能使用或取消（默认）、仅右键、仅左键、左键使用右键取消、右键使用左键取消。没选上的键拿着这些物品时无反应：不攻击、不挖方块，也不开门、不和实体交互。左键使用时只用物品，不像右键那样先和方块、实体交互；按住会像右键一样连续使用。按住使用时松开使用键照旧（投掷物扔出），单独的取消键直接取消、不扔出，为此取消包（原来只收切换模式的消耗品）改为收所有消耗品与投掷物，与切格子中断使用的效果相同。进度条下的取消提示跟着设置走。按住 TaCZ 的交互键时两个键都照原版。近战武器与闪光盾左右键本来各有动作，不在此列 |

## 已知但未修

- 近战不进入"使用中"，闪光盾只有一种，二者没有 `canContinueUsing` 的问题（查证）
- ~~TaCZ 的 `AnimateGeoItemRenderer.tryExit` 把时长当成时间点传给 `setExitingTime`~~：**2026-09-24 查证不成立**。TaCZ 1.1.8-hotfix 的 `setExitingTime` 自己加上当前时间，调用方传的是时长，读取端比较当前时间，三处一致（TaCZ 修改版说明「已知的上游问题」）

## 跟进上游

合并上游新标签后，逐条检查上表的修改是否仍然需要、是否已被上游修掉。
