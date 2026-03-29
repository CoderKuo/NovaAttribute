# 伤害计算

## 战斗流程

```
EntityDamageByEntityEvent 触发
        │
        ▼ LOWEST 优先级
  创建 DamageContext（攻击者、受害者、原始伤害）
        │
        ▼ HIGHEST 优先级
  ① damage.nova 计算基础伤害
        │
  ② 按 priority 执行所有 ATTACK 属性脚本
        │
  ③ 按 priority 执行所有 DEFENSE 属性脚本
        │
  ③.5 执行 SX2 兼容属性 + SXDamageEvent
        │
  ④ 抛出 NovaAttributeDamageEvent（可取消）
        │
  ⑤ 显示伤害指示器 + 发送战斗消息
        │
  ⑥ 应用最终伤害
        │
  ⑦ 如果目标死亡 → 执行 KILL 属性脚本
```

## 默认伤害公式（damage.nova）

```
总伤害 = 物理伤害 + 魔法伤害 + 真实伤害 + 元素伤害

物理伤害 = max(物理攻击 - 有效防御, 1.0)
有效防御 = 物理防御 × (1 - 护甲穿透)

魔法伤害 = max(魔法攻击 - 有效抗性, 0.0)
有效抗性 = 魔法抗性 × (1 - 魔法穿透)

元素伤害 = 各元素攻击 - 对应元素抗性（最低 0）

蓄力加成 = 0.2 + 有效蓄力² × 0.8
弓箭加成 = 远程攻击时叠加 arrow_damage

PVP/PVE 加成 = 总伤害 × (1 + pvp/pve_damage_bonus)
```

## 属性执行顺序

ATTACK 和 DEFENSE 脚本按 `priority` 升序执行：

| 优先级 | 属性 | 类型 |
|--------|------|------|
| 0 | dodge（闪避） | DEFENSE |
| 5 | block_chance（格挡） | DEFENSE |
| 10 | critical_chance（暴击） | ATTACK |
| 15 | toughness（韧性） | DEFENSE |
| 20 | 状态触发（点燃/中毒等） | ATTACK |
| 30 | knockback（击退） | ATTACK |
| 40 | damage_reduction（伤害减免） | DEFENSE |
| 50 | thorns（反伤） | DEFENSE |
| 90 | lifesteal（吸血） | ATTACK |
| 95 | kill_heal / exp_bonus | KILL |

## 脚本返回值

- 返回 `Number` → 覆盖当前伤害值
- 返回 `null` → 不修改伤害
- 直接操作 `ctx.setDamage()` → 也能修改伤害

## bypass 伤害

通过 `attackTo(attacker, victim, damage)` 或 API `NovaAttributeAPI.attackTo()` 造成的伤害会跳过属性计算，直接扣血。适用于技能直接伤害。

## 效果链

属性脚本之间通过 `markTriggered` / `isTriggered` 通信：

```javascript
// 闪避触发后，暴击/反伤/格挡都可以检查并跳过
if (isTriggered(ctx, "dodge")) { return null }
```
