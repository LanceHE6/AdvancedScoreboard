![AdvancedScoreboard](https://socialify.git.ci/LanceHE6/AdvancedScoreboard/image?custom_description=%E4%B8%80%E4%B8%AA%E9%92%88%E5%AF%B9MC%E6%9C%8D%E5%8A%A1%E7%AB%AF%E7%9A%84%EF%BC%8C%E8%83%BD%E6%98%BE%E7%A4%BA%E6%9B%B4%E5%A4%9A%E8%AE%A1%E5%88%86%E6%9D%BF%E7%9A%84mod&description=1&font=Inter&forks=1&issues=1&language=1&name=1&pattern=Charlie+Brown&pulls=1&stargazers=1&theme=Dark)



AdvancedScoreboard 是一个基于 Fabric 开发的 Minecraft 模组，旨在为服务器提供可配置、自动化的计分板管理能力，支持多计分板轮播展示、玩家数据自动同步、配置热更新等特性，开箱即用地提供挖掘量、放置量、在线时长、鞘翅飞行距离、受到伤害、死亡次数等维度的计分板统计。

## 功能特性

### 核心功能
1. **多维度计分板统计**
    - [x] 挖掘量：统计玩家破坏方块的数量
    - [x] 放置数量：统计玩家放置方块的数量
    - [x] 在线时长：基于游戏内置统计，自动计算玩家在线小时数
    - [x] 鞘翅飞行距离：统计玩家使用鞘翅飞行的公里数
    - [x] 受到伤害：统计玩家受到的伤害值
    - [x] 死亡次数：统计玩家的死亡次数
    - [ ] 击杀生物数：统计玩家击杀所有的生物数量
    - [ ] ...
2. **自动轮播切换**：配置化的计分板轮播
3. **数据自动同步**：定时将配置数据同步到游戏内计分板，并自动保存到本地配置文件
4. **配置热加载**：修改配置文件后无需重启，切换/保存间隔实时生效
5. **默认配置初始化**：首次启动自动生成默认配置，支持配置加载失败自动回退
6. **命令系统**
    - [x] 查询榜单：玩家可通过 `/asb scoreboard` 指令查询任意榜单的全部数据
    - [x] 配置项修改：OP 可通过 `/asb set` 指令动态修改轮播间隔、保存间隔、最大显示数量
    - [x] 全局显示控制：OP 可通过 `/asb notDisplay` 指令全局隐藏/显示指定榜单

### 指令系统

```text
/asb
├─ set                          # 仅 OP 可用
│  ├─ switchInterval <value>    # 修改轮播间隔（秒，最小 1）
│  ├─ saveInterval <value>      # 修改保存间隔（秒，最小 1）
│  ├─ maxDisplayNum <value>     # 修改榜单最大显示玩家数（最小 1）
│  └─ border <value>            # 修改榜单显示名边框（默认 "==="）
│
├─ scoreboard <displayName>     # 所有玩家可用，查询指定榜单的全部数据（不受最大显示数量限制）
│                               # 支持 Tab 自动补全 displayName
│
└─ notDisplay <displayName>     # OP 玩家可用，切换指定榜单的显示/隐藏状态
                                # 支持 Tab 自动补全 displayName
```

### 扩展特性
- 支持自定义计分板名称和显示名
- OP 可全局控制榜单显示/隐藏，轮播时自动跳过已隐藏的榜单
- 服务器启动时自动同步配置数据，保证数据一致性
- 配置文件兼容旧版本，新增字段自动设置默认值

## 实现原理

### 整体架构
采用「配置驱动 + 事件监听 + 定时任务」的核心架构：

1. **初始化流程**（`AdvancedScoreboard.java`）
    - 加载/初始化全局配置
    - 注册指令系统（`/asb set`、`/asb scoreboard`、`/asb notDisplay`）
    - 注册服务器启动、玩家破坏/放置方块等核心事件
2. **服务器启动事件**（`ServerStartedEvent.java`）
    - 初始化全局计分板实例
    - 注册配置中的计分板，同步已持久化的玩家数据
    - 启动轮播切换、数据同步定时任务
3. **定时任务**（`Task.java`）
    - 计分板轮播：由 `ServerTick` 事件驱动，根据 `hiddenScoreboards` 过滤隐藏榜单，通过 `setObjectiveSlot(SIDEBAR)` 全局切换显示
    - 数据同步：定时从 Minecraft Stats 读取在线时长/飞行距离/伤害/死亡数据，按 `maxDisplayNum` 截取前 N 名同步到游戏内计分板
4. **配置管理**（`Config.java`/`ScoreboardItem.java`）
    - 基于 Jackson 实现 JSON 配置的加载/保存
    - 支持默认配置初始化、旧版配置兼容（新增字段自动填充默认值）
5. **事件监听**（`PlayerBreakBlockEvent.java`/`PlayerPlaceBlockEvent.java`）
    - 监听方块破坏/放置事件，实时更新挖掘量/放置量计分板数据

### 配置说明
配置文件路径：`config/advanced_scoreboard.json`

#### 核心配置项
```json
{
  "border": "===",              // 榜单显示名边框（可自定义），实际显示为 border+displayName+border
  "switchInterval": 5,          // 计分板轮播间隔（秒），最小1秒
  "saveInterval": 5,            // 数据保存间隔（秒），最小1秒
  "maxDisplayNum": 15,          // 榜单最大显示玩家数量，最小1
  "hiddenScoreboards": [],      // 全局隐藏的榜单 internalName 集合
  "scoreboards": [              // 计分板列表
    {
      "internalName": "mine_count",         // 内部标识（唯一）
      "displayName": "挖掘量",              // 显示名称（不含边框）
      "data": {}                            // 玩家-分数映射（自动维护）
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
    }
  ]
}
```

#### 自定义计分板
1. 配置 `displayName`修改计分板显示名称
2. 重启服务器，模组会自动注册新的计分板
3. 可通过修改 `data` 字段手动设置玩家分数（需等待同步周期生效）

## 开发相关

### 项目结构
```
src/
├── main/
│   ├── java/cn/hycer/advancedscoreboard/
│   │   ├── AdvancedScoreboard.java        // 模组主类
│   │   ├── Command/                       // 指令系统
│   │   │   ├── ASBCommand.java            // /asb 指令注册与共享 SuggestionProvider
│   │   │   ├── SetCommand.java            // /asb set 子指令
│   │   │   ├── ScoreboardCommand.java     // /asb scoreboard 子指令
│   │   │   └── NotDisplayCommand.java     // /asb notDisplay 子指令
│   │   ├── Config/                        // 配置相关
│   │   │   ├── Config.java                // 根配置类
│   │   │   └── ScoreboardItem.java        // 单个计分板配置
│   │   ├── Event/                         // 事件监听
│   │   │   ├── PlayerBreakBlockEvent.java  // 玩家破坏方块事件
│   │   │   ├── PlayerPlaceBlockEvent.java  // 玩家放置方块事件
│   │   │   └── ServerStartedEvent.java    // 服务器启动事件
│   │   ├── Global/                        // 全局变量
│   │   │   └── Global.java                // 全局日志、计分板、配置
│   │   └── Task/                          // 定时任务
│   │       └── Task.java                  // 轮播、同步任务
│   └── resources/
│       ├── fabric.mod.json                // Fabric 模组元数据
│       ├── advancedscoreboard.mixins.json  // 通用 Mixin 配置
│       ├── log4j2.xml                     // 日志配置
└── client/                                // 客户端代码（占位）

```