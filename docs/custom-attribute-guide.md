# NovaAttribute 自定义属性教程

本文档面向服主和服务器开发者，介绍如何为 NovaAttribute 创建自定义属性。

---

## 快速上手

创建一个自定义属性只需两步：

1. 在 `plugins/NovaAttribute/attributes/` 目录下的 `.yml` 文件中**定义属性**
2. 如果属性需要触发效果，在 `plugins/NovaAttribute/scripts/` 目录下创建对应的 `.nova` 脚本

完成后执行 `/novas reload` 即可热加载。

---

## 第一步：定义属性

在 `attributes/` 目录下的任意 `.yml` 文件中添加定义。可以写在 `default.yml` 里，也可以新建文件（如 `custom.yml`）分类管理。

### 基本格式

```yaml
attributes:
  属性ID:
    name: "§颜色属性显示名"
    default: 0.0
    trigger: PASSIVE
```

### 全部配置项

| 配置项 | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| `name` | String | 必填 | 属性显示名，支持 `§` 颜色代码。去色后自动用于匹配物品 Lore |
| `default` | Double | `0.0` | 属性默认值（所有实体没有任何装备时的基础值） |
| `trigger` | String | `PASSIVE` | 触发类型，见下方说明 |
| `script` | String | 无 | 对应的脚本文件名（不含 `.nova` 后缀） |
| `priority` | Int | `0` | 执行优先级，数字越小越先执行 |
| `combat-power` | Double | `0.0` | 战斗力系数（用于战斗力评分计算） |
| `lore-divisor` | Double | `1.0` | Lore 百分比除数。设为 `100.0` 时，Lore 写 `25%` 内部存储为 `0.25` |
| `lore-pattern` | String | 自动 | 自定义 Lore 匹配正则。通常不需要，插件会根据 `name` 自动生成 |
| `range` | Boolean | `false` | 是否支持范围值（`100~200` 格式） |
| `interval` | Long | `20` | 仅 `PERIODIC` 类型使用，执行间隔（单位：tick，20 tick = 1秒） |
| `messages` | Map | 无 | 触发后自动发送的消息，支持 `attacker`、`victim`、`self` 三个键 |

### 触发类型

| 类型 | 说明 | 是否需要脚本 |
|------|------|------------|
| `PASSIVE` | 被动属性，仅提供数值供其他属性/公式读取 | 不需要 |
| `ATTACK` | 攻击时触发（从攻击者角度） | 需要 |
| `DEFENSE` | 被攻击时触发（从防御者角度） | 需要 |
| `KILL` | 击杀时触发 | 需要 |
| `PERIODIC` | 每隔 `interval` tick 定时触发 | 需要 |
| `CUSTOM` | 需要通过 API 手动调用 | 需要 |

**如何选择触发类型？**

- 物理攻击、防御值、最大生命这类"只提供数值"的属性 → `PASSIVE`
- 暴击、吸血这类"攻击时判定"的属性 → `ATTACK`
- 闪避、反伤这类"被打时判定"的属性 → `DEFENSE`
- 击杀回血、经验加成这类"杀死目标时触发"的属性 → `KILL`
- 生命恢复、持续回蓝这类"定时生效"的属性 → `PERIODIC`

---

## 第二步：编写脚本

在 `plugins/NovaAttribute/scripts/` 目录下创建 `.nova` 文件，文件名需与属性定义中的 `script` 字段一致。

### 脚本函数签名

不同触发类型的脚本函数签名不同：

#### ATTACK / DEFENSE / KILL

```nova
fun execute(attacker, victim, ctx, attrValue) {
    // attacker  — 攻击者（LivingEntity）
    // victim    — 被攻击者（LivingEntity）
    // ctx       — 伤害上下文，可读写伤害值
    // attrValue — 当前触发实体的该属性数值
}
```

- `ATTACK` / `KILL` 时 `attrValue` 取的是**攻击者**的属性值
- `DEFENSE` 时 `attrValue` 取的是**被攻击者**的属性值
- 当 `attrValue == 0` 时脚本不会被执行（自动跳过）

#### PERIODIC

```nova
fun execute(entity, attrValue) {
    // entity    — 拥有该属性的实体
    // attrValue — 该实体的属性数值
}
```

### 伤害上下文 (ctx)

`ctx` 对象提供以下方法：

| 方法 | 说明 |
|------|------|
| `ctx.getDamage()` | 获取当前伤害值 |
| `ctx.setDamage(value)` | 设置伤害值 |
| `ctx.getProperty(key)` | 获取自定义属性 |
| `ctx.setProperty(key, value)` | 设置自定义属性（如标记暴击、伤害类型等） |

---

## 可用 API 函数

脚本中可以直接调用以下函数，无需 import：

### 属性查询

| 函数 | 说明 |
|------|------|
| `getAttr(entity, "属性ID")` | 获取属性确定值 |
| `getAttrRandom(entity, "属性ID")` | 获取范围属性的随机值（非范围属性等同 getAttr） |
| `getAttrMin(entity, "属性ID")` | 获取范围属性最小值 |
| `getAttrMax(entity, "属性ID")` | 获取范围属性最大值 |

### 概率与数学

| 函数 | 说明 |
|------|------|
| `chance(probability)` | 概率判定，传入 0.0~1.0，返回 true/false |
| `random(min, max)` | 返回 min~max 之间的随机小数 |
| `randomInt(min, max)` | 返回 min~max 之间的随机整数 |
| `max(a, b)` | 取较大值 |
| `min(a, b)` | 取较小值 |
| `clamp(value, min, max)` | 限制 value 在 min~max 范围内 |
| `weightRandom("A", 10, "B", 30, "C", 60)` | 按权重随机选择，返回选中项 |

### 实体操作

| 函数 | 说明 |
|------|------|
| `safeHeal(entity, amount)` | 安全回血（不会超过最大生命值） |
| `isAlive(entity)` | 判断实体是否存活 |
| `isPlayer(entity)` | 判断是否为玩家 |
| `isType(entity, "ZOMBIE")` | 判断实体类型（大写英文名） |
| `getNearbyEntities(entity, radius)` | 获取半径内的所有生物列表 |
| `toNumber(value)` | 将值转为数字 |

### 伤害控制

| 函数 | 说明 |
|------|------|
| `attackTo(attacker, victim, damage)` | 对目标造成额外伤害（不会触发属性递归） |

### 状态效果

| 函数 | 说明 |
|------|------|
| `applyEffect(entity, "效果名", 持续tick, 等级)` | 给实体施加药水效果。效果名为大写英文（如 `POISON`、`SLOW`、`WITHER`、`BLINDNESS`） |
| `removeEffect(entity, "效果名")` | 移除指定药水效果 |
| `hasEffect(entity, "效果名")` | 判断实体是否有指定药水效果 |
| `setFire(entity, ticks)` | 点燃实体（单位：tick） |
| `strikeLightning(entity)` | 在实体位置劈下闪电（仅视觉效果） |
| `knockback(target, source, strength)` | 将 target 从 source 方向击退，strength 为力度 |

### 经验

| 函数 | 说明 |
|------|------|
| `giveExp(player, amount)` | 给予玩家经验值 |

### 冷却系统

| 函数 | 说明 |
|------|------|
| `cooldownReady(entity, "冷却键名")` | 冷却是否已就绪 |
| `setCooldown(entity, "冷却键名", ticks)` | 设置冷却时间（单位：tick） |

### 计数器

| 函数 | 说明 |
|------|------|
| `increment(entity, "计数器名", amount)` | 递增计数器，返回新值 |
| `getCounter(entity, "计数器名")` | 获取计数器当前值 |
| `resetCounter(entity, "计数器名")` | 重置计数器为 0 |

### 消息与音效

| 函数 | 说明 |
|------|------|
| `sendMessage(entity, "消息内容")` | 向玩家发送聊天消息 |
| `sendTitle(player, "主标题", "副标题", fadeIn, stay, fadeOut)` | 发送 Title（时间单位 tick） |
| `playSound(entity, "音效名", volume, pitch)` | 播放音效（音效名为 Bukkit Sound 枚举名） |

### PlaceholderAPI

| 函数 | 说明 |
|------|------|
| `placeholder(player, "%placeholder%")` | 解析 PAPI 占位符，返回字符串 |
| `placeholderDouble(player, "%placeholder%", 默认值)` | 解析 PAPI 占位符为数字 |

### 延迟任务

| 函数 | 说明 |
|------|------|
| `delay(ticks, callback)` | 延迟 N tick 后执行回调 |

### 物品匹配

| 函数 | 说明 |
|------|------|
| `matchItem(item, "匹配表达式")` | 判断物品是否匹配表达式 |

### 格式化工具

| 函数 | 说明 |
|------|------|
| `formatShort(value)` | 短格式数字（如 `1.5K`、`2.3M`） |
| `formatGrouped(value)` | 千分位分隔（如 `1,234,567`） |
| `formatPercent(value, digits)` | 百分比格式（如 `25.0%`） |
| `toRoman(value)` | 转罗马数字（如 `IV`） |
| `parseDuration("1h30m")` | 解析时间字符串为毫秒 |
| `formatDuration(millis)` | 毫秒转可读时间（如 `1小时30分`） |

---

## 完整示例

### 示例 1：被动属性（无需脚本）

最简单的属性，只提供数值。

**attributes/custom.yml:**
```yaml
attributes:
  fire_damage:
    name: "§c火焰伤害"
    default: 0.0
    trigger: PASSIVE
    combat-power: 1.0
```

物品 Lore 写上 `火焰伤害: 50`，玩家装备后就会拥有 50 点火焰伤害。其他属性脚本可以通过 `getAttr(entity, "fire_damage")` 读取这个值。

### 示例 2：百分比属性

**attributes/custom.yml:**
```yaml
attributes:
  critical_chance:
    name: "§6暴击几率"
    default: 0.0
    lore-divisor: 100.0
    trigger: ATTACK
    script: critical
    combat-power: 2.0
```

`lore-divisor: 100.0` 表示 Lore 中写的是百分比形式。物品 Lore 写 `暴击几率: 25%`，插件会自动除以 100 存储为 `0.25`。

### 示例 3：攻击触发 — 暴击

**attributes/default.yml:**
```yaml
attributes:
  critical_chance:
    name: "§6暴击几率"
    default: 0.0
    lore-divisor: 100.0
    trigger: ATTACK
    script: critical
    priority: 10
    combat-power: 2.0
```

**scripts/critical.nova:**
```nova
fun execute(attacker, victim, ctx, attrValue) {
    if (chance(attrValue)) {
        var critDmg = getAttrRandom(attacker, "critical_damage")
        var before = ctx.getDamage()
        var newDmg = before * (1.0 + critDmg)
        ctx.setDamage(newDmg)
        ctx.setProperty("isCritical", true)
    }
}
```

### 示例 4：防御触发 — 反伤

**attributes/default.yml:**
```yaml
attributes:
  thorns:
    name: "§d反伤"
    default: 0.0
    lore-divisor: 100.0
    trigger: DEFENSE
    script: thorns
    priority: 50
    combat-power: 2.0
    messages:
      attacker: "§c对方的反伤让你受到了伤害！"
      victim: "§a你的反伤反弹了部分伤害！"
```

**scripts/thorns.nova:**
```nova
fun execute(attacker, victim, ctx, attrValue) {
    if (attrValue > 0 && ctx.getDamage() > 0) {
        var reflectDmg = ctx.getDamage() * attrValue
        attackTo(victim, attacker, reflectDmg)
    }
}
```

物品 Lore 写 `反伤: 15%`，被打时会将受到伤害的 15% 反弹给攻击者。

### 示例 5：防御触发 — 闪避

**scripts/dodge.nova:**
```nova
fun execute(attacker, victim, ctx, attrValue) {
    if (chance(attrValue)) {
        ctx.setDamage(0.0)
        ctx.setProperty("damageType", "miss")
    }
}
```

### 示例 6：攻击触发 — 吸血

**scripts/lifesteal.nova:**
```nova
fun execute(attacker, victim, ctx, attrValue) {
    if (attrValue > 0 && ctx.getDamage() > 0) {
        var healAmount = ctx.getDamage() * attrValue
        safeHeal(attacker, healAmount)
    }
}
```

### 示例 7：定时触发 — 生命恢复

**attributes/default.yml:**
```yaml
attributes:
  health_regen:
    name: "§a生命恢复"
    default: 0.0
    trigger: PERIODIC
    script: health_regen
    interval: 60
```

**scripts/health_regen.nova:**
```nova
fun execute(entity, attrValue) {
    if (attrValue > 0 && isAlive(entity)) {
        safeHeal(entity, attrValue)
    }
}
```

`interval: 60` 表示每 60 tick（3秒）执行一次。

### 示例 8：带冷却的技能属性

```yaml
attributes:
  flame_strike:
    name: "§c烈焰打击"
    default: 0.0
    lore-divisor: 100.0
    trigger: ATTACK
    script: flame_strike
    priority: 20
    combat-power: 1.5
```

**scripts/flame_strike.nova:**
```nova
fun execute(attacker, victim, ctx, attrValue) {
    if (chance(attrValue) && cooldownReady(attacker, "flame_strike")) {
        setCooldown(attacker, "flame_strike", 100)
        var bonusDmg = ctx.getDamage() * 0.5
        ctx.setDamage(ctx.getDamage() + bonusDmg)
        playSound(attacker, "ENTITY_BLAZE_SHOOT", 1.0, 1.0)
        sendMessage(attacker, "§c烈焰打击！造成 50% 额外伤害")
    }
}
```

5秒冷却（100 tick），触发时额外增加 50% 伤害并播放音效。

### 示例 9：带计数器的连击属性

```yaml
attributes:
  combo:
    name: "§e连击"
    default: 0.0
    lore-divisor: 100.0
    trigger: ATTACK
    script: combo
    priority: 15
    combat-power: 1.8
```

**scripts/combo.nova:**
```nova
fun execute(attacker, victim, ctx, attrValue) {
    var count = increment(attacker, "combo_count", 1)
    if (count >= 5) {
        resetCounter(attacker, "combo_count")
        if (chance(attrValue)) {
            var bonusDmg = ctx.getDamage() * 0.8
            ctx.setDamage(ctx.getDamage() + bonusDmg)
            sendTitle(attacker, "§6连击!", "§e第5次攻击造成额外伤害", 5, 20, 5)
        }
    }
}
```

每攻击 5 次判定一次连击，触发时增加 80% 额外伤害。

### 示例 10：AOE 溅射属性

```yaml
attributes:
  splash:
    name: "§b溅射"
    default: 0.0
    lore-divisor: 100.0
    trigger: ATTACK
    script: splash
    priority: 90
    combat-power: 2.5
```

**scripts/splash.nova:**
```nova
fun execute(attacker, victim, ctx, attrValue) {
    if (attrValue > 0 && ctx.getDamage() > 0) {
        var splashDmg = ctx.getDamage() * attrValue
        var nearby = getNearbyEntities(victim, 3.0)
        for (entity in nearby) {
            if (entity != attacker && entity != victim) {
                attackTo(attacker, entity, splashDmg)
            }
        }
    }
}
```

---

## Lore 格式说明

插件会根据属性 `name` 自动匹配物品 Lore。以下格式均可自动识别：

| Lore 写法 | 解析结果 | 说明 |
|-----------|---------|------|
| `物理攻击: 100` | 100.0 | 固定值 |
| `物理攻击: +100` | 100.0 | 带正号 |
| `物理攻击: (+100)` | 100.0 | 带括号 |
| `暴击几率: 25%` | 0.25 | 百分比（需 `lore-divisor: 100.0`） |
| `物理攻击: 100~200` | min=100, max=200 | 范围值 |
| `物理攻击: +100(+10%)` | base=100, percent=0.1 | 基础值 + 百分比加成 |
| `物理攻击: 1万` | 10000.0 | 带单位后缀（需在 config.yml 配置 number-format） |

---

## 优先级（priority）说明

属性脚本按 `priority` 从小到大依次执行。合理安排优先级可以控制属性之间的交互顺序。

**ATTACK 脚本执行顺序：**
```
priority: 10   暴击
priority: 20   点燃 / 中毒 / 凋零 / 减速 / 失明 / 雷击
priority: 30   击退
priority: 90   吸血（最后执行，基于最终伤害）
```

**DEFENSE 脚本执行顺序：**
```
priority: 0    闪避（最先判定，闪避成功直接置零）
priority: 5    格挡（部分减伤）
priority: 15   韧性（减少暴击额外伤害）
priority: 40   伤害减免（百分比减伤）
priority: 50   反伤（基于最终受到的伤害反弹）
```

---

## 内置属性一览

插件默认提供 41 个属性，覆盖完整的 RPG 战斗体系：

### 攻击属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `physical_damage` | 物理攻击 | PASSIVE | 基础物理伤害，默认 1.0 |
| `magic_damage` | 魔法攻击 | PASSIVE | 基础魔法伤害 |
| `true_damage` | 真实伤害 | PASSIVE | 无视防御的固定伤害 |
| `attack_speed` | 攻击速度 | PASSIVE | 百分比属性 |

### 穿透属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `armor_penetration` | 物理穿透 | PASSIVE | 百分比降低目标物理防御 |
| `magic_penetration` | 魔法穿透 | PASSIVE | 百分比降低目标魔法抗性 |
| `ignore_dodge` | 无视闪避 | PASSIVE | 百分比降低目标闪避几率 |

### 防御属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `physical_defense` | 物理防御 | PASSIVE | 减免物理伤害 |
| `magic_defense` | 魔法抗性 | PASSIVE | 减免魔法伤害 |
| `damage_reduction` | 伤害减免 | DEFENSE | 百分比减免所有伤害（上限 90%） |
| `block_chance` | 格挡几率 | DEFENSE | 触发后按格挡减伤比例减伤 |
| `block_rate` | 格挡减伤 | PASSIVE | 格挡时的伤害减免比例，默认 50% |
| `toughness` | 韧性 | DEFENSE | 百分比减少暴击额外伤害 |
| `knockback_resistance` | 击退抗性 | PASSIVE | 百分比降低受到的击退效果 |

### 生命属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `max_health` | 最大生命 | PASSIVE | 默认 20.0 |
| `health_regen` | 生命恢复 | PERIODIC | 每 3 秒回复固定生命值 |

### 暴击属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `critical_chance` | 暴击几率 | ATTACK | 触发暴击的概率 |
| `critical_damage` | 暴击伤害 | PASSIVE | 暴击额外伤害倍率，默认 50% |

### 闪避 / 吸血 / 反伤
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `dodge` | 闪避几率 | DEFENSE | 完全回避攻击（受无视闪避影响） |
| `lifesteal` | 吸血 | ATTACK | 按伤害百分比回血 |
| `thorns` | 反伤 | DEFENSE | 将受到伤害的一定比例反弹给攻击者 |

### 状态触发属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `ignite_chance` | 点燃几率 | ATTACK | 概率点燃目标 3 秒 |
| `poison_chance` | 中毒几率 | ATTACK | 概率给目标施加中毒 3 秒 |
| `wither_chance` | 凋零几率 | ATTACK | 概率给目标施加凋零 3 秒 |
| `slow_chance` | 减速几率 | ATTACK | 概率给目标施加减速 2 秒 |
| `blind_chance` | 失明几率 | ATTACK | 概率给目标施加失明 1.5 秒 |
| `lightning_chance` | 雷击几率 | ATTACK | 概率触发闪电视觉 + 30% 额外伤害 |
| `knockback` | 击退强度 | ATTACK | 增强击退效果（受击退抗性影响） |

### PVP / PVE 属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `pvp_damage_bonus` | PVP伤害加成 | PASSIVE | 对玩家的额外伤害百分比 |
| `pve_damage_bonus` | PVE伤害加成 | PASSIVE | 对怪物的额外伤害百分比 |

### 击杀属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `kill_heal` | 击杀回血 | KILL | 击杀目标时回复固定生命值 |
| `exp_bonus` | 经验加成 | KILL | 击杀目标时获得额外经验值 |

### 元素伤害
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `fire_damage` | 火焰伤害 | PASSIVE | 在伤害公式中与火焰抗性对冲 |
| `fire_resistance` | 火焰抗性 | PASSIVE | 减免火焰伤害 |
| `ice_damage` | 冰霜伤害 | PASSIVE | 在伤害公式中与冰霜抗性对冲 |
| `ice_resistance` | 冰霜抗性 | PASSIVE | 减免冰霜伤害 |
| `lightning_damage` | 雷电伤害 | PASSIVE | 在伤害公式中与雷电抗性对冲 |
| `lightning_resistance` | 雷电抗性 | PASSIVE | 减免雷电伤害 |
| `poison_damage` | 毒素伤害 | PASSIVE | 在伤害公式中与毒素抗性对冲 |
| `poison_resistance` | 毒素抗性 | PASSIVE | 减免毒素伤害 |

### 移动属性
| 属性 ID | 显示名 | 类型 | 说明 |
|---------|-------|------|------|
| `movement_speed` | 移动速度 | PASSIVE | 百分比属性 |

---

## 注意事项

1. **属性 ID 全局唯一**：不同 yml 文件中的属性 ID 不能重复
2. **脚本文件名 = script 字段值**：`script: thorns` 对应 `scripts/thorns.nova`
3. **PASSIVE 属性不需要脚本**：只需 yml 定义即可
4. **attrValue 为 0 时自动跳过**：不会调用脚本，无需在脚本中判断
5. **热重载**：修改 yml 或 nova 文件后执行 `/novas reload` 即可生效
6. **messages 会自动发送**：在 yml 中配置了 `messages` 后，脚本触发时会自动发送给对应玩家，无需在脚本中手动 sendMessage
7. **百分比属性**：设置 `lore-divisor: 100.0` 后，Lore 中的 `25%` 会被存储为 `0.25`，脚本中拿到的 attrValue 就是 `0.25`
8. **元素伤害**：四种元素（火焰/冰霜/雷电/毒素）在 `damage.nova` 伤害公式中计算，攻击方元素伤害 - 防御方元素抗性
9. **穿透属性**：物理穿透和魔法穿透在 `damage.nova` 中降低防御的有效值，上限 100%
10. **属性联动**：部分属性之间存在联动关系——暴击+韧性、闪避+无视闪避、击退+击退抗性、格挡+格挡减伤
