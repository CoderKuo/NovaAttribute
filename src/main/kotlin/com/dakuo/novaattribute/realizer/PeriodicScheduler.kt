package com.dakuo.novaattribute.realizer

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.attribute.AttributeTrigger
import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.rulib.common.debug
import org.bukkit.Bukkit
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * PERIODIC 类型属性的定时调度器
 * 每 tick 遍历在线玩家，对属性值 > 0 的 PERIODIC 属性按 interval 计时执行脚本
 */
object PeriodicScheduler {

    // entityUUID -> (attrId -> tickCounter)
    private val counters = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    private var task: PlatformExecutor.PlatformTask? = null

    fun start() {
        stop()
        task = submit(period = 1L) {
            tick()
        }
    }

    fun stop() {
        task?.cancel()
        task = null
        counters.clear()
    }

    fun cleanup(uuid: UUID) {
        counters.remove(uuid)
    }

    private fun tick() {
        val periodicAttrs = AttributeRegistry.getByTrigger(AttributeTrigger.PERIODIC)
        if (periodicAttrs.isEmpty()) return

        for (player in Bukkit.getOnlinePlayers()) {
            val uuid = player.uniqueId
            val map = AttributeManager.getOrNull(player) ?: continue
            val entityCounters = counters.getOrPut(uuid) { ConcurrentHashMap() }

            for (attr in periodicAttrs) {
                val scriptName = attr.script ?: continue
                if (!ScriptBridge.isLoaded(scriptName)) continue

                val attrValue = map.get(attr.id)
                if (attrValue == 0.0) {
                    entityCounters.remove(attr.id)
                    continue
                }

                val count = entityCounters.getOrDefault(attr.id, 0L) + 1
                if (count >= attr.interval) {
                    entityCounters[attr.id] = 0L
                    try {
                        debug("[Periodic] Executing '{script}' for {player} ({attr}={value})",
                            "script" to scriptName,
                            "player" to player.name,
                            "attr" to attr.id,
                            "value" to attrValue
                        )
                        ScriptBridge.callFunction(scriptName, "execute", player, attrValue)
                    } catch (e: Exception) {
                        warning("[NovaAttribute] Periodic script '$scriptName' error: ${e.message}")
                    }
                } else {
                    entityCounters[attr.id] = count
                }
            }
        }
    }
}
