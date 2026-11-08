# ReWasteland

废土生存 **NeoForge** 模组，目标版本 **Minecraft 1.21.1**（NeoForge 21.1.x，Java 21）。

把「酸雨 + 变异 + 废土世界」这一套玩法，以 NeoForge 模组的形式在 1.21.1 上重建。
玩法参考 [Acid Wasteland](https://modrinth.com/project/acid-wasteland)（数据包，MIT），
本仓库是它的 1.21.1 / NeoForge 移植与扩展，数据部分为重新生成/降级改写，不是原样搬运。

开发与维护说明（环境、目录、世界生成调参、测试工具）见 [DEVELOPMENT.md](DEVELOPMENT.md)。

## 当前进度

| 模块 | 状态 | 说明 |
|---|---|---|
| 世界生成 | ✅ | 自定义 overworld 气候表（18 群系）、全局压平地形（无高山）、沙漠缩减、地表换土、全树枯枝 |
| 结构 | ✅ | 荒屋 ×4、幸存者营地 ×5，按群系标签生成 |
| 结构战利品 | ✅ | 外置到 `config/rewasteland/structure_looting/`，可逐表改 JSON |
| 酸水方块 | ✅ | 所有自然水换成 `rewasteland:acid_water`；流体/方块/桶，绿色外观；普通水安全 |
| 酸雨中毒 | ✅ | 酸水 / 露天降雨累积暴露值，3 级无限中毒；参数在 `config/rewasteland/acid_rain.json` |
| 变异（吃食物触发） | 🚧 | 计划中 |
| 怪物变种与装备 | 🚧 | 计划中 |
| 自定义天气机 | 🚧 | 计划中 |
| 食物/物品改皮 | 🚧 | 计划中 |

## 运行要求

- Minecraft **1.21.1**
- **NeoForge 21.1.x**（开发用 21.1.265）
- **Java 21**

## 构建

```bash
./gradlew build          # 产物：build/libs/rewasteland-1.1.0.jar
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

首次启动会生成 `config/rewasteland/acid_rain.json`（同目录带 `README-acid-rain.txt`）：

- 在酸水中（脚或头所在方块）或露天降雨 / 雷雨时，每 tick 累积 1 点暴露值；
  干燥时每 tick 减少 1 点，上限 1200。普通水默认安全。
- 200 / 600 / 1000 点分别进入中毒 0 / I / II（无限时长），第三级额外冒绿色粒子。
- 默认 10 秒 / 30 秒 / 50 秒连续暴露进入三级；创造模式默认不受影响
  （`affect_creative_players` 可开）。
- 调试：op 可用 `/rwacid <ticks> <exposed>` 强制暴露、`/rwacid auto <ticks>` 按环境判定，`/rwacid reset` 清空。

## 酸水方块

- 自然生成的水（海洋 / 河流 / 湖泊 / 洞穴含水层 / 泉水）全部替换为 `rewasteland:acid_water`；
  沿用原版水的流动、溺水、船等行为，外观复用原版水贴图 + 酸绿 tint。
- 酸水没有加进 `#minecraft:water` 标签：玻璃瓶 / 海绵 / 甘蔗 / 耕地 / 钓鱼不会把它当水；
  普通水保持安全。水桶只能从雨天炼药锅装净水。
- 海底神殿 / 海底废墟 / 要塞内部仍会生成原版水（结构代码硬编码，后续再处理）。
- 调试：`/place feature minecraft:spring_water` 应生成酸水；用 `/rwacid auto` 在酸水里应累计暴露，在普通水里不累计。

## 目录约定

- 所有自定义内容统一使用 **`rewasteland`** 命名空间（群系、战利品表、结构、配置目录）。
- 覆盖原版的部分放在 `src/main/resources/data/minecraft/...`。

## 许可

[MIT](LICENSE)。数据与世界生成部分派生自 **Acid Wasteland**
（MIT，Copyright (c) 2025 yoyolee-minecraft），详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
项目脚手架来自 NeoForge MDK 模板（见 `TEMPLATE_LICENSE.txt`）。
