# ReWasteland

废土生存 **NeoForge** 模组，目标版本 **Minecraft 1.21.1**（NeoForge 21.1.x，Java 21）。

把「酸雨 + 变异 + 废土世界」这一套玩法，以 NeoForge 模组的形式在 1.21.1 上重建。
玩法参考 [Acid Wasteland](https://modrinth.com/project/acid-wasteland)（数据包，MIT），
本仓库是它的 1.21.1 / NeoForge 移植与扩展，数据部分为重新生成/降级改写，不是原样搬运。

## 当前进度

| 模块 | 状态 | 说明 |
|---|---|---|
| 世界生成 | ✅ | 自定义 overworld 气候表（18 群系）、地表换土、全树枯枝、13 个原版群系废土化 |
| 结构 | ✅ | 荒屋 ×4、幸存者营地 ×5，按群系标签生成 |
| 结构战利品 | ✅ | 外置到 `config/rewasteland/structure_looting/`，可逐表改 JSON |
| 酸雨中毒 | 🚧 | 计划中 |
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
./gradlew build          # 产物：build/libs/rewasteland-1.0.0.jar
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

## 目录约定

- 所有自定义内容统一使用 **`rewasteland`** 命名空间（群系、战利品表、结构、配置目录）。
- 覆盖原版的部分放在 `src/main/resources/data/minecraft/...`。

## 许可

[MIT](LICENSE)。数据与世界生成部分派生自 **Acid Wasteland**
（MIT，Copyright (c) 2025 yoyolee-minecraft），详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
项目脚手架来自 NeoForge MDK 模板（见 `TEMPLATE_LICENSE.txt`）。
