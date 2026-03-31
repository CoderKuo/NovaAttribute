# 配置参考

所有配置位于 `plugins/NovaAttribute/config.yml`。

## 基础

```yaml
debug: false                # 调试模式（输出详细日志）
refresh-cooldown: 2         # 属性刷新防抖间隔（tick）
```

## 战斗系统

```yaml
combat:
  enabled: true                       # 启用自定义伤害计算
  cancel-vanilla-cooldown: false      # 取消原版攻击冷却
  message:
    enabled: true
    attacker: "§7你对 §f{victim} §7造成了 §c{damage} §7点伤害"
    victim: "§7你受到了 §f{attacker} §7的 §c{damage} §7点伤害"
    critical: "§6✦暴击！§7你对 §f{victim} §7造成了 §c{damage} §7点伤害"
    miss-attacker: "§7{victim} §a闪避了§7你的攻击"
    miss-victim: "§a你闪避了 §f{attacker} §a的攻击"
```

## Buff 系统

```yaml
buff:
  check-interval: 20         # 过期检查间隔（tick）
```

## 大数值格式化

```yaml
number-format:
  enabled: false             # 是否启用缩写显示
  decimal-places: 1          # 小数位数
  units:                     # 单位定义（同时用于 Lore 解析和显示）
    万: 10000
    亿: 100000000
    万亿: 1000000000000
```

## Lore 解析

```yaml
lore-format:
  enabled: true
  separator: "[：:] ?"       # 属性名与数值的分隔符（正则）
```

## 条件系统

```yaml
condition:
  enabled: true              # 装备条件（NBT + Lore）
  lore-patterns:             # Lore 条件关键词（正则，|分隔别名）
    level: "需要等级|等级限制|Lv\\.|Level"
    class: "限制职业|需要职业|职业限制"
    permission: "需要权限"

inline-condition:
  enabled: true              # 内嵌条件
  lore-separator: " / "      # Lore 中条件分隔符
```

Lore 条件匹配格式：`关键词: 值`（如 `需要等级: 50`）。详见 [条件系统](conditions.md)。

## 伤害指示器

```yaml
damage-indicator:
  enabled: true
  offset-y: 1.5              # 高度偏移（方块）
  duration: 30               # 显示时长（tick）
  view-range: 64             # 可见范围（方块）
  format:
    physical: "§c-{damage}"
    magic: "§b✦ -{damage}"
    true: "§4♦ -{damage}"
    critical: "§6§l★ -{damage}"
    heal: "§a+{damage}"
    miss: "§7Miss"
```

## 原版属性同步

```yaml
vanilla-sync:
  enabled: true              # 将 max_health/movement_speed 等同步到 Bukkit 原版
```

同步的属性：

| NovaAttribute | Bukkit | 同步方式 |
|---|---|---|
| max_health | GENERIC_MAX_HEALTH | value - 20 |
| movement_speed | GENERIC_MOVEMENT_SPEED | 0.1 × value |
| attack_speed | GENERIC_ATTACK_SPEED | 4.0 × value |
| knockback_resistance | GENERIC_KNOCKBACK_RESISTANCE | 直接设值 |

## 血量条缩放

```yaml
health-scale:
  enabled: false             # 高血量时固定血条显示
  value: 20.0                # 缩放值（20.0 = 10 颗心）
```

## 数据库

```yaml
database:
  type: sqlite               # sqlite / mysql / postgresql
  # mysql:
  #   host: localhost
  #   port: 3306
  #   database: nova_attribute
  #   user: root
  #   password: ''
```

持久化标记为 `persistent` 的 Buff、冷却和计数器。
