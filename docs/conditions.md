# 条件系统

## 装备条件

在物品 NBT 中写入 `NovaCondition` 键，穿戴时自动检查条件是否满足。不满足则该装备的属性不生效。

### NBT 格式

```
/give @p diamond_sword{NovaCondition:{level:30,permission:"vip.sword"}} 1
```

### 条件脚本

条件检查由 `conditions/default.nova` 脚本执行。默认支持：

| 条件键 | 说明 | 示例 |
|--------|------|------|
| `level` | 等级要求 | `level: 30` |
| `class` | 职业要求（需 PAPI） | `class: "warrior"` |
| `permission` | 权限要求 | `permission: "vip.sword"` |

可自定义扩展条件脚本。

### 配置

```yaml
# config.yml
condition:
  enabled: true
```

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
| `等级>=50` | 等级大于等于 50 |
| `职业:法师` | 职业为法师 |
| `hp_percent<30` | 血量百分比低于 30% |

### 配置

```yaml
# config.yml
inline-condition:
  enabled: true
  lore-separator: " / "     # Lore 中属性值与条件的分隔符
```
