# ReWasteland 开发文档

> 给下一个会话 / 新维护者的快速上手说明。最后更新：2026-11-08。
> 仓库：https://github.com/litai1hao-bot/Rewasteland （Public / main / MIT）

## 0. 30 秒上手

```powershell
cd D:\MC\rewasteland\rewasteland-template-1.21.1
git status
.\gradlew.bat build                 # 产物 build/libs/rewasteland-1.0.0.jar
```

- 本地项目目录：`D:\MC\rewasteland\rewasteland-template-1.21.1`（**2026-11-08 从 `D:\MC\rewasteland-template-1.21.1` 搬过来**，旧脚本里可能有旧路径）。
- 上游参考仓库（1.21.4 数据包）：`D:\MC\rewasteland\wasteland-datapack`。
- 参考数据 / 工具：`D:\MC\_vanilla_1211`、`D:\MC\_vanilla_1214`、`D:\MC\_vanilla_1211_df`、`D:\MC\_port_tools`。
- 当前状态：世界生成 + 结构 + 结构战利品 + 酸雨已完成；GitHub main 与本地同步。

## 1. 项目速览

- **MC 1.21.1 / NeoForge 21.1.265** 的废土生存模组，mod id `rewasteland`，包名 `com.selton.rewasteland`，版本 `1.0.0`。
- 原型是 Modrinth 上的 **Acid Wasteland**（纯 1.21.4 数据包，MIT）：本仓库是它的 1.21.1 / NeoForge 移植 + 扩展，世界生成不是原样搬运，而是重新生成 / 降级改写。
- 已完成模块：
  1. 世界生成：691 条 multi_noise、13 个原版群系改皮、2 个自定义群系、30 个枯树 feature、沙漠缩减、全局压平地形（无高山）。
  2. 结构：荒屋 ×4 + 幸存者营地 ×5。
  3. 结构战利品：外置到 `config/rewasteland/structure_looting/`，可逐表改 JSON。
  4. 酸雨 / 水中毒：暴露值 + 3 级无限中毒，参数外置到 `config/rewasteland/acid_rain.json`。
  5. GitHub 开源：MIT、CI、README。
- 还没做：吃食物变异、怪物变种与装备、自定义天气机、食物/物品改皮、撬棍贴图/模型。

## 2. 环境与构建

| 项 | 值 |
|---|---|
| JDK | 21（`java.toolchain.languageVersion = 21`；本机默认 java 是 25，靠 foojay 拿 21） |
| Gradle | 9.8.2（wrapper） |
| NeoForge | 21.1.265 |
| ModDevGradle | 2.0.148 |
| Parchment | 2024.11.17 |
| 构建命令 | `.\gradlew.bat build` |
| 产物 | `build/libs/rewasteland-1.0.0.jar` |
| 开发服 | `.\gradlew.bat runServer` |
| 开发客户端 | `.\gradlew.bat runClient` |

注意事项：

- **首次** `createMinecraftArtifacts` 要下载/反编译 NeoForge，约 1~2 分钟；之后 build 几秒。
- 下载新依赖需要代理：本机 Clash Verge 混合端口 `127.0.0.1:7897`，全局 `~/.gradle/gradle.properties` 已配好；Clash 关掉时任何需要联网的 Gradle 任务都会失败。
- 配置缓存开着（`org.gradle.configuration-cache=true`）。改了 `build.gradle` / `gradle.properties` 后如果行为诡异，先 `.\gradlew.bat --stop` 再试。
- 开发服的世界在 `run/world`，`run/` 已 gitignore。测试脚本会自动写 `run/eula.txt` 和 `run/server.properties`（RCON 25581 / 游戏端口 25580）。

## 3. 代码结构

`src/main/java/com/selton/rewasteland/`：

| 文件 | 职责 |
|---|---|
| `ReWasteland.java` | `@Mod` 主类；注册 DeferredRegister、事件监听、配置 |
| `ReWastelandClient.java` | 客户端初始化（目前只有 MDK 示例内容） |
| `Config.java` | MDK 示例的 ModConfigSpec（`logDirtBlock` 等），目前基本未使用 |
| `StructureLootConfig.java` | 读 `config/rewasteland/structure_looting/*.json`，在 `LootTableLoadEvent` 里替换战利品表 |
| `AcidRain.java` | 酸雨事件监听、配置读写、效果/音效/粒子、`/rwacid` 调试命令 |
| `AcidRainLogic.java` | 纯状态机（无 MC import，可用 javac 直接单测） |

注册入口（`ReWasteland` 构造函数）：

```java
StructureLootConfig.ensureDefaults();
NeoForge.EVENT_BUS.addListener(StructureLootConfig::onLootTableLoad);

AcidRain.ensureDefaults();
NeoForge.EVENT_BUS.addListener(AcidRain::onPlayerTick);
NeoForge.EVENT_BUS.addListener(AcidRain::onPlayerClone);
NeoForge.EVENT_BUS.addListener(AcidRain::onServerStarted);
NeoForge.EVENT_BUS.addListener(AcidRain::registerCommands);
```

## 4. 资源与数据布局

`src/main/resources/data/`：

| 路径 | 内容 |
|---|---|
| `minecraft/dimension/overworld.json` | 整表替换主世界，**691 条 multi_noise**（上游 373 条 + 调参拆分） |
| `minecraft/worldgen/noise_settings/overworld.json` | 地表规则 + 地形上限（见下） |
| `minecraft/worldgen/density_function/overworld/*.json` | 地形压平：`offset` / `factor` / `jaggedness` / `sloped_cheese` |
| `minecraft/worldgen/biome/*.json` | 13 个原版群系改皮（温度拉高、配色、去植被、雪→石） |
| `minecraft/worldgen/configured_feature/*.json` | 30 个树 feature（树叶全部换成空气 = 枯树） |
| `rewasteland/worldgen/biome/{wasteland,desert}.json` | 2 个自定义群系 |
| `rewasteland/worldgen/structure*`, `structure/*.nbt` | 荒屋 ×4 + 营地 ×5 |
| `rewasteland/loot_table/chests/*.json` | 结构战利品的**内置兜底表** |
| `rewasteland/tags/worldgen/biome/is_{wasteland,desert}.json` | 结构生成用的群系标签 |
| `src/main/resources/structure_looting/defaults/*.json` | 首次启动复制到 config 的战利品默认值 |

命名约定：自定义内容一律 `rewasteland:`；覆盖原版放 `data/minecraft/...`。用户明确纠正过「是 rewasteland，不是 wasteland」。

## 5. 世界生成设计（重点）

### 5.1 地形是全局噪声，群系管不了高度

MC 1.18+ 的地形高度来自 `noise_settings.noise_router` + `density_function`，群系只决定地表方块/地物。**没有“让某个群系里不生成山”的开关**：

- 要让某群系没有山，只能让它的 multi_noise 参数格避开山地气候区（`C≥0.03 && E≤-0.375`）；
- 或者改全局 `density_function`（会同时影响所有群系）。

用户对项目的定调：“核末日过后，荒原还是不要出现高山”，所以当前是**全局压平**。

### 5.2 当前的调参

| 项 | 做法 |
|---|---|
| desert 权重 | 只保留 `T≥0.2 && H≤0.1` 的热/干核心；参数空间 8.84% → 约 2.67%，其余并给废土 |
| 山地格 | 沙漠/废土里 `C≥0.03 && E≤-0.375` 的子格改派 `stony_peaks`；`E≤-0.78` 改派 `jagged_peaks` |
| 地形高度 | 自定义 `offset.json` 大陆度样条：深海 ≈ y23、海岸 ≈ y64、内陆 ≈ y68~73 |
| 起伏 | `factor.json = 5.0`；`jaggedness.json = 0`；`sloped_cheese.json` 里 `base_3d_noise × 0.35` |
| 上限 | `noise_settings` 的 `final_density` / `initial_density_without_jaggedness` 外包 `min(y_clamped_gradient 90→100)`，最高约 95 |

实测（种子 `rw-flat-3`，576 个区块）：wasteland 最高 78、p95 70；desert 最高 71；没有任何一格到 100。

### 5.3 生成脚本与手改文件

- `D:\MC\_port_tools\port_worldgen.js` 从上游数据包重新生成：`dimension/overworld.json`、`noise_settings`、30 个树 feature、13 个群系改皮、2 个自定义群系、tag。
- **`data/minecraft/worldgen/density_function/overworld/` 下的 4 个文件是手写/静态文件，脚本不会重写它们**（改完 offset 样条要自己维护）。
- 脚本里写死了绝对路径：`PACK`（上游克隆）、`OUT`（本项目 resources）。换机器/搬目录后先核对再跑。
- 调参常量在脚本顶部：`C_MOUNT / E_STONY / E_JAGGED / T_DESERT / H_DESERT`；地形 cap 在脚本第 2 段（`from_y: 90 / to_y: 100`）。

### 5.4 验证方式

```powershell
# 1) 生成
node D:\MC\_port_tools\port_worldgen.js
# 2) 构建
.\gradlew.bat build
# 3) 起 dev 服 + forceload + 解析 region，输出每群系的平均/最高高度
powershell -NoProfile -ExecutionPolicy Bypass -File D:\MC\_port_tools\test_flat.ps1
```

`test_flat.ps1` 会：删 `run/world` → 写 `server.properties`（种子 `rw-flat-3`）→ `gradlew runServer` → RCON `locate biome` + `forceload` → `save-all` / `stop` → `dump_biome_height.js`。

## 6. 酸雨 / 水中毒

- 实现：`AcidRain.java`（`PlayerTickEvent.Post`，仅服务端）、`AcidRainLogic.java`（纯逻辑）。
- 判定：脚或头所在方块是水（骑乘时不判）；或主世界露天 + 下雨/雷雨。
- 数值：暴露 `+1/tick`，干燥 `-1/tick`，上限 1200；`200/600/1000` → 无限中毒 0/I/II，III 级额外绿色 `dust_color_transition` 粒子；升级 `witch.drink`、降级 `fish.swim`。
- 状态存在玩家 `persistent data`（`rw_acid_exposure` / `rw_acid_level` / `rw_acid_poison`），死亡保留；创造模式默认不受影响。
- 配置：`config/rewasteland/acid_rain.json`（首次启动生成，附 `README-acid-rain.txt`，世界启动时重读）。
- 配置字段：`enabled` / `water_is_acidic` / `rain_is_acidic` / `affect_creative_players` / `exposure_gain_per_tick` / `exposure_loss_per_tick` / `max_exposure` / `tier1..3_threshold`。
- 调试命令（op）：
  - `/rwacid <ticks> <true|false>`：在假人身上强制暴露/干燥跑 N tick；
  - `/rwacid auto <ticks>`：按假人所在位置真实判定水/雨/遮挡；
  - `/rwacid reset`：清空假人状态。
- 目前只认原版天气；自定义天气机是后续独立系统。

## 7. 结构 / 战利品

- 结构 id：`rewasteland:ruin`（4 模板）、`rewasteland:survivor_camp`（5 模板）。
- 限定标签：`#rewasteland:is_desert` / `#rewasteland:is_wasteland`。
- 战利品：`StructureLootConfig` 在 `LootTableLoadEvent` 里用 `RegistryOps + LootTable.DIRECT_CODEC` 替换；
  配置目录 `config/rewasteland/structure_looting/`，每张表一个 JSON，`"loot_table"` 声明要接管哪张表；
  改完 `/reload` 或重进世界生效；写坏只报错并回退内置表。
- 默认结构表：`rewasteland:chests/survivor_camp_scraps`、`rewasteland:chests/survivor_camp_tools`。

## 8. 测试工具（D:\MC\_port_tools）

| 脚本 | 用途 |
|---|---|
| `port_worldgen.js` | 世界生成移植 + 调参（见 5.3） |
| `port_structures.js` | 结构 JSON / NBT / 战利品默认值（路径可能仍是旧项目路径，用前核对） |
| `rcon.js` | 单命令 RCON 客户端（多命令会挂，别用） |
| `dump_chunk_biome.js` / `dump_biome_height.js` | 解析 `region/*.mca`，统计群系/地表方块/高度 |
| `test_flat.ps1` | 压平地形端到端实测 |
| `test_structures2.ps1` | 结构 + 战利品端到端 |
| `test_acid.ps1` | 酸雨状态机（强制暴露） |
| `test_acid_env.ps1` | 酸雨环境判定（水/雨/遮挡） |

旧脚本 `test_neoforge.ps1` / `test_surface*.ps1` / `test_1211_server.ps1` 里还是旧项目路径（`D:\MC\rewasteland-template-1.21.1`），直接用会失败。

## 9. 踩过的坑（省时间）

- `maven.neoforged.net` 曾被 DNS 污染，构建报 `Could not resolve ... socket timed out` 时先查 DNS/代理，不要乱改版本。
- PowerShell 5.1 的 `Set-Content -Encoding utf8` 写 BOM，`javac` 报「非法字符 '\ufeff'」；用 `write` 工具或 `UTF8Encoding($false)`。
- PowerShell 函数名 `R` 会被内置别名 `Invoke-History` 抢，脚本里用 `Send-Rcon`。
- `[System.IO.File]` 的静态方法用**进程 CWD**，不跟随 `Set-Location`——传绝对路径。
- 双引号字符串里反引号是转义符，匹配含反引号的文本用单引号 / here-string。
- `/weather rain` 后 `rainLevel` 要爬 1 秒左右才 `isRaining()`，测试环境判定要等几秒再采样。
- `addEffect` 不会把更强效果降级，降级必须先 `removeEffect`。
- 无客户端测玩家效果：`FakePlayerFactory` + op 命令（`/rwacid`）。
- 老世界已生成的区块不会因世界生成改动而变化，必须开新世界。

## 10. Git / CI / 发布

- 分支：`main`；远程 `origin` = `https://github.com/litai1hao-bot/Rewasteland.git`。
- 提交作者：`litai1hao-bot <350916319+litai1hao-bot@users.noreply.github.com>`。
- 推送：HTTPS + Git Credential Manager（SSH 22 被代理拦截，不走 SSH）。
- CI：`.github/workflows/build.yml`，push / PR 触发，ubuntu-latest + JDK 21 + `./gradlew build`。
- `.gitignore` 已排除 `run/`、`build/`、`.gradle/`、`.idea/`、`.dsh-meow/`（agent 记忆库与会话记录，绝不提交）。
- 发版清单：改 `gradle.properties` 的 `mod_version` → `.\gradlew.bat clean build` → `jar tf build/libs/*.jar` 核对 → `git tag vX.Y.Z` → GitHub Release 上传 jar →（可选）Modrinth / CurseForge。

## 11. 下一步建议

按上游 Acid Wasteland 的系统顺序：

1. **吃食物变异**：带 `custom_data.mutate` 的废土食物吃 5 份 → 60 秒属性 buff（攻 10 / 速 0.15 / 体型 1.5）。
2. **怪物变种与装备**：`#zombies` 标签 + attribute + 装备战利品表。
3. **自定义天气机**：`doWeatherCycle=false` + 状态机，让酸雨有节奏。
4. **食物 / 物品改皮**：1.21.1 用 `custom_model_data` + 模型 override 替代 `item_model`；撬棍贴图/模型。

## 12. 新会话检查清单

1. `git status` / `git log --oneline -n 5` 看有没有未提交改动、远程是否同步。
2. 读本文件 + `README.md`。
3. `.\gradlew.bat build` 确认能编译。
4. 改世界生成 → 先跑 `port_worldgen.js` 再 build，再 `test_flat.ps1` 实测。
5. 改玩法 → build 后用 dev 服 + RCON / 调试命令验证。
6. 提交前看 `git status`，别把 `run/`、`build/`、`.dsh-meow/` 带进去。
