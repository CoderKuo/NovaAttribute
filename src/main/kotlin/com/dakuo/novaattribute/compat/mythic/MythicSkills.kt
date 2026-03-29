package com.dakuo.novaattribute.compat.mythic

import com.dakuo.novaattribute.api.NovaAttributeAPI
import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.buff.Buff
import com.dakuo.novaattribute.trigger.TriggerManager
import com.dakuo.rulib.common.lang.RandomUtil
import ink.ptms.um.event.MobConditionLoadEvent
import ink.ptms.um.event.MobSkillLoadEvent
import ink.ptms.um.skill.SkillConfig
import ink.ptms.um.skill.SkillMeta
import ink.ptms.um.skill.SkillResult
import ink.ptms.um.skill.condition.EntityCondition
import ink.ptms.um.skill.type.EntityTargetSkill
import ink.ptms.um.skill.type.NoTargetSkill
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.event.SubscribeEvent

/**
 * NovaAttribute 自定义 MythicMobs 技能
 *
 * 用法（MythicMobs 怪物配置 Skills 节点）:
 *
 * ### nova-damage — 基于施法者属性造成伤害
 * ```yaml
 * Skills:
 *   - nova-damage{attr=physical_damage;multiplier=1.5} @target ~onAttack
 *   - nova-damage{attrs=fire_damage,magic_damage} @target ~onTimer:100
 * ```
 * 参数:
 *   attr     — 单个属性ID
 *   attrs    — 多个属性ID（逗号分隔，值累加）
 *   multiplier — 伤害倍率（默认 1.0）
 *
 * ### nova-cast — 指定属性值造成伤害（不读取施法者属性）
 * ```yaml
 * Skills:
 *   - nova-cast{physical_damage=100;fire_damage=50~80;true_damage=30} @target ~onAttack
 * ```
 * 参数: 任意 NovaAttribute 属性ID=固定值 或 属性ID=最小值~最大值
 * 额外参数:
 *   multiplier — 总伤害倍率（默认 1.0）
 *
 * ### nova-heal — 基于属性值治疗目标
 * ```yaml
 * Skills:
 *   - nova-heal{attr=max_health;multiplier=0.1} @self ~onTimer:60
 *   - nova-heal{amount=20} @self ~onDamaged
 * ```
 * 参数:
 *   attr       — 从施法者读取的属性ID
 *   multiplier — 属性值倍率（默认 1.0）
 *   amount     — 固定治疗量（优先于 attr）
 *
 * ### nova-buff — 给目标添加属性Buff
 * ```yaml
 * Skills:
 *   - nova-buff{id=fire_boost;attr=fire_damage;value=50;duration=100} @self ~onSpawn
 * ```
 * 参数:
 *   id       — Buff ID
 *   attr     — 属性ID
 *   value    — 属性值
 *   duration — 持续时间（tick）
 */
object MythicSkills {

    @SubscribeEvent
    fun onSkillLoad(event: MobSkillLoadEvent) {
        if (!MythicMobsHook.isAvailable()) return

        when {
            event.nameIs("nova-damage") -> event.register(NovaDamageSkill(event.config))
            event.nameIs("nova-cast") -> event.register(NovaCastSkill(event.config))
            event.nameIs("nova-heal") -> event.register(NovaHealSkill(event.config))
            event.nameIs("nova-buff") -> event.register(NovaBuffSkill(event.config))
            event.nameIs("nova-trigger") -> event.register(NovaTriggerSkill(event.config))
        }
    }

    /**
     * 注册自定义条件: novaattr
     *
     * MythicMobs 配置:
     * ```yaml
     * Conditions:
     *   - novaattr{attr=physical_damage;value>=100}
     *   - novaattr{attr=critical_chance;value>0.5}
     *   - novaattr{attr=max_health;value<=50}
     * ```
     */
    @SubscribeEvent
    fun onConditionLoad(event: MobConditionLoadEvent) {
        if (!MythicMobsHook.isAvailable()) return
        if (!event.name.equals("novaattr", ignoreCase = true)) return
        event.register(NovaAttrCondition(event.config))
    }

    /** 基于施法者属性造成伤害 */
    private class NovaDamageSkill(config: ink.ptms.um.skill.SkillConfig) : EntityTargetSkill {
        private val attr = config.getString("attr", "")
        private val attrs = config.getString("attrs", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        private val multiplier = config.getDouble("multiplier", 1.0)

        override fun cast(meta: SkillMeta, entity: org.bukkit.entity.Entity): SkillResult {
            val caster = meta.caster.entity as? LivingEntity ?: return SkillResult.INVALID_TARGET
            val target = entity as? LivingEntity ?: return SkillResult.INVALID_TARGET
            val map = AttributeManager.getOrNull(caster) ?: return SkillResult.ERROR

            var damage = 0.0
            if (attr.isNotEmpty()) {
                damage += map.get(attr)
            }
            for (a in attrs) {
                damage += map.get(a)
            }
            damage *= multiplier

            if (damage > 0) {
                NovaAttributeAPI.attackTo(caster, target, damage)
            }
            return SkillResult.SUCCESS
        }
    }

    /**
     * 指定属性值造成伤害（不读取施法者属性）
     *
     * 从技能配置中解析所有 NovaAttribute 属性ID作为参数名：
     *   - 固定值: physical_damage=100
     *   - 范围值: fire_damage=50~80（每次随机）
     *   - multiplier: 总伤害倍率
     *
     * 忽略不识别的参数（multiplier 等保留字段）。
     */
    private class NovaCastSkill(config: SkillConfig) : EntityTargetSkill {
        private val multiplier: Double
        private val entries: List<AttrEntry>

        data class AttrEntry(val attrId: String, val min: Double, val max: Double) {
            fun roll(): Double = if (min == max) min else RandomUtil.randomDouble(min, max)
        }

        init {
            multiplier = config.getDouble("multiplier", 1.0)
            val reserved = setOf("multiplier")
            val parsed = mutableListOf<AttrEntry>()
            for ((key, _) in config.entrySet()) {
                val attrId = key.lowercase()
                if (attrId in reserved) continue
                if (com.dakuo.novaattribute.core.attribute.AttributeRegistry.get(attrId) == null) {
                    taboolib.common.platform.function.warning("[NovaAttribute] nova-cast: unknown attribute '$attrId', skipped.")
                    continue
                }
                val raw = config.getString(key, "0")
                val tildeIdx = raw.indexOf('~')
                if (tildeIdx > 0) {
                    val min = raw.substring(0, tildeIdx).trim().toDoubleOrNull() ?: continue
                    val max = raw.substring(tildeIdx + 1).trim().toDoubleOrNull() ?: continue
                    parsed.add(AttrEntry(attrId, min, max))
                } else {
                    val value = raw.toDoubleOrNull() ?: continue
                    parsed.add(AttrEntry(attrId, value, value))
                }
            }
            entries = parsed
        }

        override fun cast(meta: SkillMeta, entity: org.bukkit.entity.Entity): SkillResult {
            val caster = meta.caster.entity as? LivingEntity ?: return SkillResult.INVALID_TARGET
            val target = entity as? LivingEntity ?: return SkillResult.INVALID_TARGET
            if (entries.isEmpty()) return SkillResult.INVALID_CONFIG

            var total = 0.0
            for (entry in entries) {
                total += entry.roll()
            }
            total *= multiplier

            if (total > 0) {
                NovaAttributeAPI.attackTo(caster, target, total)
            }
            return SkillResult.SUCCESS
        }
    }

    /** 基于属性值治疗目标 */
    private class NovaHealSkill(config: ink.ptms.um.skill.SkillConfig) : EntityTargetSkill {
        private val attr = config.getString("attr", "")
        private val multiplier = config.getDouble("multiplier", 1.0)
        private val amount = config.getDouble("amount", 0.0)

        override fun cast(meta: SkillMeta, entity: org.bukkit.entity.Entity): SkillResult {
            val target = entity as? LivingEntity ?: return SkillResult.INVALID_TARGET

            val heal = if (amount > 0) {
                amount
            } else if (attr.isNotEmpty()) {
                val caster = meta.caster.entity as? LivingEntity ?: return SkillResult.ERROR
                val map = AttributeManager.getOrNull(caster) ?: return SkillResult.ERROR
                map.get(attr) * multiplier
            } else {
                return SkillResult.INVALID_CONFIG
            }

            if (heal > 0) {
                target.health = (target.health + heal).coerceAtMost(target.maxHealth)
            }
            return SkillResult.SUCCESS
        }
    }

    /** 给目标添加属性Buff */
    private class NovaBuffSkill(config: ink.ptms.um.skill.SkillConfig) : EntityTargetSkill {
        private val id = config.getString("id", "")
        private val attr = config.getString("attr", "")
        private val value = config.getDouble("value", 0.0)
        private val durationTicks = config.getLong("duration", 200L)

        override fun cast(meta: SkillMeta, entity: org.bukkit.entity.Entity): SkillResult {
            val target = entity as? LivingEntity ?: return SkillResult.INVALID_TARGET
            if (id.isEmpty() || attr.isEmpty()) return SkillResult.INVALID_CONFIG

            val data = AttributeData()
            data.set(attr, value)
            val durationMs = durationTicks * 50L
            val buff = Buff(
                id = id,
                data = data,
                duration = durationMs,
                expireAt = System.currentTimeMillis() + durationMs
            )
            NovaAttributeAPI.addBuff(target, buff)
            return SkillResult.SUCCESS
        }
    }

    /**
     * 触发 CUSTOM 属性脚本
     *
     * 用法:
     *   - nova-trigger{attr=my_custom_attr} @self ~onAttack
     *   - nova-trigger{attr=my_custom_attr} @target ~onDamaged
     *
     * 施法者作为 caster，目标作为 target 传给 CUSTOM 属性脚本。
     * 无目标版（@self）: caster = 施法者, target = null
     * 有目标版（@target）: caster = 施法者, target = 目标实体
     */
    private class NovaTriggerSkill(config: SkillConfig) : EntityTargetSkill, NoTargetSkill {
        private val attrId = config.getString("attr", "")

        override fun cast(meta: SkillMeta, entity: Entity): SkillResult {
            val caster = meta.caster.entity as? LivingEntity ?: return SkillResult.INVALID_TARGET
            val target = entity as? LivingEntity
            if (attrId.isEmpty()) return SkillResult.INVALID_CONFIG
            TriggerManager.triggerCustomAttribute(caster, attrId, target)
            return SkillResult.SUCCESS
        }

        override fun cast(meta: SkillMeta): SkillResult {
            val caster = meta.caster.entity as? LivingEntity ?: return SkillResult.INVALID_TARGET
            if (attrId.isEmpty()) return SkillResult.INVALID_CONFIG
            TriggerManager.triggerCustomAttribute(caster, attrId)
            return SkillResult.SUCCESS
        }
    }

    /**
     * 自定义条件: novaattr
     *
     * 检查实体的 NovaAttribute 属性值是否满足条件。
     *
     * 用法:
     *   - novaattr{attr=physical_damage;value>=100}
     *   - novaattr{attr=critical_chance;value>0.5}
     *   - novaattr{attr=max_health;value<=50}
     *
     * 支持的运算符: >=, <=, >, <, =
     */
    private class NovaAttrCondition(config: SkillConfig) : EntityCondition {
        private val attrId = config.getString("attr", "")
        private val valueExpr = config.getString("value", "")
        private val operator: String
        private val threshold: Double

        init {
            // 解析 ">=100", ">50", "<=200", "<10", "=0.5"
            val expr = valueExpr.trim()
            when {
                expr.startsWith(">=") -> { operator = ">="; threshold = expr.substring(2).toDoubleOrNull() ?: 0.0 }
                expr.startsWith("<=") -> { operator = "<="; threshold = expr.substring(2).toDoubleOrNull() ?: 0.0 }
                expr.startsWith(">") -> { operator = ">"; threshold = expr.substring(1).toDoubleOrNull() ?: 0.0 }
                expr.startsWith("<") -> { operator = "<"; threshold = expr.substring(1).toDoubleOrNull() ?: 0.0 }
                expr.startsWith("=") -> { operator = "="; threshold = expr.substring(1).toDoubleOrNull() ?: 0.0 }
                else -> {
                    // 纯数字默认 >=
                    operator = ">="
                    threshold = expr.toDoubleOrNull() ?: 0.0
                }
            }
        }

        override fun check(entity: Entity?): Boolean {
            val e = entity as? LivingEntity ?: return false
            if (attrId.isEmpty()) return false
            val map = AttributeManager.getOrNull(e) ?: return false
            val value = map.get(attrId)
            return when (operator) {
                ">=" -> value >= threshold
                "<=" -> value <= threshold
                ">" -> value > threshold
                "<" -> value < threshold
                "=" -> value == threshold
                else -> false
            }
        }
    }
}
