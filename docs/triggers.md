# 触发器系统

## PERIODIC 触发器

定时自动执行的属性脚本。适用于生命恢复、光环效果等。

```yaml
# attributes/default.yml
health_regen:
  name: "§a生命恢复"
  trigger: PERIODIC
  script: health_regen
  interval: 60              # 每 60 tick（3秒）执行一次
```

脚本签名：
```javascript
fun execute(entity, attrValue) {
    if (attrValue > 0 && isAlive(entity)) {
        safeHeal(entity, attrValue)
    }
}
```

只对属性值 > 0 的在线玩家执行，不浪费性能。

## CUSTOM 触发器

支持两种触发模式：

### 模式一：代码触发

其他插件通过 API 直接触发：

```kotlin
// Java/Kotlin
NovaAttributeAPI.triggerCustomAttribute(player, "my_attr_id")
NovaAttributeAPI.triggerCustomAttribute(player, "my_attr_id", target, params)
```

```javascript
// 脚本中
triggerCustom(entity, "my_attr_id")
triggerCustomEx(entity, "my_attr_id", target, params)
```

### 模式二：事件绑定

通过 `trigger-event` 绑定到事件触发器，事件发生时自动执行。

```yaml
# attributes/custom.yml
right_click_heal:
  name: "§a右键治愈"
  trigger: CUSTOM
  trigger-event: "INTERACT"    # 绑定到 INTERACT 触发器
  script: right_click_heal
```

脚本签名：
```javascript
fun execute(caster, target, params, attrValue) {
    // caster: 施放者
    // target: 目标（可为 null）
    // params: 额外参数 Map
    // attrValue: 属性值
}
```

## 内置触发器

| 名称 | 事件 | 说明 |
|------|------|------|
| `INTERACT` | PlayerInteractEvent | 玩家右键交互 |
| `CONSUME` | PlayerItemConsumeEvent | 吃东西/喝药水 |
| `SNEAK` | PlayerToggleSneakEvent | 进入潜行 |
| `SPRINT` | PlayerToggleSprintEvent | 进入疾跑 |

## 注册自定义触发器

第三方插件通过 API 注册：

```kotlin
NovaAttributeAPI.registerTrigger(object : EventTrigger<PlayerCastSkillEvent> {
    override val name = "SKILL_CAST"
    override val eventClass = PlayerCastSkillEvent::class.java

    override fun caster(event: PlayerCastSkillEvent) = event.player
    override fun target(event: PlayerCastSkillEvent) = event.target
    override fun params(event: PlayerCastSkillEvent) = mapOf(
        "skill_name" to event.skillName,
        "skill_level" to event.level
    )
})

// 注销
NovaAttributeAPI.unregisterTrigger("SKILL_CAST")
```

然后 CUSTOM 属性就可以绑定：
```yaml
skill_amplify:
  trigger: CUSTOM
  trigger-event: "SKILL_CAST"
  script: skill_amplify
```
