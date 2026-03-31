# 条件系统

## 装备条件

穿戴物品时自动检查条件，不满足则该装备的属性不生效。支持 **NBT** 和 **Lore** 两种来源。

### NBT 格式

```
/give @p diamond_sword{NovaCondition:{level:30,class:"warrior",permission:"vip.sword"}} 1
```

### Lore 格式

在物品 Lore 中写入条件关键词：

```
§7需要等级: 50
§7限制职业: 战士/法师
§7需要权限: vip.sword
```

关键词通过 `config.yml` 的 `condition.lore-patterns` 配置：

```yaml
condition:
  enabled: true
  lore-patterns:
    level: "需要等级|等级限制|Lv\\.|Level"
    class: "限制职业|需要职业|职业限制"
    permission: "需要权限"
```

格式为 `条件键: "关键词1|关键词2"`（正则分隔），匹配 Lore 中的 `关键词: 值`。

NBT 和 Lore 条件会合并，**NBT 优先覆盖 Lore**。

### 条件脚本

条件检查由 `conditions/default.nova` 脚本执行。默认支持：

| 条件键 | 说明 | 示例 |
|--------|------|------|
| `level` | 等级要求 | `level: 30` 或 Lore `需要等级: 30` |
| `class` | 职业要求（检查权限 `nova.class.xxx`） | `class: "warrior/mage"` 或 Lore `限制职业: 战士/法师` |
| `permission` | 权限要求 | `permission: "vip.sword"` 或 Lore `需要权限: vip.sword` |
| `source` | 来源位置 | `source: "mainhand"` |

可自定义扩展条件脚本。

---

## 内嵌条件

单属性行级条件。同一物品的不同属性可以有各自独立的生效条件。

### Lore 格式

```
§c物理攻击: 100
§6暴击几率: 25% / 等级>=50
§9魔法攻击: 200 / 职业:法师
```

`/` 后面的部分是内嵌条件。只有条件满足时该行属性才参与计算。

### NBT 格式

```json
{
  "NovaAttr": {
    "critical_chance": {
      "values": [0.25],
      "condition": "level>=50"
    }
  }
}
```

### 条件脚本

内嵌条件由 `conditions/inline.nova` 脚本执行。默认支持：

| 格式 | 说明 |
|------|------|
| `等级>=50` 或 `level>=50` | 等级大于等于 50 |
| `职业:法师` 或 `class:mage` | 职业（检查权限 `nova.class.xxx`） |
| `hp<30` 或 `血量<30` | 血量百分比低于 30% |

### 配置

```yaml
inline-condition:
  enabled: true
  lore-separator: " / "
```
