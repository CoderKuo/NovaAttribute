# 开发者 API

## Maven / Gradle 依赖

```kotlin
// build.gradle.kts
repositories {
    maven("https://jitpack.io")
}
dependencies {
    compileOnly("com.github.CoderKuo:NovaAttribute:版本号")
}
```

## 核心 API

所有功能通过 `NovaAttributeAPI` 单例访问：

```kotlin
import com.dakuo.novaattribute.api.NovaAttributeAPI
```

### 属性查询

```kotlin
val hp = NovaAttributeAPI.getAttribute(player, "max_health")
val attrs = NovaAttributeAPI.getAttributes(player)  // Map<String, Double>
val power = NovaAttributeAPI.getCombatPower(player)
```

### 属性来源管理

```kotlin
// 添加来源（一行代码注入属性）
val data = AttributeData()
data.set("physical_damage", 100.0)
data.set("critical_chance", 0.15)
NovaAttributeAPI.updateSource(player, "myplugin:skill_bonus", data)

// 从物品添加
NovaAttributeAPI.updateSource(player, "myplugin:ring", ringItemStack)

// 移除来源
NovaAttributeAPI.removeSource(player, "myplugin:skill_bonus")

// 按命名空间批量移除
NovaAttributeAPI.removeByNamespace(player, "myplugin")

// 强制刷新
NovaAttributeAPI.refresh(player)
```

### Buff 管理

```kotlin
NovaAttributeAPI.addBuff(player, buff)
NovaAttributeAPI.removeBuff(player, "buff_id")
NovaAttributeAPI.hasBuff(player, "buff_id")
NovaAttributeAPI.getBuffRemaining(player, "buff_id")  // 毫秒
NovaAttributeAPI.getBuffStacks(player, "buff_id")
```

### 冷却 / 计数器

```kotlin
NovaAttributeAPI.setCooldown(player, "my_cd", 100L)
NovaAttributeAPI.isCooldownReady(player, "my_cd")

NovaAttributeAPI.incrementCounter(player, "combo", 1)
NovaAttributeAPI.getCounter(player, "combo")
NovaAttributeAPI.resetCounter(player, "combo")
```

### CUSTOM 触发器

```kotlin
// 代码触发
NovaAttributeAPI.triggerCustomAttribute(player, "my_custom_attr")
NovaAttributeAPI.triggerCustomAttribute(player, "my_custom_attr", target, params)

// 注册事件触发器
NovaAttributeAPI.registerTrigger(myEventTrigger)
NovaAttributeAPI.unregisterTrigger("TRIGGER_NAME")
```

### 其他

```kotlin
NovaAttributeAPI.attackTo(attacker, victim, 100.0)  // bypass 伤害
NovaAttributeAPI.setHealthScale(player, 20.0)        // 血量条缩放
NovaAttributeAPI.rebuildLore(itemStack)               // 重建 Lore
NovaAttributeAPI.registerAttribute(attribute)         // 注册属性
NovaAttributeAPI.registerProvider(provider)            // 注册 Provider
```

## AttributeProvider 接口

第三方插件通过 Provider 接口为玩家提供额外的属性来源：

```kotlin
class MyProvider : AttributeProvider {
    override val id = "myplugin"

    override fun provide(player: Player): AttributeBundle {
        return AttributeBundle.build {
            // 方式一：提交物品（系统自动解析 Lore/NBT）
            item("ring", getRingItem(player))
            item("necklace", getNecklaceItem(player))

            // 方式二：直接提供属性数据
            source("passive_bonus") {
                set("physical_damage", calculateBonus(player))
            }
        }
    }
}

// 注册
NovaAttributeAPI.registerProvider(MyProvider())

// 注销
NovaAttributeAPI.unregisterProvider("myplugin")
```

## 事件列表

| 事件 | 时机 | 可取消 |
|------|------|--------|
| `AttributeRefreshEvent` | 属性刷新前（PRE） | 否 |
| `AttributeUpdateEvent` | 属性刷新后（POST） | 否 |
| `AttributeChangeEvent` | 单个属性值变化时 | 否 |
| `NovaAttributeDamageEvent` | 伤害计算完成后 | 是 |
| `VanillaAttributeSyncEvent` | 原版属性同步后 | 否 |
| `BuffAddEvent` | Buff 添加时 | 是 |
| `BuffRemoveEvent` | Buff 移除时 | 否 |
| `BuffStackEvent` | Buff 叠加时 | 否 |

## NovaScript 全局函数库

NovaAttribute 在 `onLoad()` 阶段注册 `novaattr` 全局函数库。其他使用 NovaScript 的插件的脚本可直接调用：

```javascript
var hp = novaattr.getAttr(player, "max_health")
var hp2 = novaattr.getAttr("Steve", "max_health")  // 支持玩家名
novaattr.addBuff(player, "boost", 6000, "physical_damage", 100.0)
novaattr.refresh(player)
novaattr.triggerCustom(player, "my_attr")
```

完整函数列表见 [脚本 API](script-api.md)。
