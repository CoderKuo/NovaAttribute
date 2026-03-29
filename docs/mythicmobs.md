# NovaAttribute × MythicMobs 兼容指南

NovaAttribute 通过 [universal-mythic](https://github.com/TabooLib/universal-mythic) 桥接 MythicMobs，自动检测并集成。无需额外配置，安装 MythicMobs 即可生效。

---

## 一、怪物属性注入

在 MythicMobs 怪物配置中添加 `NovaAttributes` 或 `NovaAttributeLines` 节点，怪物生成时自动注入 NovaAttribute 属性。

### 方式一：直接映射（推荐）

```yaml
FireBoss:
  Type: BLAZE
  Health: 200
  NovaAttributes:
    physical_damage: 80
    magic_damage: 120
    physical_defense: 40
    fire_damage: 60
    max_health: 200
    movement_speed: 0.15
```

键为 NovaAttribute 属性 ID，值为数值。百分比属性（如 `movement_speed`）使用小数表示（0.15 = 15%）。

### 等级缩放

添加 `NovaAttributeScale` 字段，怪物等级会自动乘算所有属性值：

```yaml
ScaledBoss:
  Type: ZOMBIE
  Health: 100
  NovaAttributes:
    physical_damage: 50
    physical_defense: 20
    max_health: 100
  NovaAttributeScale: 0.1    # 每级属性 +10%
```

公式：`最终值 = 基础值 × (1 + 等级 × scale)`

例如 level=5, scale=0.1 → 所有属性 ×1.5（+50%）。

### 方式二：Lore 格式

```yaml
UndeadKnight:
  Type: SKELETON
  Health: 150
  NovaAttributeLines:
    - "物理攻击: 60~100"
    - "物理防御: 35"
    - "暴击几率: 15%"
    - "吸血: 10%"
```

格式与物品 Lore 完全一致，支持范围值、百分比、单位后缀等所有 Lore 解析功能。

---

## 二、自定义技能

NovaAttribute 注册了 6 个 MythicMobs 技能，可在怪物的 `Skills` 节点中使用。

### nova-damage — 属性伤害

基于施法者的 NovaAttribute 属性值对目标造成伤害。

```yaml
Skills:
  # 基于物理攻击造成 1.5 倍伤害
  - nova-damage{attr=physical_damage;multiplier=1.5} @target ~onAttack

  # 基于多个属性累加造伤（火焰 + 魔法）
  - nova-damage{attrs=fire_damage,magic_damage;multiplier=1.0} @target ~onTimer:100
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `attr` | 单个属性 ID | 无 |
| `attrs` | 多个属性 ID（逗号分隔，值累加） | 无 |
| `multiplier` | 伤害倍率 | 1.0 |

### nova-cast — 指定属性值伤害

直接在技能中指定属性值造成伤害，不读取施法者身上的属性。支持固定值和随机范围。

```yaml
Skills:
  # 造成 100 点物理 + 50~80 点火焰伤害
  - nova-cast{physical_damage=100;fire_damage=50~80} @target ~onAttack

  # 造成 200 点真实伤害（不可闪避/防御）
  - nova-cast{true_damage=200} @target ~onTimer:200

  # 多属性 + 倍率
  - nova-cast{magic_damage=60~120;ice_damage=40;multiplier=1.5} @PIR{r=5} ~onTimer:100
```

| 参数 | 说明 | 格式 |
|------|------|------|
| `属性ID` | 任意 NovaAttribute 属性 | 固定值 `100` 或范围 `50~80` |
| `multiplier` | 总伤害倍率 | 默认 1.0 |

所有已注册的属性 ID 都可作为参数名（`physical_damage`、`fire_damage`、`true_damage` 等）。范围值每次释放随机取值。

### nova-heal — 治疗

治疗目标，支持基于属性值或固定数值两种方式。

```yaml
Skills:
  # 每 3 秒回复最大生命的 10%
  - nova-heal{attr=max_health;multiplier=0.1} @self ~onTimer:60

  # 固定回复 20 点
  - nova-heal{amount=20} @self ~onDamaged
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `attr` | 从施法者读取的属性 ID | 无 |
| `multiplier` | 属性值倍率 | 1.0 |
| `amount` | 固定治疗量（设置后忽略 attr） | 0 |

### nova-buff — 添加 Buff

给目标添加 NovaAttribute 的临时属性 Buff。

```yaml
Skills:
  # 生成时给自己加火焰伤害 Buff 10 秒
  - nova-buff{id=fire_boost;attr=fire_damage;value=50;duration=200} @self ~onSpawn

  # 攻击时给目标减速 5 秒
  - nova-buff{id=slow_debuff;attr=movement_speed;value=-0.1;duration=100} @target ~onAttack
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `id` | Buff ID（唯一标识） | 必填 |
| `attr` | 属性 ID | 必填 |
| `value` | 属性值（负数为减益） | 0 |
| `duration` | 持续时间（tick，20 tick = 1 秒） | 200 |

### nova-trigger — 触发 CUSTOM 属性

在 MythicMobs 技能中触发 NovaAttribute 的 CUSTOM 类型属性脚本。

```yaml
Skills:
  # 攻击时触发自定义属性脚本（施法者为 caster）
  - nova-trigger{attr=my_custom_attr} @target ~onAttack

  # 无目标触发（@self）
  - nova-trigger{attr=aura_pulse} @self ~onTimer:100
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `attr` | CUSTOM 类型的属性 ID | 必填 |

施法者作为 caster，`@target` 的目标作为 target 传给属性脚本。`@self` 时 target 为 null。

---

## 三、自定义条件

### novaattr — 属性值条件

检查实体的属性值是否满足条件，用于控制技能释放。

```yaml
Conditions:
  # 物理攻击 >= 100 时才释放技能
  - novaattr{attr=physical_damage;value>=100}

  # 暴击几率 > 50% 时
  - novaattr{attr=critical_chance;value>0.5}

  # 血量 <= 50 时（低血量）
  - novaattr{attr=max_health;value<=50}
```

| 参数 | 说明 | 格式 |
|------|------|------|
| `attr` | 属性 ID | 必填 |
| `value` | 条件表达式 | `>=100`、`>50`、`<=200`、`<10`、`=0.5` |

支持的运算符：`>=`、`<=`、`>`、`<`、`=`。纯数字默认为 `>=`。

---

## 四、掉落增强

根据击杀者的自定义属性值（如"幸运"）影响 MythicMobs 怪物的掉落。支持**简单模式**（仅数量）和**脚本模式**（完全自定义）。

### 简单模式 — 数量增强

```yaml
LuckyMob:
  Type: ZOMBIE
  NovaDropBonus:
    attr: luck              # 读取击杀者的 luck 属性
    quantity: 0.01          # 每点 luck 增加 1% 掉落数量
```

公式：`数量倍率 = 1 + 属性值 × quantity`

### 脚本模式 — 完全自定义

```yaml
BossWithLoot:
  Type: ZOMBIE
  NovaDropBonus:
    attr: luck              # 属性 ID
    script: drop_bonus      # 脚本名（scripts/drop_bonus.nova）
```

脚本通过 `onDrop(killer, mobId, level, drops, attrValue)` 接收掉落列表，可以：

- **修改数量** — `item.setAmount(item.getAmount() * 2)`
- **添加特殊掉落** — `drops.add(buildItem("DIAMOND", 3))`
- **根据等级/属性值动态调整** — `if (level >= 5 && attrValue > 30) ...`
- **设置掉落物名称/Lore** — `setItemName(item, "§6幸运宝箱")`

插件内置示例脚本 `scripts/drop_bonus.nova`：

```javascript
fun onDrop(killer, mobId, level, drops, attrValue) {
    // 每点幸运 +1% 掉落数量
    var bonus = attrValue * 0.01
    var i = 0
    while (i < drops.size()) {
        var item = drops.get(i)
        var extra = item.getAmount() * bonus
        if (extra >= 1) { item.setAmount(item.getAmount() + extra) }
        i = i + 1
    }

    // 幸运 >= 30 时，5% 概率额外掉落钻石
    if (attrValue >= 30 && chance(0.05)) {
        drops.add(buildItem("DIAMOND", randomInt(1, 3)))
    }
}
```

> 需要先在 NovaAttribute 中注册 `luck` 属性（`attributes/` 目录下的 YAML）。

---

## 五、完整示例

```yaml
# MythicMobs/Mobs/nova_bosses.yml

FlameOverlord:
  Type: BLAZE
  Display: '&c&l炎魔领主'
  Health: 500
  Damage: 0
  NovaAttributes:
    max_health: 500
    physical_damage: 80
    magic_damage: 40
    fire_damage: 100
    physical_defense: 60
    critical_chance: 0.2
    critical_damage: 0.8
    movement_speed: 0.1
  NovaAttributeScale: 0.1    # 每级 +10%
  NovaDropBonus:
    attr: luck
    multiplier: 0.01
  Skills:
    # 普攻：基于物理+火焰属性造伤
    - nova-damage{attrs=physical_damage,fire_damage} @target ~onAttack
    # 每 5 秒 AOE 火焰爆发
    - nova-damage{attr=fire_damage;multiplier=2.0} @PIR{r=5} ~onTimer:100
    # 低血量时自我治疗
    - nova-heal{attr=max_health;multiplier=0.05} @self ~onTimer:40 0.3
    # 狂暴 Buff（血量低于 30% 触发）
    - nova-buff{id=enrage;attr=physical_damage;value=50;duration=200} @self ~onDamaged 0.3
  Conditions:
    # 只在物理攻击 >= 50 时释放狂暴
    - novaattr{attr=physical_damage;value>=50}

ShadowAssassin:
  Type: ZOMBIE
  Display: '&8&l暗影刺客'
  Health: 200
  Damage: 0
  NovaAttributeLines:
    - "物理攻击: 120~180"
    - "暴击几率: 40%"
    - "暴击伤害: 150%"
    - "闪避几率: 25%"
    - "移动速度: 20%"
  NovaAttributeScale: 0.05   # 每级 +5%
  Skills:
    - nova-damage{attr=physical_damage;multiplier=1.0} @target ~onAttack
    # 背刺：200 点真实伤害
    - nova-cast{true_damage=200} @target ~onTimer:200
    # 触发自定义属性脚本
    - nova-trigger{attr=stealth_strike} @target ~onTimer:300
```

---

## 六、注意事项

1. **伤害通道**：所有技能伤害通过 `attackTo()` bypass 通道，不会重复触发 NovaAttribute 的属性脚本（暴击、闪避等），适合作为技能的「基础伤害」。如果需要走完整伤害流程（含暴击/闪避判定），应使用 MythicMobs 原生伤害机制，让 NovaAttribute 的 DamageListener 自动接管。

2. **属性刷新**：怪物生成后属性会延迟 1 tick 应用，确保实体完全初始化。生成后自动设置满血。

3. **Buff ID 唯一性**：同一实体上相同 ID 的 Buff 会覆盖（非叠加）。如需叠加效果，使用不同 ID。

4. **原版属性同步**：如果配置了 `max_health` 和 `movement_speed`，VanillaSync 会自动同步到 Bukkit 原版属性，怪物的实际血量和移速会生效。
