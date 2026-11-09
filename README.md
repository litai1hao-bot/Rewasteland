# ReWasteland

废土生存 **NeoForge** 模组，目标版本 **Minecraft 1.21.1**（NeoForge 21.1.x，Java 21）。

把「酸雨 + 变异 + 废土世界」这一套玩法，以 NeoForge 模组的形式在 1.21.1 上重建。
玩法参考 [Acid Wasteland](https://modrinth.com/project/acid-wasteland)（数据包，MIT），
本仓库是它的 1.21.1 / NeoForge 移植与扩展，数据部分为重新生成/降级改写，不是原样搬运。

开发与维护说明（环境、目录、世界生成调参、测试工具）见 [DEVELOPMENT.md](DEVELOPMENT.md)；玩家/服主使用说明见 [MANUAL.md](MANUAL.md)。

## 当前进度

| 模块 | 状态 | 说明 |
|---|---|---|
| 世界类型 | ✅ | 独立世界类型 **`rewasteland:wasteland`**：创建世界时像原版默认/超平坦/放大化一样选择，服务器用 `level-type=rewasteland:wasteland`；不选就是原版世界 |
| 世界生成 | ✅ | 自定义 overworld 气候表（18 群系）、适度起伏地形（6 格下移，低地多水、无高山）、沙漠缩减、地表换土、全树枯枝、窄深河流；洞穴保持原版，建议另装 NoCaves |
| 结构 | ✅ | 荒屋 ×4、幸存者营地 ×5，按群系标签生成 |
| 结构战利品 | ✅ | 外置到 `config/rewasteland/structure_looting/`，可逐表改 JSON |
| 第三方结构兼容 | ✅ | NTM-Continuum：废土群系接入它的 1.7 生物群系字典，废土/沙漠/海洋类结构都能生成 |
| 酸水方块 | ✅ | 废土世界里自然水换成 `rewasteland:acid_water`；流体/方块/桶，绿色外观；普通水安全 |
| 酸雨中毒 | ✅ | 只在废土世界：酸水 / 露天降雨累积暴露值；200/600/1000 每秒刷新 5 秒中毒 0/I/II，1200 上限额外凋零；参数在 `config/rewasteland/acid_rain.json` |
| 变异（吃食物触发） | 🚧 | 计划中 |
| 怪物变种与装备 | 🚧 | 计划中 |
| 自定义天气机 | 🚧 | 计划中 |
| 食物/物品改皮 | 🚧 | 计划中 |

## 世界类型

1.2.0 起世界生成是**可选**的，不再直接改写原版主世界：

- 单人：创建新世界 →「世界类型」里选 **废土世界**（`rewasteland:wasteland`）；选默认/超平坦/放大化就是原版世界。
- 服务器：`server.properties` 里 `level-type=rewasteland:wasteland`（不写 = `minecraft:normal`）。
- 酸雨、酸水、废土地形、自定义结构只在废土世界类型里出现；同一个 mod 装着的原版世界保持普通水、普通天气、原版群系。
- 1.1.3 及更早的旧存档创建时是“默认”世界类型，升级到 1.2.0 后新生成的区块会回到原版外观（已生成区块不变）；要完整体验请开新世界。

## 第三方模组兼容

- **NTM-Continuum（HBM）**：1.2.0 起自带兼容标签（`data/hbm/tags/worldgen/biome/biome_dictionary/*`），废土世界里的沙漠、海洋、河岸、平原类 NTM 结构会正常生成；NTM 不在时这些标签不产生任何影响。
- **TNew Apocalypse Structure / The Lost City**：1.2.0 起兼容结构**内置在模组里**（`compat_data/`，检测到对应模组才会注册为内建数据包，并按版本选包：Lost City 1.2.x / 1.5.x），不需要手动往 `world/datapacks` 放包；需要新世界或未探索的区块才会出现。The Lost City 还需要 `berezka_api`。
- 需要 `hbm` 和 `golemcore` 两个 1.21.1 NeoForge jar；NTM 的 `/locate` 很慢（单次数十秒到数分钟）属于它自身实现，和本模组无关。
- **性能提示**：NTM 在非原版群系上生成结构很重（默认配置下出生区生成从 3.5 秒涨到 121 秒）。建议把 `config/hbm.cfg` 的 `5.02_structureMaxChunks` 调到 32、`5.01_structureMinChunks` 调到 8；实测可回到 ~3.8 秒，代价是 NTM 结构更稀。

## 运行要求

- Minecraft **1.21.1**
- **NeoForge 21.1.x**（开发用 21.1.265）
- **Java 21**

## 构建

```bash
./gradlew build          # 产物：build/libs/rewasteland-1.2.0.jar
./gradlew runClient      # 开发客户端
./gradlew runServer      # 开发服务端（test world 在 run/）
```

## 配置

首次启动会在服务端/客户端配置目录生成：

```
config/rewasteland/structure_looting/
├── README.txt
├── survivor_camp_scraps.json
└── survivor_camp_tools.json
```

每个 JSON 用 `"loot_table"` 声明要接管的战利品表，`pools` 里写条目（`item` / `weight` /
`count` / `name` / `lore` / `components` / `custom_data` / `enchant_randomly`）。
改完 `/reload` 或重进世界生效；写错只会在日志报错并保留内置表，不会空箱或崩档。

结构默认使用：

- `rewasteland:chests/survivor_camp_scraps` —— 营地/荒屋的杂物桶
- `rewasteland:chests/survivor_camp_tools` —— 工具桶（含撬棍）

营地的大箱子仍使用原版村庄战利品表；如果想一起接管，新建一个
`"loot_table": "minecraft:chests/village/village_armorer"` 的 JSON 即可。

## 酸雨 / 水中毒

**只在废土世界类型生效**（原版/超平坦/放大化世界完全不受影响）。
首次启动会生成 `config/rewasteland/acid_rain.json`（同目录带 `README-acid-rain.txt`）：

- 在酸水中（脚或头所在方块）或露天降雨 / 雷雨时，每 tick 累积 1 点暴露值；
  干燥时每 tick 减少 1 点，上限 1200。普通水默认安全。
- 200 / 600 / 1000 点每秒刷新一次 5 秒的中毒 0 / I / II，第三级额外冒绿色粒子；
  到 1200 点上限时还会每秒刷新 5 秒凋零 0（会致死，不能靠一血硬抗）。
- 默认 10 秒 / 30 秒 / 50 秒连续暴露进入三级；创造模式默认不受影响
  （`affect_creative_players` 可开）。
- 调试：op 可用 `/rwacid <ticks> <exposed>` 强制暴露、`/rwacid auto <ticks>` 按环境判定，`/rwacid reset` 清空。

## 酸水方块

- 自然生成的水（海洋 / 河流 / 湖泊 / 洞穴含水层 / 泉水）全部替换为 `rewasteland:acid_water`；
  沿用原版水的流动、溺水、船等行为，外观是自定义的酸绿静止/流动贴图与绿色水下遮罩。
- 群系水色已重置为原版蓝：结构/水桶里的原版水是蓝色，绿色只代表酸水。
- 酸水没有加进 `#minecraft:water` 标签：玻璃瓶 / 海绵 / 甘蔗 / 耕地 / 钓鱼不会把它当水；
  普通水保持安全。水桶只能从雨天炼药锅装净水。
- 海底神殿 / 海底废墟 / 要塞内部仍会生成原版水（结构代码硬编码，后续再处理）。
- 调试：`/place feature minecraft:spring_water` 应生成酸水；用 `/rwacid auto` 在酸水里应累计暴露，在普通水里不累计。

## 目录约定

- 所有自定义内容统一使用 **`rewasteland`** 命名空间（群系、世界类型、战利品表、结构、配置目录）。
- 世界生成（维度/噪声/密度函数/群系/feature）全部在 `data/rewasteland/...`；`data/minecraft/` 只放 tag（世界类型列表、结构群系标签、`is_overworld` 兼容标签）。

## 许可

[MIT](LICENSE)。数据与世界生成部分派生自 **Acid Wasteland**
（MIT，Copyright (c) 2025 yoyolee-minecraft），详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
项目脚手架来自 NeoForge MDK 模板（见 `TEMPLATE_LICENSE.txt`）。
