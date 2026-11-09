# ReWasteland 开发文档

> 给下一个会话 / 新维护者的快速上手说明。最后更新：2026-11-09。
> 仓库：https://github.com/litai1hao-bot/Rewasteland （Public / main / MIT）

## 0. 30 秒上手

```powershell
cd D:\MC\rewasteland\rewasteland-template-1.21.1
git status
.\gradlew.bat build                 # 产物 build/libs/rewasteland-2.0.0.jar
```

- 本地项目目录：`D:\MC\rewasteland\rewasteland-template-1.21.1`（**2026-11-08 从 `D:\MC\rewasteland-template-1.21.1` 搬过来**，旧脚本里可能有旧路径）。
- 上游参考仓库（1.21.4 数据包）：`D:\MC\rewasteland\wasteland-datapack`。
- 参考数据 / 工具：`D:\MC\_vanilla_1211`、`D:\MC\_vanilla_1214`、`D:\MC\_vanilla_1211_df`、`D:\MC\_port_tools`。
- 当前状态：世界生成（2.0.0 起是独立世界类型 `rewasteland:wasteland`）+ 结构 + 结构战利品 + 酸雨 + 酸水方块，版本 2.0.0。

## 1. 项目速览

- **MC 1.21.1 / NeoForge 21.1.265** 的废土生存模组，mod id `rewasteland`，包名 `com.selton.rewasteland`，版本 `2.0.0`。
- 原型是 Modrinth 上的 **Acid Wasteland**（纯 1.21.4 数据包，MIT）：本仓库是它的 1.21.1 / NeoForge 移植 + 扩展，世界生成不是原样搬运，而是重新生成 / 降级改写。
- 已完成模块：
  1. 世界生成：**独立世界类型 `rewasteland:wasteland`**（像原版的默认/超平坦/放大化一样在世界创建界面选择，服务器用 `level-type=rewasteland:wasteland`）；691 条 multi_noise、13 个改皮群系、2 个自定义群系、10 个作用域内 feature、沙漠缩减、全局压平地形（无高山）。
  2. 结构：荒屋 ×4 + 幸存者营地 ×5。
  3. 结构战利品：外置到 `config/rewasteland/structure_looting/`，可逐表改 JSON。
  4. 酸雨 / 水中毒：暴露值 + 3 级中毒（每秒刷新 5 秒，1200 上限额外凋零），参数外置到 `config/rewasteland/acid_rain.json`；**只在废土世界类型生效**。
  5. 酸水方块：自然生成的水全部换成 `rewasteland:acid_water`（只限废土世界）；水的毒性只作用于酸水（普通水安全）；独立 FluidType + 流体方块 + 桶，客户端酸绿贴图/雾/水下遮罩。
  6. GitHub 开源：MIT、CI、README。
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
| 产物 | `build/libs/rewasteland-2.0.0.jar` |
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
| `ReWastelandClient.java` | 客户端：酸水渲染（酸绿贴图/水下雾/水下遮罩）、流体渲染层 |
| `Config.java` | MDK 示例的 ModConfigSpec（`logDirtBlock` 等），目前基本未使用 |
| `StructureLootConfig.java` | 读 `config/rewasteland/structure_looting/*.json`，在 `LootTableLoadEvent` 里替换战利品表 |
| `AcidRain.java` | 酸雨事件监听、配置读写、效果/音效/粒子、`/rwacid` 调试命令；只在废土世界类型生效 |
| `WastelandWorld.java` | 判断一个 level 是不是废土世界类型（看 chunk generator 的 noise settings 是否 `rewasteland:wasteland`） |
| `AcidRainLogic.java` | 纯状态机（无 MC import，可用 javac 直接单测） |
| `ModFluids.java` | 酸水注册：FluidType、source/flowing 流体、LiquidBlock、酸水桶 |
| `AcidWaterFluid.java` | 继承 `WaterFluid`，复刻流速/无限水/桶音效，只换流体身份 |
| `AcidWaterType.java` | 酸水 FluidType（溺水/游泳/推挤/灭火/船；`canHydrate=false`） |

注册入口（`ReWasteland` 构造函数）：

```java
StructureLootConfig.ensureDefaults();
NeoForge.EVENT_BUS.addListener(StructureLootConfig::onLootTableLoad);

AcidRain.ensureDefaults();
NeoForge.EVENT_BUS.addListener(AcidRain::onPlayerTick);
NeoForge.EVENT_BUS.addListener(AcidRain::onPlayerClone);
NeoForge.EVENT_BUS.addListener(AcidRain::onServerStarted);
NeoForge.EVENT_BUS.addListener(AcidRain::registerCommands);

ModFluids.register(modEventBus);   // 酸水：流体类型 / 流体 / 方块 / 桶
```

## 4. 资源与数据布局

`src/main/resources/data/`（2.0.0 起：世界生成全部在 `rewasteland` 命名空间；`data/minecraft/` 只留 tag）：

| 路径 | 内容 |
|---|---|
| `rewasteland/worldgen/world_preset/wasteland.json` | **世界类型** `rewasteland:wasteland`：主世界内联 691 条 multi_noise + `rewasteland:wasteland` noise settings；下界/末地照抄 `minecraft:normal` |
| `minecraft/tags/worldgen/world_preset/normal.json` | 把 `rewasteland:wasteland` 挂进创建界面的「世界类型」轮换列表（tag 合并，不顶掉原版） |
| `rewasteland/worldgen/noise_settings/wasteland.json` | 地表规则 + 地形上限 + `default_fluid = rewasteland:acid_water` |
| `rewasteland/worldgen/density_function/overworld/*.json` | 地形压平：`offset` / `factor` / `jaggedness` / `sloped_cheese` |
| `rewasteland/worldgen/biome/*.json` | 世界里的群系：13 个改皮（`badlands`…`wooded_badlands`）+ 2 个洞穴群系（`dripstone_caves` / `lush_caves`）+ 2 个自定义（`wasteland` / `desert`） |
| `rewasteland/worldgen/configured_feature/*.json` | 10 个作用域内 feature：枯树链（`oak` / `oak_bees_005` / `fancy_oak_bees_005` / `trees_plains`）、`spring_water`（酸水）、`dripstone_cluster`、`clay_pool_with_dripleaves`、杜鹃树链 |
| `rewasteland/worldgen/placed_feature/*.json` | 上面这些 feature 的 placed 版本 + `patch_grass_wasteland` |
| `minecraft/tags/worldgen/biome/has_structure/*.json` | 把新群系 id 加回原版 `has_structure` 标签，原版结构分布保持与 1.1.x 一致 |
| `minecraft/tags/worldgen/biome/is_overworld.json` | 把废土群系标成主世界群系（第三方模组兼容） |
| `rewasteland/worldgen/structure*`, `structure/*.nbt` | 荒屋 ×4 + 营地 ×5 |
| `rewasteland/loot_table/chests/*.json` | 结构战利品的**内置兜底表** |
| `rewasteland/tags/worldgen/biome/is_{wasteland,desert}.json` | 结构生成用的群系标签 |
| `rewasteland/tags/fluid/acid_water.json` | 酸水流体标签（酸雨判定用） |
| `src/main/resources/structure_looting/defaults/*.json` | 首次启动复制到 config 的战利品默认值 |

命名约定：自定义内容一律 `rewasteland:`；`data/minecraft/` 只用来放 tag（合并语义），**不要再往里放 worldgen / dimension 文件**——那会重新变成“接管原版主世界”。用户明确纠正过「是 rewasteland，不是 wasteland」。

## 5. 世界生成设计（重点）

### 5.0 独立世界类型（2.0.0 起）

以前是直接接管 `minecraft:dimension/overworld` + `minecraft:noise_settings` + 13 个 `minecraft:biome`，等于把原版主世界换掉；2.0.0 起改成**可选世界类型**：

- **机制**：世界类型 = `worldgen/world_preset` 注册表条目；创建界面的「世界类型」轮换列表由 tag `#minecraft:world_preset/normal`（`WorldPresetTags.NORMAL`）驱动，tag 是合并语义，所以只往 `data/minecraft/tags/worldgen/world_preset/normal.json` 里追加 `rewasteland:wasteland` 即可，原版的默认/超平坦/放大化仍在。
- **服务器**：`server.properties` 的 `level-type=rewasteland:wasteland`（`DedicatedServerProperties` 会按 ResourceLocation 查 world preset 注册表，支持自定义 id）；不写就是原版默认世界。已实测两种世界都能正常启动。
- **preset 内容**：主世界 `type: minecraft:overworld` + `generator.settings: rewasteland:wasteland` + 内联 691 条 multi_noise；下界/末地直接复用 `minecraft:normal` 的内联配置。`WorldPreset` 的 dimensions 不支持“引用其他 dimension 文件”，必须内联。
- **隔离原理**：世界生成数据全部搬到 `rewasteland:` 命名空间，原版注册表一条不动。判定“是不是废土世界”用 `WastelandWorld.isWasteland(level)`——看 chunk generator 的 noise settings 是不是 `rewasteland:wasteland`（`level.dat` 里存的是这个引用）。
- **酸雨/酸水跟着世界类型走**：`AcidRain.tick()` 第一步就检查 `WastelandWorld.isWasteland(level)`，不是废土世界直接 return；自然酸水来自 `rewasteland:noise_settings/wasteland.json` 的 `default_fluid`，原版世界用原版 noise settings，所以是普通水。
- **群系 id 变了**：13 个改皮群系从 `minecraft:badlands` 等变成 `rewasteland:badlands` 等，2 个洞穴群系也复制成 `rewasteland:dripstone_caves` / `rewasteland:lush_caves`（为了把 `spring_water` / `dripstone_cluster` / `clay_pool_with_dripleaves` / 杜鹃树这些 feature 也 scope 进废土世界）。原版结构分布用 `has_structure` tag 补回新 id，保持和 1.1.x 一致。
- **旧存档**：`level.dat` 存的是引用（`minecraft:overworld` noise settings + `minecraft:*` 群系 id），这些覆盖删掉后，旧存档**新生成区块**会回到原版；已生成区块不变。要完整废土体验请开新世界。

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
| 地形高度 | 自定义 `offset.json`：大陆度样条（深海 ≈ y23、海岸 ≈ y64、内陆 ≈ y68~73）整体再降 0.06（约 6 格），加低频丘陵项（`ridge` 噪声 `xz_scale 0.4`，振幅 0.12）+ `ridges_folded` 河带下切（约 -0.06） |
| 河流频率 | `dimension` 里 10 条 `minecraft:river` 参数的 weirdness 带从 ±0.05 放宽到 ±0.1；河道只沿 river 带下切，不再使用高频水道网（它会在荒原上留下大量干坑） |
| 起伏 | `factor.json = 8.0`；`jaggedness.json = 0`；`sloped_cheese.json` 里 `base_3d_noise × 0.5` |
| 上限 | `noise_settings` 的 `final_density` / `initial_density_without_jaggedness` 外包 `min(y_clamped_gradient 90→100)`，最高约 95 |
| 岩洞 | 回退为原版：18 群系 carvers 恢复、`caves/entrances` 恢复；模组内不再做洞穴删除，用户后续自行安装 NoCaves |
| 原版水颜色 | 15 个群系的 `effects.water_color` / `water_fog_color` 重置为原版蓝（4159204 / 329011），否则原版水会被群系染成绿/青色 |

实测（种子 `rw-river-1`，432 个 full chunk）：wasteland 平均 y62.9、p95 y68、最高 y78；river 平均 y62.3、最高 y69；没有任何一格到 100。因为降了 6 格，低地大面积被淹：248 个区块有水面酸水（约 57%），水面宽 p50=13、平均 10.9 格；水深 p50=2、平均 4.5；`minecraft:water` 0。洞穴保持原版。

### 5.3 生成脚本与手改文件

- 调世界生成的固定流程（2.0.0 起）：`port_worldgen.js` 仍然生成到 `data/minecraft/...`（当成“源文件”），**必须紧接着跑 `migrate_worldtype.js`**，它会：① 把 dimension/noise_settings/密度函数/群系/feature 全部改写成 `rewasteland:` 命名空间并生成 world preset；② 把 feature 引用收敛成作用域闭包；③ 生成 `has_structure` / `is_overworld` / `world_preset` tag；④ 删掉 `data/minecraft` 下的 worldgen/dimension 临时覆盖。然后 build。
- 只改 `data/rewasteland/...` 里的产物也行，但下次跑 `port_worldgen.js` 会被重新生成覆盖。
- **`data/rewasteland/worldgen/density_function/overworld/` 下的文件是手写/静态文件**，`port_worldgen.js` 不会重写它们；改完要自己维护，并在 `migrate_worldtype.js` 之后确认引用还是 `rewasteland:overworld/*`。`offset.json` 现在是：大陆度样条 + 低频丘陵项 + `ridges_folded` 河带下切。
- 脚本里写死了绝对路径：`PACK`（上游克隆）、`OUT`（本项目 resources）。换机器/搬目录后先核对再跑。
- 调参常量在 `port_worldgen.js` 顶部：`C_MOUNT / E_STONY / E_JAGGED / T_DESERT / H_DESERT`；地形 cap 在脚本第 2 段（`from_y: 90 / to_y: 100`）。

### 5.4 验证方式

```powershell
# 1) 生成（port_worldgen 生成到 data/minecraft，再由 migrate_worldtype 迁到 rewasteland 并删除临时覆盖）
node D:\MC\_port_tools\port_worldgen.js
node D:\MC\_port_tools\migrate_worldtype.js
# 2) 构建
.\gradlew.bat build
# 3) 起 dev 服 + forceload + 解析 region，输出每群系的平均/最高高度
powershell -NoProfile -ExecutionPolicy Bypass -File D:\MC\_port_tools\test_flat.ps1
```

`test_flat.ps1` 会：删 `run/world` → 写 `server.properties`（种子 `rw-flat-3`）→ `gradlew runServer` → RCON `locate biome` + `forceload` → `save-all` / `stop` → `dump_biome_height.js`。**注意它默认写的是 `minecraft:normal`**，2.0.0 起跑之前要把 `level-type` 改成 `rewasteland:wasteland`（或直接用下面的 `test_worldtype.ps1`）。

世界类型 + 酸雨作用域验证：

```powershell
# 废土世界：应为 rewasteland:* 群系、酸水、/rwacid 能累积暴露
powershell -NoProfile -ExecutionPolicy Bypass -File D:\MC\_port_tools\test_worldtype.ps1 -LevelType 'rewasteland:wasteland' -Tag 'wt-wasteland'
# 原版世界：应为原版群系（forest/plains…）、普通水、/rwacid 暴露恒 0
powershell -NoProfile -ExecutionPolicy Bypass -File D:\MC\_port_tools\test_worldtype.ps1 -LevelType 'minecraft:normal' -Tag 'wt-vanilla'
```

2026-11-09 实测结论（种子 `rw-worldtype-1`）：废土世界 `locate biome rewasteland:wasteland` 命中、`locate structure rewasteland:survivor_camp` 115 格、`rwacid 200 true` → exposure 200 / level 1、领域内 `minecraft:water` 0 / `rewasteland:acid_water` 382；原版世界同一套命令 → 群系 `minecraft:forest`（有橡木/白桦树叶）、`rewasteland:wasteland` 找不到、`minecraft:water` 382 / `acid_water` 0、`rwacid 200 true` → exposure 0 / level 0。

河流单独验证：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File D:\MC\_port_tools\test_river.ps1
node D:\MC\_port_tools\analyze_river.js D:\MC\rewasteland\rewasteland-template-1.21.1\run\world\region
node D:\MC\_port_tools\river_metrics.js D:\MC\rewasteland\rewasteland-template-1.21.1\run\world\region
node D:\MC\_port_tools\water_metrics.js D:\MC\rewasteland\rewasteland-template-1.21.1\run\world\region
```

### 5.5 自然水 → 酸水（1.1.0）

用户要求：所有自然水都换成酸水，普通水安全，净水保留；外观最初是复用原版水贴图 + 酸绿 tint，最终改成直接使用生成的酸绿贴图。

- **流体**：`rewasteland:acid_water`（source + flowing + LiquidBlock + 桶），独立 `FluidType`（继承 `WaterFluid` 复用流速/无限水/桶音效）。**没有加进 `#minecraft:water` 标签**：避免玻璃瓶从酸水装出净水、海绵/甘蔗/耕地/钓鱼把酸水当水；代价是水下 FOV/环境音/游泳姿态缺失（酸水致死，影响小）。
- **客户端**：`IClientFluidTypeExtensions` 使用自定义 `acid_water_still`/`acid_water_flow`/`acid_water_overlay` 酸绿贴图（绿色已烘焙，tint 设为白色）、绿色雾、绿色水下遮罩；`ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent())` 必须设，否则 chunk 渲染层默认是 solid。
- **世界生成**：`rewasteland:noise_settings/wasteland.json` 的 `default_fluid` → 酸水（海洋/河流/湖泊/洞穴含水层全换）；`spring_water` → 酸水（作用域副本 `rewasteland:spring_water`）；lush 洞穴 `clay_pool_with_dripleaves` 改成无水 patch；`dripstone_cluster` 设 `wetness: 0.0`；`rewasteland:desert` 移除 `desert_well`。这些 feature 都是 `rewasteland:` 命名空间下的副本，只挂在废土世界自己的群系上，原版世界不受影响。
- **河流**：原版河道其实是 `offset` 样条对 `ridges_folded` 的响应（river 群系就是 weirdness≈0 的带）；之前全局压平时把这段抹掉了，导致 river 群系没有水。现在 `offset.json` 保留 river 带下切（river 的 weirdness 带 ±0.1、深度约 -0.06），不再叠高频水道网——高频水道会在荒原上切出大量干坑（2026-11-08 按用户反馈移除）。另外把内陆 offset 整体下调 0.06（约 6 格），让河床和低地落到海平面以下；实测 `rw-river-1`：432 区块中 248 个有水面酸水（约 57%），水面宽 p50=13、平均 10.9 格；水深 p50=2、平均 4.5；`minecraft:water` 0。这个降幅会让大片低地被淹，属于用户要求的 6 格版本。
- **地形起伏**：`factor.json` 8.0、`base_3d_noise × 0.5`，再加低频丘陵项（振幅 0.12），让压平后的荒原有适度起伏；`jaggedness = 0` 去掉尖峰。6 格下移后实测 wasteland p95 y68、最高 y78，没有到 100 的列。
- **洞穴**：尝试过生成期过滤 + `CaveRemover`，但用户认为效果不行，2026-11-08 回退到“只降低地形高度”的版本；18 群系 carvers 和 `caves/entrances` 已恢复原版，`CaveRemover`/`remove_features`/`cave_decor` 全部删除。后续洞穴由用户自行安装 NoCaves 处理。
- **原版水颜色**：15 个群系的 `effects.water_color` / `water_fog_color` 已重置为原版蓝（4159204 / 329011）。之前沿用上游的绿/青色，导致结构水、玩家水桶等原版水看起来也是绿的；酸水用自定义贴图/tint/雾，不受影响。
- **已知遗留**：海底神殿/海底废墟/要塞是 Java 结构代码里硬编码放 `minecraft:water`，数据包改不了；目前保留（可当净水点），要处理得加 Mixin/结构覆盖。客户端观感 2026-11-08 尚未目视验证。
- **净水**：自然水全酸后，水桶只能从雨天炼药锅装净水（滴水石需要 `#minecraft:water` 上方，不会在酸水上滴）。
- **验证**（2026-11-08，种子 `rw-acid-1`，207 区块）：`rewasteland:acid_water` 114239 格 / 88 区块，`minecraft:water` 0；`/place feature minecraft:spring_water` 生成酸水；酸水接触 `exposed=true`、普通水 `false`；210 tick → level 1、poison amp 0 dur -1。

## 6. 酸雨 / 水中毒

- 实现：`AcidRain.java`（`PlayerTickEvent.Post`，仅服务端）、`AcidRainLogic.java`（纯逻辑）。
- **作用域**：只在 `rewasteland:wasteland` 世界类型生效——`AcidRain.tick()` 先调 `WastelandWorld.isWasteland(level)`（判断 chunk generator 的 noise settings），原版/超平坦/放大化世界直接 return，`/rwacid` 也不会累积暴露。这样「酸雨酸水跟着世界类型走」。
- 判定：脚或头所在方块是**酸水**（`#rewasteland:acid_water`，骑乘时不判）；或主世界露天 + 下雨/雷雨。普通水默认安全。
- 数值：暴露 `+1/tick`，干燥 `-1/tick`，上限 1200；`200/600/1000` → 每秒刷新一次、持续 5 秒的中毒 0/I/II，III 级额外绿色 `dust_color_transition` 粒子；到 1200 上限额外每秒刷新 5 秒凋零 0（会致死）；离开酸水后效果最多残留 5 秒；升级 `witch.drink`、降级 `fish.swim`。
- 状态存在玩家 `persistent data`（`rw_acid_exposure` / `rw_acid_level` / `rw_acid_poison`），死亡保留；创造模式默认不受影响。
- 配置：`config/rewasteland/acid_rain.json`（首次启动生成，附 `README-acid-rain.txt`，世界启动时重读）。
- 配置字段：`enabled` / `acid_water_toxic` / `plain_water_toxic` / `rain_is_acidic` / `affect_creative_players` / `exposure_gain_per_tick` / `exposure_loss_per_tick` / `max_exposure` / `tier1..3_threshold`。旧配置的 `water_is_acidic` 仍会作为 `acid_water_toxic` 读取。
- 调试命令（op）：
  - `/rwacid <ticks> <true|false>`：在假人身上强制暴露/干燥跑 N tick；
  - `/rwacid auto <ticks>`：按假人所在位置真实判定酸水/雨/遮挡；
  - `/rwacid reset`：清空假人状态。
- 目前只认原版天气；自定义天气机是后续独立系统。

## 7. 结构 / 战利品

- 结构 id：`rewasteland:ruin`（4 模板）、`rewasteland:survivor_camp`（5 模板）。
- 限定标签：`#rewasteland:is_desert` / `#rewasteland:is_wasteland`（指向 2 个自定义群系，2.0.0 起群系 id 不变）。
- 群系 id 改名（`minecraft:badlands` → `rewasteland:badlands`）只影响原版结构；自定义结构的 tag 不用改。原版结构的分布靠 `data/minecraft/tags/worldgen/biome/has_structure/*` 补新 id 保持。 
- 战利品：`StructureLootConfig` 在 `LootTableLoadEvent` 里用 `RegistryOps + LootTable.DIRECT_CODEC` 替换；
  配置目录 `config/rewasteland/structure_looting/`，每张表一个 JSON，`"loot_table"` 声明要接管哪张表；
  改完 `/reload` 或重进世界生效；写坏只报错并回退内置表。
- 默认结构表：`rewasteland:chests/survivor_camp_scraps`、`rewasteland:chests/survivor_camp_tools`。

## 8. 测试工具（D:\MC\_port_tools）

| 脚本 | 用途 |
|---|---|
| `port_worldgen.js` | 世界生成移植 + 调参（见 5.3；生成到 `data/minecraft`，之后必须跑 `migrate_worldtype.js`） |
| `migrate_worldtype.js` | 把 worldgen 数据迁到 `rewasteland:` 命名空间、生成 world preset/tag、删除原版覆盖（见 5.3） |
| `test_worldtype.ps1` | 端到端验证世界类型：`-LevelType rewasteland:wasteland` / `minecraft:normal`，跑 locate/酸雨/群系/流体统计 |
| `port_structures.js` | 结构 JSON / NBT / 战利品默认值（路径可能仍是旧项目路径，用前核对） |
| `rcon.js` | 单命令 RCON 客户端（多命令会挂，别用） |
| `dump_chunk_biome.js` / `dump_biome_height.js` | 解析 `region/*.mca`，统计群系/地表方块/高度 |
| `test_flat.ps1` | 压平地形端到端实测 |
| `test_structures2.ps1` | 结构 + 战利品端到端 |
| `test_acid.ps1` | 酸雨状态机（强制暴露） |
| `test_acid_env.ps1` | 酸水/普通水/雨/遮挡判定 |
| `test_acidwater.ps1` | 新世界生成统计（酸水/原版水）+ 放置/流动/spring 覆盖冒烟 |
| `test_river.ps1` / `analyze_river.js` | 河流：locate river + forceload，统计 river 群系区块与河床酸水 |
| `river_metrics.js` / `water_metrics.js` | 河道宽度/深度指标：水面列数、连续水深、沿 x/z 的水面连通宽度 |
| `cave_metrics.js` / `cave_exposure.js` | 洞穴空气统计：按 Y 分段计数，判断 `cave_air` 是否高于地表（暴露） |
| `cave_decor.js` | 统计滴水石/苔藓/藤蔓/紫水晶/幽匿/发光地衣等洞穴装饰残留 |
| `probe_acidlogic.ps1` | 酸水/普通水/酸雨/中毒分级实测 |
| `dump_fluids.js` / `analyze_fluids.js` | 解析 region，统计酸水/原版水与按群系/结构分布 |

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
- **mod 构造期不要静态 `new Fluid`**：`Fluid` 构造会 `createIntrusiveHolder`，此时注册表已冻结 → `IllegalStateException: Registry is already frozen`。用 `DeferredRegister` 在注册事件里懒创建；block 注册事件在 fluid 之后，所以方块构造里可以安全 `get()` 流体 holder。
- 自定义流体的 chunk 渲染层默认 `solid`，必须 `ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent())`。
- 自然水不止 `default_fluid`：`spring_water`、lush 洞穴黏土水池、`dripstone_cluster` 水池、`desert_well` 都会放原版水；海底神殿/海底废墟/要塞是 Java 代码硬编码。
- 河流不是单独的 density function：它藏在 `offset` 样条对 `ridges_folded` 的响应里。全局压平/重写 offset 时必须保留或重建这段河道下切，否则 river 群系会生成但没有水。
- 群系的 `effects.water_color` / `water_fog_color` 会影响**原版水**的 tint 和雾；把所有自然水换成酸水后，这两个值要重置为原版蓝，否则结构水/玩家放的水也会发绿。
- 河流/水道宽度对下切样条的阈值非常敏感：`|ridge|` 阈值稍大就会出现整块水面，稍小就只剩地下含水层。调完一定要用 `river_metrics.js` / `water_metrics.js` 看水面宽度、深度和出现区块数，不能只看酸水总格数。
- 不要为了增加河道数量直接在 offset 里叠高频 `ridge` 水道网：它会在平坦荒原上切出大量干坑，实机地面会变得坑坑洼洼。要更多河就加规则的 river band（或调 weirdness 参数），代价是 biome 分布变化。
- `caves/entrances` 密度**不能直接设 0**：实测会把整个地表地形生成搞崩（wasteland 平均高度掉到 y-7）。这是试做洞穴删除时的教训，现已回退，模组不碰洞穴。
- 河道下切和河宽是耦合的：同样 ±0.1 的 river band，下切从 -0.06 加深到 -0.075 会把水面 p50 从 9 推到 11~15，想保持窄必须同时收窄平台（folded -0.97）。
- `#minecraft:water` 标签不等于“水生生物会生成”：`WaterAnimal` 还要求上方方块是 `minecraft:water` 方块；加标签只影响 drowned/guardian，且它们不会正常游泳。
- PowerShell 5.1 把命令传给原生程序会剥掉内嵌双引号，含 SNBT `id:"..."` 的 RCON 测试命令会坏；测试时避开引号或让 `rcon.js` 从文件读命令。
- **给 configured feature 换命名空间时只能重写 `"feature"` 键**：`"type"` 是 feature 类型 id（如 `minecraft:dripstone_cluster`、`minecraft:spring_feature`），和 configured feature 可能同名。整段字符串替换会把 type 也改掉，报 `Unknown registry key in ResourceKey[minecraft:root / minecraft:worldgen/feature]: rewasteland:dripstone_cluster`，整个注册表加载失败、服务器起不来。
- 新世界类型要两件东西：`data/<ns>/worldgen/world_preset/<name>.json` + 在 `#minecraft:world_preset/normal` tag 里加一行（UI 才看得到）；服务器直接 `level-type=<ns>:<name>`，`DedicatedServerProperties` 按 ResourceLocation 查注册表。`WorldPreset` 的 dimensions 不支持引用别的 dimension 文件，必须内联完整 LevelStem。
- 数据包 tag 是**合并**语义（不写 `"replace": true` 就追加），所以往 `data/minecraft/tags/...` 只写新增项是安全的，不会顶掉原版值。
- 群系改成 `rewasteland:` 新 id 后，原版结构的 `has_structure` 标签不会自动认它们（`mineshaft` / `trial_chambers` / `ocean_ruin_*` / `ruined_portal_*`），要把新 id 补进同名 tag，否则原版结构凭空消失。
- 2.0.0 起 `data/minecraft/` 下**不能**再出现 worldgen/dimension JSON；`port_worldgen.js` 是按老结构生成到那里的，必须紧接 `migrate_worldtype.js` 迁移并删除，别把临时覆盖提交进仓库。
- 旧存档的世界生成参数存在 `level.dat` 里，但其中是注册表**引用**（`minecraft:overworld` noise settings、`minecraft:*` 群系 id）：删掉这些覆盖后，旧存档新生成区块会回落到原版，已生成区块不变。要完整的废土体验请开新世界。

## 10. Git / CI / 发布

- 分支：`main`；远程 `origin` = `https://github.com/litai1hao-bot/Rewasteland.git`。
- 当前 main：`2.0.0` 世界类型改造（2026-11-09，`mod_version=2.0.0`）；此前 `1.1.3.6`（de7d3fc，CI #11 success）。
- 2.0.0 是**破坏性**变更：删除 `data/minecraft` 下的 worldgen/dimension 覆盖、群系改 id、旧存档新区块回原版；发布时要在 Release 说明里写清楚。
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

酸水后续（1.1.0 遗留）：① runClient 目视确认酸绿贴图/水下雾/遮罩/桶贴图；② 结构水（海底神殿/海底废墟/要塞）是否也转酸水；③ 桶的装/倒交互实测。

2.0.0 后续：① runClient 目视确认「世界类型」列表里出现「废土世界」并可选（tag 机制已按源码核对，UI 未目视）；② 第三方建筑 mod 兼容（影子副本：复制对方 structure JSON 换 id + biomes，模板引用对方命名空间，见记忆/文档）；③ 可以把 `is_wasteland` 标签扩展成包含更多废土群系，让自定义结构在老版改皮群系里也生成。

## 12. 新会话检查清单

1. `git status` / `git log --oneline -n 5` 看有没有未提交改动、远程是否同步。
2. 读本文件 + `README.md`。
3. `.\gradlew.bat build` 确认能编译。
4. 改世界生成 → 先跑 `port_worldgen.js`（生成到 data/minecraft）→ 再跑 `migrate_worldtype.js`（迁到 rewasteland 并删除临时覆盖）→ build → `test_worldtype.ps1 -LevelType rewasteland:wasteland` 实测。
5. 改玩法 → build 后用 dev 服 + RCON / 调试命令验证；改水/流体再跑 `test_acidwater.ps1` + `test_acid_env.ps1`；改世界类型/酸雨作用域跑 `test_worldtype.ps1` 两种 level-type 各一次。
6. 提交前看 `git status`，**确认 `src/main/resources/data/minecraft/` 下只有 tags、没有 worldgen/dimension**，别把 `run/`、`build/`、`.dsh-meow/` 带进去。
