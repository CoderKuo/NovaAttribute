# 快速入门

## 安装

### 前置依赖

- Minecraft 服务端 1.12+ (Spigot/Paper)
- Java 8+
- [NovaScript](https://github.com/CoderKuo/NovaScript) 插件

### 安装步骤

1. 将 `NovaScript.jar` 和 `NovaAttribute.jar` 放入 `plugins/` 目录
2. 启动服务器
3. 插件自动生成配置文件到 `plugins/NovaAttribute/`

### 可选依赖

- **MythicMobs** — 安装后自动启用怪物属性集成
- **PlaceholderAPI** — 安装后自动注册 `%novaattr_xxx%` 占位符

## 目录结构

```
plugins/NovaAttribute/
├── config.yml              # 主配置
├── attributes/
│   ├── default.yml         # 默认属性定义（46个内置属性）
│   └── mechanics.yml       # 战斗机制属性（蓄力/弓箭/召唤）
├── scripts/
│   ├── damage.nova         # 伤害计算公式
│   ├── combine.nova        # 属性汇总方式
│   ├── critical.nova       # 暴击脚本
│   ├── dodge.nova          # 闪避脚本
│   └── ...                 # 更多属性脚本
├── conditions/
│   ├── default.nova        # 装备条件脚本
│   └── inline.nova         # 内嵌条件脚本
├── lore-template.yml       # Lore 生成模板
├── affixes.yml             # 词条系统
└── mob-elements.yml        # 怪物元素弱点
```

## 第一个自定义属性

### 1. 定义属性

创建 `plugins/NovaAttribute/attributes/custom.yml`：

```yaml
attributes:
  fire_burst:
    name: "§c火焰爆发"
    default: 0.0
    lore-divisor: 100.0      # 百分比属性
    trigger: ATTACK           # 攻击时触发
    script: fire_burst        # 脚本文件名
    priority: 25              # 执行优先级
    combat-power: 2.0         # 战斗力系数
```

### 2. 编写脚本

创建 `plugins/NovaAttribute/scripts/fire_burst.nova`：

```javascript
fun execute(attacker, victim, ctx, attrValue) {
    // attrValue 是攻击者的火焰爆发属性值（如 0.3 = 30%）
    if (chance(attrValue)) {
        // 触发！标记效果链
        markTriggered(ctx, "fire_burst")

        // 造成额外伤害（当前伤害的 50%）
        var bonus = ctx.getDamage() * 0.5
        ctx.setDamage(ctx.getDamage() + bonus)

        // 点燃目标 3 秒
        setFire(victim, 60)

        // 播放音效
        playSound(attacker, "ENTITY_BLAZE_SHOOT", 1.0, 1.0)
    }
}
```

### 3. 给物品添加属性

在物品 Lore 中写入：

```
§c火焰爆发: 30%
```

或通过 NBT：

```
/give @p diamond_sword{NovaAttr:{fire_burst:[0.3]}} 1
```

### 4. 重载并测试

```
/na reload
/na lookup @p
```

## 下一步

- [属性配置详解](attributes.md) — 所有配置项的含义
- [脚本 API](script-api.md) — 脚本中可用的所有函数
- [伤害计算](damage.md) — 理解伤害公式
