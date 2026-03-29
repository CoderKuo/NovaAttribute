package com.dakuo.novaattribute.combat

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.attribute.AttributeTrigger
import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.novaattribute.util.Mirror
import com.dakuo.novaattribute.util.NumberFormatter
import com.dakuo.rulib.common.LogUtil
import com.dakuo.rulib.common.debug
import taboolib.common.platform.function.warning

object CombatHandler {

    fun calculateDamage(ctx: DamageContext): Double = Mirror.time("damage:calculate") {
        return@time try {
            val result = ScriptBridge.callFunction(
                "damage", "calculate",
                ctx.attacker, ctx.victim, ctx.originalDamage, ctx
            )
            val damage = (result as? Number)?.toDouble() ?: ctx.originalDamage

            val safeDamage = NumberFormatter.sanitize(damage)
            debug("[Combat] {attacker} → {victim} damage: {original} → {final}",
                "attacker" to ctx.attacker.name,
                "victim" to ctx.victim.name,
                "original" to ctx.originalDamage,
                "final" to safeDamage
            )
            safeDamage
        } catch (e: Exception) {
            warning("[NovaAttribute] damage script error: ${e.message}")
            ctx.originalDamage
        }
    }

    fun executeAttributeScripts(ctx: DamageContext, trigger: AttributeTrigger) {
        val attributes = AttributeRegistry.getByTrigger(trigger)
        for (attr in attributes) {
            val scriptName = attr.script ?: continue
            if (!ScriptBridge.isLoaded(scriptName)) continue
            try {
                val entity = when (trigger) {
                    AttributeTrigger.ATTACK, AttributeTrigger.KILL -> ctx.attacker
                    AttributeTrigger.DEFENSE -> ctx.victim
                    else -> ctx.attacker
                }
                val attrValue = AttributeManager.get(entity).get(attr.id)
                if (attrValue == 0.0) continue

                debug("[Combat] Executing {trigger} script '{script}' for {attr}={value}",
                    "trigger" to trigger.name,
                    "script" to scriptName,
                    "attr" to attr.id,
                    "value" to attrValue
                )
                val result = ScriptBridge.callFunction(
                    scriptName, "execute",
                    ctx.attacker, ctx.victim, ctx, attrValue
                )
                // 脚本可通过返回值修改伤害（返回 Number 则采用，返回 null 则不变）
                if (result is Number) {
                    ctx.finalDamage = result.toDouble()
                }
                // 防止 NaN/Infinity 污染
                ctx.finalDamage = NumberFormatter.sanitize(ctx.finalDamage)

                // 发送触发消息
                attr.messages?.let { msgs ->
                    val attacker = ctx.attacker
                    val victim = ctx.victim
                    if (attacker is org.bukkit.entity.Player && msgs.attacker != null) {
                        attacker.sendMessage(msgs.attacker)
                    }
                    if (victim is org.bukkit.entity.Player && msgs.victim != null) {
                        victim.sendMessage(msgs.victim)
                    }
                }
            } catch (e: Exception) {
                warning("[NovaAttribute] Attribute script '$scriptName' error: ${e.message}")
            }
        }
    }
}
