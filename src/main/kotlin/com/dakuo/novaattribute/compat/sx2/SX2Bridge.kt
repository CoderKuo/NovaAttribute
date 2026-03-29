package com.dakuo.novaattribute.compat.sx2

import com.dakuo.novaattribute.core.attribute.Attribute
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.attribute.AttributeTrigger
import com.dakuo.novaattribute.combat.DamageContext
import github.saukiya.sxattribute.data.attribute.AttributeType
import github.saukiya.sxattribute.data.attribute.SXAttributeData
import github.saukiya.sxattribute.data.attribute.SubAttribute
import github.saukiya.sxattribute.data.eventdata.sub.DamageData
import github.saukiya.sxattribute.data.eventdata.sub.UpdateData
import github.saukiya.sxattribute.event.SXDamageEvent
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.info

/**
 * SX2 兼容桥接管理器
 *
 * 职责:
 * 1. 接收 SX2 附属注册的 SubAttribute，转换为 NovaAttribute 属性
 * 2. 在战斗流程中调用 SX2 附属的 eventMethod()
 * 3. 触发 SXDamageEvent 供 SX2 附属监听
 */
object SX2Bridge {

    private var initialized = false

    fun init() {
        initialized = true
        // 启用已注册的 SX2 属性
        for (attr in SubAttribute.getAttributes()) {
            attr.onEnable()
        }
        val count = SubAttribute.getAttributes().size
        if (count > 0) {
            info("[NovaAttribute] SX2 compatibility: $count addon attributes loaded.")
        }
    }

    fun disable() {
        for (attr in SubAttribute.getAttributes()) {
            try { attr.onDisable() } catch (_: Exception) {}
        }
    }

    /**
     * SX2 附属调用 registerAttribute() 时回调
     */
    fun onAttributeRegistered(sub: SubAttribute) {
        // 将 SX2 SubAttribute 注册为 NovaAttribute 属性
        val trigger = when {
            sub.containsType(AttributeType.ATTACK) -> AttributeTrigger.ATTACK
            sub.containsType(AttributeType.DEFENCE) -> AttributeTrigger.DEFENSE
            sub.containsType(AttributeType.UPDATE) -> AttributeTrigger.PASSIVE
            else -> AttributeTrigger.PASSIVE
        }

        val attr = Attribute(
            id = "sx2:${sub.name}",
            name = sub.name,
            trigger = trigger,
            priority = sub.priority,
            combatPower = 0.0
        )
        AttributeRegistry.register(attr)

        if (initialized) {
            try { sub.onEnable() } catch (_: Exception) {}
        }
    }

    /**
     * 在伤害流程中调用 SX2 ATTACK/DEFENCE 类型属性的 eventMethod
     * 由 DamageListener 在 NovaAttribute 脚本执行后调用
     */
    fun executeDamageAttributes(ctx: DamageContext) {
        val sx2Attrs = SubAttribute.getAttributes()
        if (sx2Attrs.isEmpty()) return

        val attackerData = buildSXData(ctx.attacker)
        val defenderData = buildSXData(ctx.victim)

        val damageData = DamageData(
            ctx.victim, ctx.attacker,
            defenderData, attackerData,
            null, // event 可能已不可用，SX2 附属通常不直接操作 event
            ctx
        )
        damageData.setDamage(ctx.finalDamage)

        // 执行 ATTACK 类型
        for (attr in sx2Attrs) {
            if (!attr.containsType(AttributeType.ATTACK)) continue
            val values = attackerData.getValues(attr)
            var hasValue = false
            for (v in values) { if (v != 0.0) { hasValue = true; break } }
            if (!hasValue) continue
            try {
                attr.eventMethod(values, damageData)
            } catch (e: Exception) {
                taboolib.common.platform.function.warning("[NovaAttribute-SX2] Error in ATTACK attribute '${attr.name}': ${e.message}")
            }
        }

        // 执行 DEFENCE 类型
        for (attr in sx2Attrs) {
            if (!attr.containsType(AttributeType.DEFENCE)) continue
            val values = defenderData.getValues(attr)
            var hasValue = false
            for (v in values) { if (v != 0.0) { hasValue = true; break } }
            if (!hasValue) continue
            try {
                attr.eventMethod(values, damageData)
            } catch (e: Exception) {
                taboolib.common.platform.function.warning("[NovaAttribute-SX2] Error in DEFENCE attribute '${attr.name}': ${e.message}")
            }
        }

        // 同步伤害回 DamageContext
        if (!damageData.isCancelled) {
            ctx.finalDamage = damageData.damage
        }

        // 触发 SXDamageEvent 供其他 SX2 附属监听
        try {
            val sxEvent = SXDamageEvent(damageData)
            Bukkit.getPluginManager().callEvent(sxEvent)
            // 事件处理后再次同步
            ctx.finalDamage = damageData.damage
        } catch (_: Exception) {
        }
    }

    /**
     * 执行 UPDATE 类型属性
     */
    fun executeUpdateAttributes(entity: LivingEntity) {
        val updateData = UpdateData(entity)
        val entityData = buildSXData(entity)
        for (attr in SubAttribute.getAttributes()) {
            if (!attr.containsType(AttributeType.UPDATE)) continue
            try {
                attr.eventMethod(entityData.getValues(attr), updateData)
            } catch (_: Exception) {}
        }
    }

    /**
     * 从 NovaAttribute 的 AttributeMap 构建 SXAttributeData
     */
    private fun buildSXData(entity: LivingEntity): SXAttributeData {
        val novaMap = AttributeManager.getOrNull(entity)
        if (novaMap == null) return SXAttributeData()

        val data = SXAttributeData()
        for (attr in SubAttribute.getAttributes()) {
            if (attr.priority < 0) continue
            val values = data.getValues(attr)
            // 尝试从 NovaAttribute 获取值
            val novaValue = novaMap.get("sx2:${attr.name}")
            if (novaValue != 0.0 && values.isNotEmpty()) {
                values[0] = novaValue
            }
        }
        return data
    }
}
