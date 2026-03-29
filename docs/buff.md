# Buff 系统

## 概述

Buff 是带持续时间的临时属性加成。到期自动移除，支持叠加和持久化。

## 命令添加

```
/na buff <player> <id> <duration> [attrId] [value]
```

duration 支持：
- tick 数字：`200`（200 tick = 10 秒）
- 时间格式：`1d2h30m10s`（1天2小时30分10秒）
- `-1` 表示永久

示例：
```
/na buff Steve fire_boost 5m fire_damage 50
/na buff Steve speed_buff 1h movement_speed 0.2
/na unbuff Steve fire_boost
```

## API 添加

```kotlin
val data = AttributeData()
data.set("physical_damage", 100.0)
data.set("critical_chance", 0.15)

val buff = Buff(
    id = "warrior_rage",
    data = data,
    duration = 60000,           // 60 秒（毫秒）
    stackable = false,          // 是否可叠加
    persistent = true,          // 是否持久化（重启后恢复）
    expireAt = System.currentTimeMillis() + 60000
)

NovaAttributeAPI.addBuff(player, buff)
NovaAttributeAPI.removeBuff(player, "warrior_rage")
NovaAttributeAPI.hasBuff(player, "warrior_rage")
NovaAttributeAPI.getBuffRemaining(player, "warrior_rage")
```

## 脚本添加（novaattr 库）

```javascript
// 其他插件的脚本
novaattr.addBuff(player, "boost", 60000, "physical_damage", 100.0)
novaattr.addBuff(player, "boost", 60000, "physical_damage", 100.0, true, true)
//                                                                 stackable persistent

novaattr.removeBuff(player, "boost")
novaattr.hasBuff(player, "boost")        // → true/false
novaattr.getBuffRemaining(player, "boost") // → 毫秒
```

## 叠加规则

- `stackable = false`：同 ID 的 Buff 覆盖（刷新时间）
- `stackable = true`：同 ID 的 Buff 叠加层数（stacks +1），触发 `BuffStackEvent`

## 持久化

设置 `persistent = true` 的 Buff 在玩家退出时保存到数据库，重新上线后恢复。需要 `config.yml` 中启用数据库。

## 事件

| 事件 | 触发时机 |
|------|---------|
| `BuffAddEvent` | Buff 添加时（可取消） |
| `BuffRemoveEvent` | Buff 移除时（含原因：API/过期/死亡） |
| `BuffStackEvent` | Buff 叠加时 |
