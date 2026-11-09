# ReWasteland 使用手册（1.2.0）

面向玩家与服主。开发/改数据请看 [DEVELOPMENT.md](DEVELOPMENT.md)。

---

## 1. 这是什么

ReWasteland 是一个 **Minecraft 1.21.1 / NeoForge** 的废土生存模组：

- 独立的**废土世界类型**（像原版的默认/超平坦/放大化一样选择），不是强制替换原版主世界；
- 干裂的地表、枯树、缩减的沙漠、变窄变深的河流；
- 自然水全部替换成**酸水**，下雨是**酸雨**，暴露过量会中毒/凋零；
- 自定义结构：荒屋、幸存者营地，并有可选的第三方建筑模组兼容。

---

## 2. 环境要求

| 项 | 要求 |
|---|---|
| Minecraft | **1.21.1** |
| NeoForge | **21.1.x**（开发用 21.1.265） |
| Java | **21** |
| 客户端/服务端 | 都可以；单人、局域网、专用服务器均可 |

---

## 3. 安装

1. 安装对应版本的 NeoForge；
2. 把 `rewasteland-1.2.0.jar` 放进 `mods/`；
3. 启动游戏/服务器。

不需要额外前置模组。第三方建筑兼容见第 8 节。

---

## 4. 创建世界

### 4.1 单人

创建新世界 →「世界类型」里选择 **废土世界（Wasteland）**。

选「默认 / 超平坦 / 放大化」就是普通原版世界，本模组不生效（酸雨、酸水、废土地形都不会出现）。

### 4.2 服务器

`server.properties`：

```properties
level-type=rewasteland:wasteland
```

不写则是原版默认世界。

### 4.3 已有存档

- **用当前版本（1.2.0）新建的废土世界**继续正常。
- **1.1.3 及更早**的旧存档：它创建时还是“默认世界 + 覆盖数据”，升级后**已生成的区块不变，新生成的区块会回到原版外观**。想要完整体验请开新世界。
- 换世界类型不会删除存档，但一个世界创建后类型就固定了。

---

## 5. 世界内容

- **18 个群系**：13 个改皮的废土化群系（badlands、beach、海洋、河流、stone 系列、wooded badlands 等）、2 个洞穴群系（滴水石洞、繁茂洞穴）、自定义的 `rewasteland:wasteland` 与 `rewasteland:desert`。
- **地形**：整体压平、无高山，内陆略低于海平面，河流窄而深。
- **水**：自然水（海洋/河流/湖泊/含水层/泉水）全部为**酸水**。
- **结构**：自定义荒屋 ×4、幸存者营地 ×5；原版结构（矿井、试炼密室、海底废墟等）按原分布保留。
- **天气**：酸雨只在废土世界的露天环境生效。

---

## 6. 酸雨与酸水

### 6.1 规则

- 站在**酸水**里，或在废土世界**露天淋雨**：暴露值每 tick +1；
- 干燥时每 tick −1，上限 1200；
- 每 20 tick（1 秒）按当前档位刷新一次 5 秒效果：

| 暴露值 | 效果 |
|---|---|
| 200–599 | 中毒 0 |
| 600–999 | 中毒 I |
| 1000+ | 中毒 II + 绿色粒子 |
| 1200 | 额外刷新凋零 0（会致死） |

- **普通水是安全的**；创造模式默认不受影响。

### 6.2 配置

首次启动生成 `config/rewasteland/acid_rain.json`（同目录有 README）：

```json
{
  "enabled": true,
  "acid_water_toxic": true,
  "plain_water_toxic": false,
  "rain_is_acidic": true,
  "affect_creative_players": false,
  "exposure_gain_per_tick": 1,
  "exposure_loss_per_tick": 1,
  "max_exposure": 1200,
  "tier1_threshold": 200,
  "tier2_threshold": 600,
  "tier3_threshold": 1000
}
```

改完重进世界生效。

### 6.3 调试命令（需要管理员）

```text
/rwacid <ticks> <true|false>   强制暴露/干燥 N tick
/rwacid auto <ticks>           按真实环境判定
/rwacid reset                  清空状态
```

---

## 7. 结构战利品

结构里的箱子/木桶战利品外置到：

```text
config/rewasteland/structure_looting/
├── README.txt
├── survivor_camp_scraps.json
└── survivor_camp_tools.json
```

每个 JSON 用 `"loot_table"` 声明要接管哪张表，`pools` 里写条目（`item`、`weight`、`count`、`name`、`lore`、`components`、`custom_data`、`enchant_randomly`）。
改完 `/reload` 或重进世界生效；写错只会在日志报错并保留内置表，不会空箱或崩档。

---

## 8. 第三方模组兼容

兼容结构**内置在模组里**，检测到对应模组且**版本匹配**时自动启用，不需要手动放数据包。

> 通用前提：兼容结构只在**新世界或未探索的区块**生成；旧区块不会补。用 `/locate structure <id>` 验证。

### 8.1 NTM-Continuum（HBM）

需要：

- `hbm-neoforge-<版本>+1.21.1.jar`
- `golemcore-neoforge-<版本>+1.21.1.jar`（两个都要）

效果：废土/沙漠、海洋、河岸、平原类 NTM 结构可以生成；结构战利品、装饰方块的缺失取决于源模组自身。

**性能（重要）**：NTM 在非原版群系上生成结构非常重。推荐改 `config/hbm.cfg`：

```ini
I:5.01_structureMinChunks=8
I:5.02_structureMaxChunks=32
```

默认 `16/4` 时出生区生成可能从 3.5 秒涨到 2 分钟。改稀后实测回到 ~4 秒。

已知限制：

- `hbm:submarine` 不会生成（按原版海洋 id 精确匹配）；
- 雪地/丛林类地牢（Arctic Vault、Jungle Dungeon、Military Base）需要雪地/丛林群系，本世界没有；
- NTM 的 `/locate` 很慢（单次数十秒到数分钟），属于它自身实现；调试时可把 `server.properties` 的 `max-tick-time` 设为 `-1`，避免 60 秒看门狗杀服。

### 8.2 TNew Apocalypse Structure（TNAS）

- 支持版本：**1.0.1**；
- 24 个结构（房屋、塔、机场、地下掩体）自动加入废土/沙漠群系；
- 不需要额外前置。

### 8.3 The Lost City

- 支持版本：**1.2.x**（`compat_data/lostcity_1_2`）与 **1.5.x**（`compat_data/lostcity_1_5`）；
- 依赖 **berezka_api**（1.5.x 需要 1.2.9.x 的 API）；
- 1.5.0-beta.1 提供 13 个结构（城市、营地、工厂、无限城、灯塔、哨塔、铁路、公路等）。

**版本门禁**：如果安装的是其他版本，模组会在日志打 warning 并**跳过兼容包**——不会崩溃，但也没有这些结构。这是因为不同版本的模板池 id 会改名，配错会导致创建世界崩溃（`Unbound values in ... template_pool`）。

### 8.4 兼容结构不出现时怎么排查

1. 源模组是否安装、版本是否在支持列表；
2. 是否在**新世界/未探索区块**（旧区块不补）；
3. `/locate structure rewasteland:tnas_spafner` 之类是否命中；
4. 看日志有没有 `Registered built-in structure pack` 或 `no built-in structure bridge for ...`。

---

## 9. 性能与优化

其他可用的优化模组（1.21.1 NeoForge）：

| 模组 | 作用 |
|---|---|
| C2ME (NeoForge) | 并行化区块生成（alpha，谨慎） |
| Noisium | 世界生成噪声优化 |
| Chunky | 预生成区块 |
| ModernFix | 通用性能/内存 |
| FerriteCore | 降低内存/GC |
| ServerCore | 服务端优化 |
| Spark | 性能分析 |
| Sodium / ImmediatelyFast / EntityCulling / Distant Horizons | 客户端渲染/视距 |

另外：**NTM 结构间距**是最大变量（见 8.1）；不想用 NTM 结构可以设 `5.00_enableStructures=false`，它的矿物/流体世界生成不受影响。

---

## 10. 常见问题

**Q：世界类型里没有「废土世界」？**
A：确认装的是 1.2.0 的 jar；创建世界界面里点开「世界类型」按钮轮换。

**Q：生成出来是原版世界？**
A：单人没选世界类型；服务器没写 `level-type=rewasteland:wasteland`。

**Q：兼容结构一个都没有？**
A：源模组缺失/版本不在支持列表/旧区块已生成。见 8.4。

**Q：创建世界崩溃，日志有 `Unbound values in ... template_pool`？**
A：安装的源模组版本与内置兼容包不匹配。要么升级 ReWasteland（会按版本匹配），要么把源模组换回支持版本。1.2.0 已对未知版本跳过兼容包，正常情况下不会再因此崩溃。

**Q：装 NTM 后卡顿严重？**
A：先按 8.1 改 `hbm.cfg` 的 structure spacing；仍卡可关闭 NTM 结构或加 C2ME。

**Q：旧存档升级后地形变了/没有酸水了？**
A：1.1.3 及更早的存档是旧机制创建的，新区块会回原版。开新世界，或用旧版本继续玩该存档。

**Q：结构里的水是普通水？**
A：海底神殿/要塞等原版结构的水是代码硬编码的，数据包无法替换；本模组只替换自然水。

---

## 11. 版本变更摘要

- **1.2.0**：TNAS / The Lost City 兼容结构内置（按源模组版本选包）；1.1.5 起的 NTM 兼容、世界类型、沙漠气候修复等全部包含。
- **1.1.4**：世界生成改为可选世界类型 `rewasteland:wasteland`；数据全部迁到 `rewasteland` 命名空间。
- **1.1.0**：酸水方块（自然水替换、酸雨系统重做）。
- **1.0.0**：首个版本（世界生成 + 结构 + 结构战利品）。

---

## 12. 许可与致谢

- 许可：[MIT](LICENSE)。
- 世界生成/结构数据派生自 **Acid Wasteland**（MIT），结构兼容参考 NTM-Continuum、TNew Apocalypse Structure、The Lost City 的公开数据，仅引用其命名空间，不复制资源。
- 第三方模组版权归各自作者所有。
