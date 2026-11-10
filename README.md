# ReWasteland

废土生存 **NeoForge** 模组，目标版本 **Minecraft 1.21.1**（NeoForge 21.1.x，Java 21）。

把「废土世界 + 酸雨 + 酸水」这一套玩法在 1.21.1 上重建。玩法参考
[Acid Wasteland](https://modrinth.com/project/acid-wasteland)（数据包，MIT），
本仓库是它的 1.21.1 / NeoForge 移植与扩展，数据部分为重新生成/降级改写。

开发与维护说明见 [DEVELOPMENT.md](DEVELOPMENT.md)。

## 这是什么

- **可选世界类型** `rewasteland:wasteland`：创建世界时像原版的默认/超平坦/放大化一样选择；不选就是原版世界。
- **废土地形**：18 个群系、整体压平无高山、枯树、窄深河流、缩减沙漠；洞穴保持原版（建议另装 NoCaves）。
- **酸水与酸雨**：自然水全部替换为 `rewasteland:acid_water`；露天淋雨或泡酸水会累积暴露值，按档位中毒/凋零；普通水安全。
- **天气机**：废土世界里关掉原版随机天气，按「晴 → 雨 →（10% 雷暴）→ 小雨 → 晴」循环，时长随机；配置在 `config/rewasteland/weather_cycle.json`，`/rwweather` 调试。
- **结构**：荒屋 ×4、幸存者营地 ×5；战利品外置到 `config/rewasteland/structure_looting/`，可逐表改 JSON。
- **第三方结构兼容**：NTM-Continuum、TNew Apocalypse Structure、The Lost City，内置并按源模组版本自动选择兼容包。

## 世界类型

- 单人：创建新世界 →「世界类型」里选 **废土世界**（`rewasteland:wasteland`）；选默认/超平坦/放大化就是原版世界。
- 服务器：`server.properties` 里 `level-type=rewasteland:wasteland`（不写 = `minecraft:normal`）。
- 酸雨、酸水、废土地形、自定义结构只在废土世界类型里出现。
- 1.1.3 及更早的旧存档升级后，新生成的区块会回到原版外观（已生成区块不变）；要完整体验请开新世界。

## 第三方模组兼容

| 模组 | 要求 | 说明 |
|---|---|---|
| NTM-Continuum (hbm) | `hbm` + `golemcore`（1.21.1） | 废土/沙漠/海洋/河岸/平原类结构可生成；建议把 `config/hbm.cfg` 的 `5.02_structureMaxChunks` 调到 32、`5.01_structureMinChunks` 调到 8，否则出生区生成会从 3.5 秒涨到 121 秒 |
| TNew Apocalypse Structure | 1.0.1 | 24 个结构自动加入废土/沙漠 |
| The Lost City | 1.2.x 或 1.5.x，需 `berezka_api` | 内置按版本选包；未知版本会跳过兼容并打日志（不会崩） |

兼容结构只在新世界或未探索的区块生成；用 `/locate structure <id>` 验证。

## 当前进度

| 模块 | 状态 |
|---|---|
| 世界类型 / 世界生成 | ✅ |
| 结构 + 结构战利品 | ✅ |
| 酸水 / 酸雨 | ✅ |
| 第三方结构兼容 | ✅ |

## 未来计划

- 撬棍贴图 / 模型。

## 运行要求

- Minecraft **1.21.1**
- **NeoForge 21.1.x**（开发用 21.1.265）
- **Java 21**

## 构建

```bash
./gradlew build      # 产物：build/libs/rewasteland-1.3.0.jar
```

## 目录约定

- 自定义内容统一使用 `rewasteland` 命名空间（群系、世界类型、战利品表、结构、配置目录）。
- 世界生成全部在 `data/rewasteland/...`；`data/minecraft/` 只放 tag。

## 许可

[MIT](LICENSE)。数据与世界生成部分派生自 **Acid Wasteland**（MIT，Copyright (c) 2025 yoyolee-minecraft），
详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。项目脚手架来自 NeoForge MDK 模板（见 `TEMPLATE_LICENSE.txt`）。
