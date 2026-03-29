package com.dakuo.novaattribute.script

import com.dakuo.novascript.NovaScriptAPI
import com.dakuo.novascript.ScriptConfigurer
import com.dakuo.novaattribute.api.NovaAttributeAPI
import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.buff.Buff
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 将 NovaAttribute 的 API 导出为 NovaScript 全局函数库
 *
 * 其他插件的脚本可通过库名调用:
 *   var hp = novaattr.getAttr(player, "max_health")
 *   novaattr.addBuff(player, "boost", 6000, "physical_damage", 100.0)
 *   novaattr.triggerCustom(player, "my_custom_attr")
 */
object ScriptLibraryExport {

    private const val LIB_NAME = "novaattr"

    /**
     * 解析实体参数：支持 LivingEntity 对象或玩家名字符串
     */
    private fun resolveEntity(obj: Any?): LivingEntity? {
        if (obj is LivingEntity) return obj
        if (obj is String) return Bukkit.getPlayerExact(obj)
        return null
    }

    private fun resolvePlayer(obj: Any?): Player? {
        if (obj is Player) return obj
        if (obj is String) return Bukkit.getPlayerExact(obj)
        return null
    }

    fun register() {
        NovaScriptAPI.defineLibrary("NovaAttribute", LIB_NAME, ScriptConfigurer { lib ->

            // ====== 属性查询 ======

            lib.defineFunction("getAttr") { entity, attrId ->
                val e = resolveEntity(entity) ?: return@defineFunction 0.0
                NovaAttributeAPI.getAttribute(e, attrId as String)
            }

            lib.defineFunction("getAttrRandom") { entity, attrId ->
                val e = resolveEntity(entity) ?: return@defineFunction 0.0
                NovaAttributeAPI.getAttributeRandom(e, attrId as String)
            }

            lib.defineFunction("getAttrMin") { entity, attrId ->
                val e = resolveEntity(entity) ?: return@defineFunction 0.0
                NovaAttributeAPI.getAttribute(e, "${attrId as String}_min")
            }

            lib.defineFunction("getAttrMax") { entity, attrId ->
                val e = resolveEntity(entity) ?: return@defineFunction 0.0
                NovaAttributeAPI.getAttribute(e, "${attrId as String}_max")
            }

            lib.defineFunction("getAttrs") { entity ->
                val e = resolveEntity(entity) ?: return@defineFunction emptyMap<String, Double>()
                NovaAttributeAPI.getAttributes(e)
            }

            lib.defineFunction("getCombatPower") { entity ->
                val e = resolveEntity(entity) ?: return@defineFunction 0.0
                NovaAttributeAPI.getCombatPower(e)
            }

            // ====== 来源管理 ======

            lib.defineFunction("updateSource") { entity, source, attrId, value ->
                val e = resolveEntity(entity) ?: return@defineFunction null
                val data = AttributeData()
                data.set(attrId as String, (value as Number).toDouble())
                NovaAttributeAPI.updateSource(e, source as String, data)
                null
            }

            lib.defineFunction("removeSource") { entity, source ->
                val e = resolveEntity(entity) ?: return@defineFunction null
                NovaAttributeAPI.removeSource(e, source as String)
                null
            }

            lib.defineFunction("refresh") { entity ->
                val e = resolveEntity(entity) ?: return@defineFunction null
                NovaAttributeAPI.refresh(e)
                null
            }

            // ====== Buff 管理 ======

            lib.defineFunctionVararg("addBuff") { args ->
                // addBuff(entity, id, durationMs, attrId, value)
                // addBuff(entity, id, durationMs, attrId, value, stackable, persistent)
                val entity = resolveEntity(args.getOrNull(0)) ?: return@defineFunctionVararg false
                val id = args.getOrNull(1) as? String ?: return@defineFunctionVararg false
                val durationMs = (args.getOrNull(2) as? Number)?.toLong() ?: return@defineFunctionVararg false
                val attrId = args.getOrNull(3) as? String ?: return@defineFunctionVararg false
                val value = (args.getOrNull(4) as? Number)?.toDouble() ?: return@defineFunctionVararg false
                val stackable = args.getOrNull(5) as? Boolean ?: false
                val persistent = args.getOrNull(6) as? Boolean ?: false

                val data = AttributeData()
                data.set(attrId, value)
                val buff = Buff(
                    id = id,
                    data = data,
                    duration = durationMs,
                    stackable = stackable,
                    persistent = persistent,
                    expireAt = if (durationMs < 0) Long.MAX_VALUE else System.currentTimeMillis() + durationMs
                )
                NovaAttributeAPI.addBuff(entity, buff)
            }

            lib.defineFunction("removeBuff") { entity, buffId ->
                val e = resolveEntity(entity) ?: return@defineFunction null
                NovaAttributeAPI.removeBuff(e, buffId as String)
                null
            }

            lib.defineFunction("hasBuff") { entity, buffId ->
                val e = resolveEntity(entity) ?: return@defineFunction false
                NovaAttributeAPI.hasBuff(e, buffId as String)
            }

            lib.defineFunction("getBuffRemaining") { entity, buffId ->
                val e = resolveEntity(entity) ?: return@defineFunction 0L
                NovaAttributeAPI.getBuffRemaining(e, buffId as String)
            }

            // ====== 冷却 ======

            lib.defineFunction("cooldownReady") { entity, key ->
                val e = resolveEntity(entity) ?: return@defineFunction false
                NovaAttributeAPI.isCooldownReady(e, key as String)
            }

            lib.defineFunction("setCooldown") { entity, key, ticks ->
                val e = resolveEntity(entity) ?: return@defineFunction null
                NovaAttributeAPI.setCooldown(e, key as String, (ticks as Number).toLong())
                null
            }

            // ====== 计数器 ======

            lib.defineFunction("increment") { entity, key, amount ->
                val e = resolveEntity(entity) ?: return@defineFunction 0
                NovaAttributeAPI.incrementCounter(e, key as String, (amount as? Number)?.toInt() ?: 1)
            }

            lib.defineFunction("getCounter") { entity, key ->
                val e = resolveEntity(entity) ?: return@defineFunction 0
                NovaAttributeAPI.getCounter(e, key as String)
            }

            lib.defineFunction("resetCounter") { entity, key ->
                val e = resolveEntity(entity) ?: return@defineFunction null
                NovaAttributeAPI.resetCounter(e, key as String)
                null
            }

            // ====== 伤害控制 ======

            lib.defineFunction("attackTo") { attacker, victim, damage ->
                val a = resolveEntity(attacker) ?: return@defineFunction null
                val v = resolveEntity(victim) ?: return@defineFunction null
                NovaAttributeAPI.attackTo(a, v, (damage as Number).toDouble())
                null
            }

            // ====== 血量条缩放 ======

            lib.defineFunction("setHealthScale") { player, scale ->
                val p = resolvePlayer(player) ?: return@defineFunction null
                NovaAttributeAPI.setHealthScale(p, (scale as Number).toDouble())
                null
            }

            // ====== CUSTOM 触发器 ======

            lib.defineFunction("triggerCustom") { entity, attrId ->
                val e = resolveEntity(entity) ?: return@defineFunction null
                NovaAttributeAPI.triggerCustomAttribute(e, attrId as String)
                null
            }

            // ====== Lore 生成 ======

            lib.defineFunction("rebuildLore") { item ->
                val stack = item as? ItemStack ?: return@defineFunction item
                NovaAttributeAPI.rebuildLore(stack)
            }
        })
    }

    fun unregister() {
        NovaScriptAPI.removeLibrary("NovaAttribute", LIB_NAME)
    }
}
