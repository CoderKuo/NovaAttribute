# 属性公式

根据 PAPI 占位符动态计算属性值。每次属性刷新时自动计算，作为 `formula:__combined__` 来源注入。

## 配置文件

`plugins/NovaAttribute/formulas.yml`

```yaml
formulas:
  # 简单表达式
  max_health: "return 20 + toNumber(placeholder(entity, \"%player_level%\")) * 5"
  physical_damage: "return 1 + toNumber(placeholder(entity, \"%player_level%\")) * 2"
  physical_defense: "return toNumber(placeholder(entity, \"%player_level%\")) * 1.5"

  # 复杂脚本（多行）
  critical_chance: |
    var level = toNumber(placeholder(entity, "%player_level%"))
    var base = 0.05
    if (level >= 30) { base = 0.1 }
    if (level >= 60) { base = 0.15 }
    return base
```

## 语法

每个公式是一段 NovaLang 脚本，编译为 JVM 字节码执行。

### 可用变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `entity` | Player | 当前玩家 |

### 可用函数

所有 [脚本 API](script-api.md) 函数均可使用：

```javascript
placeholder(entity, "%player_level%")    // PAPI 占位符
toNumber(value)                          // 转数字
getAttr(entity, "attrId")                // 获取其他属性值
getMobLevel(entity)                      // MM 怪物等级
chance(0.5)                              // 概率判定
```

### 返回值

脚本必须 `return` 一个数值。返回 0 或 null 则不注入该属性。

## 工作原理

1. 插件启动时编译所有公式为字节码（`ScriptBridge.compile()`）
2. 每次 `AttributeManager.refresh()` 时，对玩家逐个计算公式
3. 所有公式结果合并为一个 `AttributeData`，注入为 `formula:__combined__` 来源
4. 与装备、Buff、Provider 等来源一起参与属性汇总

## 注意事项

- 避免循环依赖：公式中 `getAttr()` 读取的是**本次刷新前**的属性值
- 需要 PlaceholderAPI 插件来使用 `%player_level%` 等占位符
- `/na reload` 会重新编译所有公式
