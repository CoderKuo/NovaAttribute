# 属性配置

## 属性定义格式

在 `plugins/NovaAttribute/attributes/` 目录下的任何 `.yml` 文件中定义属性。

```yaml
attributes:
  属性ID:
    name: "§c显示名称"          # 带颜色代码的显示名（也用于全局 Lore 匹配）
    default: 0.0               # 默认值
    range: false               # 是否为范围值（min~max）
    lore-pattern: "正则表达式"   # 自定义 Lore 匹配正则（可选）
    lore-divisor: 1.0          # Lore 除数（100.0 表示百分比属性）
    trigger: PASSIVE           # 触发类型
    trigger-event: ""          # CUSTOM 类型绑定的事件触发器名
    script: ""                 # 脚本文件名（不含 .nova 后缀）
    priority: 0                # 执行优先级（数字越小越先）
    interval: 20               # PERIODIC 类型的执行间隔（tick）
    combat-power: 0.0          # 战斗力系数
    messages:                  # 触发消息
      attacker: ""             # 攻击者看到的
      victim: ""               # 被攻击者看到的
      self: ""                 # 自身触发时看到的（PERIODIC/KILL）
```

## 触发类型

| 类型 | 说明 | 脚本参数 |
|------|------|----------|
| `PASSIVE` | 纯数据，不触发脚本 | 无 |
| `ATTACK` | 攻击时执行 | `(attacker, victim, ctx, attrValue)` |
| `DEFENSE` | 被攻击时执行 | `(attacker, victim, ctx, attrValue)` |
| `KILL` | 击杀后执行 | `(attacker, victim, ctx, attrValue)` |
| `PERIODIC` | 按 `interval` 定时执行 | `(entity, attrValue)` |
| `CUSTOM` | 手动/事件触发 | `(caster, target, params, attrValue)` |

## Lore 匹配

### 全局匹配

属性未定义 `lore-pattern` 时，自动根据 `name`（去色后）匹配 Lore 行。

支持的格式：

| Lore 格式 | 解析结果 |
|-----------|---------|
| `物理攻击: 100` | 固定值 100 |
| `物理攻击: +100` | 固定值 100 |
| `物理攻击: 100~200` | 范围值 100-200 |
| `暴击几率: 25%` | 百分比 0.25（需 lore-divisor: 100.0） |
| `物理攻击: +100(+10%)` | 基础 100 + 乘算 10% |
| `物理攻击: 100万` | 带单位 1000000 |

### 自定义正则

```yaml
lore-pattern: "溟灭几率:\\s*\\+?(\\d+\\.?\\d*)%"
```

捕获组中的数字自动除以 `lore-divisor`。

## 百分比属性

`lore-divisor` 不为 1.0 的属性自动被视为百分比：
- Lore 写 `25%` → 内部存储 `0.25`
- 面板显示时自动乘回 divisor 并加 `%`

## 范围属性

`range: true` 的属性支持 min~max 格式：
- 内部存储为 `[min, max]`
- `getAttr()` 返回平均值
- `getAttrRandom()` 返回随机值
- `getAttrMin()` / `getAttrMax()` 返回边界值

## 内置属性一览（46个）

### 攻击
| ID | 名称 | 默认值 | 类型 |
|----|------|--------|------|
| `physical_damage` | 物理攻击 | 1.0 | PASSIVE |
| `magic_damage` | 魔法攻击 | 0.0 | PASSIVE |
| `true_damage` | 真实伤害 | 0.0 | PASSIVE |
| `attack_speed` | 攻击速度 | 0.0 | PASSIVE (%) |

### 穿透
| ID | 名称 | 类型 |
|----|------|------|
| `armor_penetration` | 物理穿透 | PASSIVE (%) |
| `magic_penetration` | 魔法穿透 | PASSIVE (%) |
| `ignore_dodge` | 无视闪避 | PASSIVE (%) |

### 防御
| ID | 名称 | 触发 |
|----|------|------|
| `physical_defense` | 物理防御 | PASSIVE |
| `magic_defense` | 魔法抗性 | PASSIVE |
| `damage_reduction` | 伤害减免 | DEFENSE (%) |
| `block_chance` | 格挡几率 | DEFENSE (%) |
| `block_rate` | 格挡减伤 | PASSIVE (%) |
| `toughness` | 韧性 | DEFENSE (%) |
| `knockback_resistance` | 击退抗性 | PASSIVE (%) |

### 生命
| ID | 名称 | 触发 |
|----|------|------|
| `max_health` | 最大生命 | PASSIVE |
| `health_regen` | 生命恢复 | PERIODIC (interval=60) |

### 暴击 / 闪避 / 吸血 / 反伤
| ID | 触发 | 优先级 |
|----|------|--------|
| `critical_chance` | ATTACK | 10 |
| `critical_damage` | PASSIVE | - |
| `dodge` | DEFENSE | 0 |
| `lifesteal` | ATTACK | 90 |
| `thorns` | DEFENSE | 50 |

### 状态触发（均为 ATTACK，priority=20）
`ignite_chance`, `poison_chance`, `wither_chance`, `slow_chance`, `blind_chance`, `lightning_chance`, `knockback`

### PVP/PVE / 击杀
`pvp_damage_bonus`, `pve_damage_bonus`, `kill_heal`, `exp_bonus`

### 元素
`fire_damage/resistance`, `ice_damage/resistance`, `lightning_damage/resistance`, `poison_damage/resistance`

### 移动 / 蓄力 / 弓箭 / 召唤
`movement_speed`, `charge_bonus`, `charge_disrupt`, `arrow_damage`, `arrow_speed`, `summon_power`
