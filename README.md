<p align="center">
  <img src="nova-banner.svg" alt="NovaAttribute" width="480"/>
</p>

<h1 align="center">NovaAttribute</h1>

<p align="center">
  基于 TabooLib 6.2 的下一代 Bukkit 自定义属性插件<br/>
  全脚本驱动 · 来源无关 · 高度可扩展
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.12--1.20+-green" alt="MC Version"/>
  <img src="https://img.shields.io/badge/Java-8+-orange" alt="Java"/>
  <img src="https://img.shields.io/badge/Script-NovaLang-7c3aed" alt="Script Engine"/>
  <br/>
  <a href="https://nova.geekyunfu.com">NovaLang 官网</a>
</p>

---

## 特性

- **来源无关** — 属性不关心从哪来，任何插件一行代码注入属性
- **全脚本驱动** — 伤害公式、属性特效、汇总方式全由 [NovaLang](https://github.com/CoderKuo/NovaScript) 脚本定义
- **46+ 内置属性** — 攻防、暴击、闪避、元素、蓄力、弓箭、召唤等开箱即用
- **6 种触发类型** — PASSIVE / ATTACK / DEFENSE / KILL / PERIODIC / CUSTOM
- **原版属性同步** — max_health、movement_speed 等自动同步到 Bukkit 原版
- **MythicMobs 集成** — 怪物属性注入 + 6 种自定义技能 + 条件 + 掉落增强
- **SX-Attribute 兼容** — SX2 附属插件无需修改代码即可运行
- **完整数据持久化** — Buff / 冷却 / 计数器支持 SQLite / MySQL / PostgreSQL
- **性能分析** — 内置 Mirror 性能监控，`/na mirror` 一键查看

## 快速开始

1. 安装 [NovaScript](https://github.com/CoderKuo/NovaScript)
2. 将 NovaAttribute.jar 放入 `plugins/` 目录
3. 启动服务器，编辑 `plugins/NovaAttribute/config.yml`
4. 在 `plugins/NovaAttribute/attributes/` 下定义属性
5. 在 `plugins/NovaAttribute/scripts/` 下编写脚本

## 文档

| 文档 | 说明 |
|------|------|
| [1. 快速入门](docs/01-getting-started.md) | 安装、配置、第一个自定义属性 |
| [2. 属性配置](docs/02-attributes.md) | 属性定义 YAML 格式详解 |
| [3. 脚本 API](docs/03-script-api.md) | NovaLang 脚本可用函数一览 |
| [4. 伤害计算](docs/04-damage.md) | 伤害公式和战斗流程 |
| [5. Buff 系统](docs/05-buff.md) | Buff 添加、叠加、持久化 |
| [6. 触发器系统](docs/06-triggers.md) | PERIODIC / CUSTOM 触发器和事件绑定 |
| [7. 条件系统](docs/07-conditions.md) | 装备条件（NBT + Lore）和内嵌条件 |
| [8. 属性公式](docs/08-formulas.md) | 基于 PAPI 占位符的动态属性计算 |
| [9. MythicMobs 兼容](docs/09-mythicmobs.md) | 怪物属性、自定义技能、掉落增强 |
| [10. 开发者 API](docs/10-developer-api.md) | Java/Kotlin API 和 Provider 接口 |
| [11. 命令参考](docs/11-commands.md) | 所有命令及用法 |
| [12. 配置参考](docs/12-config.md) | config.yml 完整配置项说明 |
| [14. 占位符](docs/14-placeholders.md) | PlaceholderAPI 占位符一览 |

## 命令

| 命令 | 说明 |
|------|------|
| `/na reload` | 重载配置和脚本 |
| `/na lookup [player]` | 查看玩家属性 |
| `/na power [player]` | 查看战斗力 |
| `/na buff <player> <id> <duration> [attr] [value]` | 添加 Buff |
| `/na unbuff <player> <id>` | 移除 Buff |
| `/na panel [player]` | 打开属性面板 |
| `/na debug [player]` | 调试信息 |
| `/na mirror` | 性能分析报告 |
| `/na refresh [player]` | 刷新属性 |

## 项目结构

```
NovaAttribute/
├── api/                    # 公共 API + 事件
├── core/                   # 核心数据层
│   ├── attribute/          # 属性模型
│   ├── buff/               # Buff 系统
│   ├── cooldown/           # 冷却管理
│   ├── counter/            # 计数器
│   ├── reader/             # 物品属性解析
│   └── condition/          # 条件系统
├── combat/                 # 战斗系统
├── realizer/               # 原版属性同步
├── trigger/                # 触发器系统
├── script/                 # 脚本集成
├── feature/                # 功能模块
├── compat/                 # 第三方兼容
│   ├── mythic/             # MythicMobs
│   └── sx2/                # SX-Attribute
├── command/                # 命令
├── listener/               # 事件监听
└── util/                   # 工具
```

## 依赖

- [TabooLib 6.2](https://taboolib.org/) — 开发框架
- [NovaScript](https://github.com/CoderKuo/NovaScript) — 脚本引擎
- [Rulib](https://github.com/CoderKuo/Rulib) — 通用工具库
- [universal-mythic](https://github.com/TabooLib/universal-mythic) — MythicMobs 桥接（可选）

## 许可

MIT License
