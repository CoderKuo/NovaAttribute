# 脚本 API

所有属性脚本（`.nova` 文件）中可使用以下函数。脚本语言为 [NovaLang](https://github.com/CoderKuo/NovaScript)。

## 属性查询

| 函数 | 返回 | 说明 |
|------|------|------|
| `getAttr(entity, attrId)` | Double | 获取属性值（范围属性返回平均值） |
| `getAttrRandom(entity, attrId)` | Double | 获取范围属性的随机值 |
| `getAttrMin(entity, attrId)` | Double | 获取范围属性最小值 |
| `getAttrMax(entity, attrId)` | Double | 获取范围属性最大值 |

## 类型转换

| 函数 | 返回 | 说明 |
|------|------|------|
| `toNumber(value)` | Double | 转数字（String/Number → Double） |
| `toFloat(value)` | Float | 转 float（调用 Java 方法需要 float 参数时使用） |
| `toInt(value)` | Int | 转整数 |

## 实体工具

| 函数 | 返回 | 说明 |
|------|------|------|
| `getNearbyEntities(entity, radius)` | List | 获取附近生物 |
| `safeHeal(entity, amount)` | - | 安全治疗（不超过最大血量） |
| `isType(entity, type)` | Boolean | 检查实体类型（如 "ZOMBIE"） |
| `isAlive(entity)` | Boolean | 是否存活 |
| `isPlayer(entity)` | Boolean | 是否为玩家 |

## 概率 / 数学

| 函数 | 返回 | 说明 |
|------|------|------|
| `chance(probability)` | Boolean | 概率判定（0.0~1.0） |
| `random(min, max)` | Double | 随机浮点数 |
| `randomInt(min, max)` | Int | 随机整数 |
| `max(a, b)` | Double | 取最大值 |
| `min(a, b)` | Double | 取最小值 |
| `clamp(value, min, max)` | Double | 限制范围 |
| `weightRandom(item1, w1, item2, w2, ...)` | Any | 权重随机选择 |

## 冷却系统

| 函数 | 返回 | 说明 |
|------|------|------|
| `cooldownReady(entity, key)` | Boolean | 冷却是否就绪 |
| `setCooldown(entity, key, ticks)` | - | 设置冷却 |

## 计数器

| 函数 | 返回 | 说明 |
|------|------|------|
| `increment(entity, key, amount)` | Int | 增加计数器 |
| `getCounter(entity, key)` | Int | 获取计数器值 |
| `resetCounter(entity, key)` | - | 重置计数器 |

## 伤害控制

| 函数 | 返回 | 说明 |
|------|------|------|
| `attackTo(attacker, victim, damage)` | - | 造成伤害（bypass 模式，不触发属性脚本） |
| `getAttackCooldown(player)` | Double | 获取攻击冷却值（0.0~1.0） |

## 效果链（属性脚本间通信）

| 函数 | 返回 | 说明 |
|------|------|------|
| `markTriggered(ctx, name)` | - | 标记某效果已触发 |
| `isTriggered(ctx, name)` | Boolean | 检查某效果是否已触发 |

使用示例：

```javascript
// dodge.nova (priority=0，先执行)
fun execute(attacker, victim, ctx, attrValue) {
    if (chance(attrValue)) {
        markTriggered(ctx, "dodge")
        ctx.setDamage(0)
    }
}

// critical.nova (priority=10，后执行)
fun execute(attacker, victim, ctx, attrValue) {
    if (isTriggered(ctx, "dodge")) { return null }  // 闪避了不暴击
    if (chance(attrValue)) {
        markTriggered(ctx, "critical")
        var critDmg = getAttr(attacker, "critical_damage")
        return ctx.getDamage() * (1.0 + critDmg)
    }
}
```

## 状态效果

| 函数 | 说明 |
|------|------|
| `applyEffect(entity, effectName, ticks, amplifier)` | 添加药水效果 |
| `removeEffect(entity, effectName)` | 移除药水效果 |
| `hasEffect(entity, effectName)` | 检查药水效果 |
| `setFire(entity, ticks)` | 着火 |
| `strikeLightning(entity)` | 雷击 |
| `knockback(target, source, strength)` | 击退 |

## 消息 / 音效

| 函数 | 说明 |
|------|------|
| `sendMessage(player, text)` | 发送消息 |
| `sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)` | 发送标题 |
| `playSound(entity, sound, volume, pitch)` | 播放音效 |
| `placeholder(player, text)` | PAPI 占位符替换 |
| `placeholderDouble(player, text, default)` | PAPI 返回数值 |

## MythicMobs

| 函数 | 返回 | 说明 |
|------|------|------|
| `getMobLevel(entity)` | Double | MM 怪物等级（非 MM 怪返回 0） |
| `getMobId(entity)` | String? | MM 怪物 ID（非 MM 怪返回 null） |
| `isMythicMob(entity)` | Boolean | 是否为 MM 怪物 |

## 物品构建

| 函数 | 返回 | 说明 |
|------|------|------|
| `buildItem(material, amount)` | ItemStack | 构建物品 |
| `setItemName(item, name)` | ItemStack | 设置显示名 |
| `setItemLore(item, loreList)` | ItemStack | 设置 Lore |
| `matchItem(item, expression)` | Boolean | 物品匹配表达式 |

## CUSTOM 触发器

| 函数 | 说明 |
|------|------|
| `triggerCustom(entity, attrId)` | 触发 CUSTOM 属性 |
| `triggerCustomEx(entity, attrId, target, params)` | 带参数触发 |

## 格式化工具

| 函数 | 说明 |
|------|------|
| `formatShort(value)` | 大数缩写（100万） |
| `formatGrouped(value)` | 千分位（1,000,000） |
| `formatPercent(value, digits)` | 百分比格式 |
| `toRoman(value)` | 罗马数字 |
| `parseDuration(str)` | 解析时间（"1d2h30m"）→ 毫秒 |
| `formatDuration(millis)` | 毫秒 → 中文时间 |

## DamageContext 方法

在 ATTACK/DEFENSE/KILL 脚本中，`ctx` 参数是 `DamageContext` 对象：

| 方法 | 说明 |
|------|------|
| `ctx.getDamage()` | 获取当前伤害 |
| `ctx.setDamage(value)` | 设置伤害 |
| `ctx.isProjectileHit()` | 是否为远程攻击 |
| `ctx.getProperty(key)` | 获取自定义属性 |
| `ctx.setProperty(key, value)` | 设置自定义属性 |

## 全局函数库（供其他插件脚本使用）

NovaAttribute 导出 `novaattr` 全局函数库，其他使用 NovaScript 的插件可以调用：

```javascript
var hp = novaattr.getAttr(player, "max_health")
var hp2 = novaattr.getAttr("Steve", "max_health")  // 支持玩家名
novaattr.addBuff(player, "boost", 6000, "physical_damage", 100.0)
novaattr.triggerCustom(player, "my_custom_attr")
```
