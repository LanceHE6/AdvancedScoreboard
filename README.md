![AdvancedScoreboard](https://socialify.git.ci/LanceHE6/AdvancedScoreboard/image?custom_description=%E4%B8%80%E4%B8%AA%E9%92%88%E5%AF%B9MC%E6%9C%8D%E5%8A%A1%E7%AB%AF%E7%9A%84%EF%BC%8C%E8%83%BD%E6%98%BE%E7%A4%BA%E6%9B%B4%E5%A4%9A%E8%AE%A1%E5%88%86%E6%9D%BF%E7%9A%84mod&description=1&font=Inter&forks=1&issues=1&language=1&name=1&pattern=Charlie+Brown&pulls=1&stargazers=1&theme=Dark)

AdvancedScoreboard 是一个基于 Fabric 开发的 Minecraft 模组，旨在为服务器提供可配置、自动化的计分板管理能力，支持多计分板轮播展示、玩家数据自动同步、配置热更新等特性，开箱即用地提供挖掘量、放置量、在线时长、鞘翅飞行距离、受到伤害、死亡次数、击杀生物数、延迟显示等维度的计分板统计。

## 功能特性

### 核心功能
1. **多维度计分板统计**
    - [x] 挖掘量：统计玩家破坏方块的数量
    - [x] 放置量：统计玩家放置方块的数量
    - [x] 在线时长：基于游戏内置统计，自动计算玩家在线小时数
    - [x] 鞘翅飞行距离：统计玩家使用鞘翅飞行的公里数
    - [x] 受到伤害：统计玩家受到的伤害值
    - [x] 死亡次数：统计玩家的死亡次数
    - [x] 击杀生物数：统计玩家击杀生物的数量（不包含击杀玩家）
    - [x] 延迟显示：在 TAB 玩家列表中按颜色显示延迟大小
2. **每玩家独立轮播**：每位玩家拥有独立的计分板轮播索引，通过纯服务端数据包方案实现自定义渲染，无需修改客户端
3. **数据自动同步**：定时将配置数据同步到游戏内计分板，并自动保存到本地配置文件
4. **实时刷新**：方块破坏/放置/击杀等实时事件立即刷新受影响玩家的计分板显示
5. **配置热加载**：修改配置文件后无需重启，切换/保存间隔实时生效
6. **默认配置初始化**：首次启动自动生成默认配置，支持配置加载失败自动回退
7. **命令系统**
    - [x] 查询榜单：玩家可通过 `/asb scoreboard` 指令查询任意榜单的全部数据
    - [x] 配置项修改：OP 可通过 `/asb set` 指令动态修改轮播间隔、保存间隔、最大显示数量、边框
    - [x] 全局显示控制：OP 可通过 `/asb notDisplay` 指令全局隐藏/显示指定榜单

### 指令系统

```text
/asb
├─ set                          # 仅 OP 可用
│  ├─ switchInterval <value>    # 修改轮播间隔（秒，最小 1）
│  ├─ saveInterval <value>      # 修改保存间隔（秒，最小 1）
│  ├─ maxDisplayNum <value>     # 修改榜单最大显示玩家数（最小 1）
│  └─ border <value>            # 修改榜单显示名边框（默认 "==="），即时生效
│
├─ scoreboard <displayName>     # 所有玩家可用，查询指定榜单的全部数据
│                               # 支持 Tab 自动补全 displayName
│
└─ notDisplay <displayName>     # OP 玩家可用，切换指定榜单的显示/隐藏状态
                                # 支持 Tab 自动补全 displayName
```

### 扩展特性
- **每玩家独立渲染**：每位玩家拥有独立的计分板展示，轮播索引互不干扰
- 支持自定义计分板名称和显示名
- OP 可全局控制榜单显示/隐藏，轮播时自动跳过已隐藏的榜单
- 服务器启动时自动同步配置数据，保证数据一致性
- 配置文件兼容旧版本，新增字段自动设置默认值
- 玩家断开连接时自动清理其虚拟计分板数据

## 实现原理

### 整体架构
采用「配置驱动 + 事件监听 + 定时任务 + 每玩家独立数据包渲染」的核心架构：

1. **初始化流程**（`AdvancedScoreboard.java`）
    - 加载/初始化全局配置
    - 注册指令系统（`/asb set`、`/asb scoreboard`、`/asb notDisplay`）
    - 使用 Fabric API 的 `UseBlockCallback` 替代 Mixin 注入实现方块放置计数
    - 注册服务器启动事件、玩家断连事件
2. **服务器启动事件**（`ServerStartedEvent.java`）
    - 初始化全局计分板实例
    - 注册配置中的计分板，同步已持久化的玩家数据
    - 注册延迟 objective 到 LIST 显示槽（TAB 列表）
    - 启动轮播切换、数据同步定时任务
    - 注册玩家断连事件，清理虚拟计分板
3. **定时任务**（`Task.java`）
    - **计分板轮播**：由 `ServerTick` 事件驱动，根据 `hiddenScoreboards` 过滤隐藏榜单，为每位玩家独立生成轮播索引
    - **数据同步**：定时从 Minecraft Stats 读取在线时长/飞行距离/伤害/死亡数据，按 `maxDisplayNum` 截取前 N 名同步到游戏内计分板
    - **延迟同步**：每秒读取所有玩家的延迟数据，按延迟区间着色显示在 TAB 列表
4. **自定义渲染器**（`CustomScoreboardRenderer.java`）
    - 为每位玩家创建独立的虚拟计分板（`asb_v_{uuid}`）
    - 通过 `ClientboundSetObjectivePacket` / `ClientboundSetScorePacket` / `ClientboundSetDisplayObjectivePacket` 等原生数据包实现计分板渲染
    - 轮播切换时先清除旧条目再发送新数据，确保客户端显示正确
5. **配置管理**（`Config.java`/`ScoreboardItem.java`）
    - 基于 Jackson 实现 JSON 配置的加载/保存
    - 支持默认配置初始化、旧版配置兼容（新增字段自动填充默认值）
6. **事件监听**
    - 方块破坏/放置/击杀生物事件实时更新数据并调用 `Task.refreshDisplayForItem()` 即时刷新

### 配置说明
配置文件路径：`config/advanced_scoreboard.json`

#### 核心配置项
```json
{
  "border": "===",
  "switchInterval": 5,
  "saveInterval": 5,
  "maxDisplayNum": 15,
  "hiddenScoreboards": [],
  "scoreboards": [
    {
      "internalName": "mine_count",
      "displayName": "挖掘量",
      "data": {}
    },
    {
      "internalName": "place_count",
      "displayName": "放置量",
      "data": {}
    },
    {
      "internalName": "online_time",
      "displayName": "在线时长(h)",
      "data": {}
    },
    {
      "internalName": "elytra_dist",
      "displayName": "飞行距离(km)",
      "data": {}
    },
    {
      "internalName": "damage_taken",
      "displayName": "受到伤害",
      "data": {}
    },
    {
      "internalName": "deaths",
      "displayName": "死亡次数",
      "data": {}
    },
    {
      "internalName": "mob_kills",
      "displayName": "击杀生物数",
      "data": {}
    },
    {
      "internalName": "latency",
      "displayName": "延迟(ms)",
      "data": {}
    }
  ]
}
```

#### 自定义计分板
1. 修改 `displayName` 可自定义计分板显示名称
2. 新增条目到 `scoreboards` 数组即可注册新的计分板
3. 可通过修改 `data` 字段手动设置玩家分数（需等待同步周期生效）

## 开发相关

### 环境要求
- JDK 25+
- Gradle 9.x（已配置 `org.gradle.toolchains.foojay-resolver-convention` 自动下载 JDK）

### 构建
```bash
# 编译全部版本模块并收集 JAR 到 dist/
./gradlew buildAll

# 编译指定模块
./gradlew :mc-26.2:build

# 运行服务端测试
./gradlew :mc-26.2:runServer
```

### 项目结构（多版本）
```
├── common/                                # 跨版本共享代码
│   └── src/
│       ├── main/
│       │   ├── java/cn/hycer/advancedscoreboard/
│       │   │   ├── AdvancedScoreboard.java        # 模组主类
│       │   │   ├── Command/                       # 指令系统
│       │   │   ├── Config/                        # 配置管理
│       │   │   ├── Event/                         # 事件监听
│       │   │   ├── Global/                        # 全局变量
│       │   │   ├── Task/                          # 定时任务
│       │   │   ├── render/                        # 自定义渲染
│       │   │   │   └── CustomScoreboardRenderer.java
│       │   │   └── mixin/                         # Mixin
│       │   │       └── ServerCommonPacketListenerImplAccessor.java
│       │   └── resources/
│       │       ├── advancedscoreboard.mixins.json
│       │       └── log4j2.xml
│       └── client/                                # 客户端代码
├── mc-26.2/                               # Minecraft 1.21.4
│   ├── build.gradle
│   └── src/main/resources/fabric.mod.json
├── mc-26.1.2/                             # Minecraft 1.21.2
│   ├── build.gradle
│   └── src/main/resources/fabric.mod.json
├── mc-26.1.1/                             # Minecraft 1.21.1
│   ├── build.gradle
│   └── src/main/resources/fabric.mod.json
├── mc-26.1/                               # Minecraft 1.21
│   ├── build.gradle
│   └── src/main/resources/fabric.mod.json
├── build.gradle                           # 根构建（subprojects + buildAll）
├── settings.gradle                        # 多模块引入
└── gradle.properties                      # 全局属性
```

### 多版本架构
- **`common/`**：存放所有版本通用的 Java 源码和资源文件（事件、配置、渲染、指令等）
- **`mc-{version}/`**：每个 Minecraft 版本一个模块，仅包含版本特定的构建配置和 `fabric.mod.json`
- 当某个版本需要不同的代码时，在该版本模块的 `src/main/java/` 下添加同名类即可覆盖 `common/` 的实现

### 版本模块说明
| 模块 | Minecraft | Fabric API | Loader |
|------|-----------|------------|--------|
| mc-26.1 | 26.1 | 0.145.1+26.1 | 0.19.3 |
| mc-26.1.1 | 26.1.1 | 0.145.4+26.1.1 | 0.19.3 |
| mc-26.1.2 | 26.1.2 | 0.152.1+26.1.2 | 0.19.3 |
| mc-26.2 | 26.2 | 0.152.1+26.2 | 0.19.3 |
