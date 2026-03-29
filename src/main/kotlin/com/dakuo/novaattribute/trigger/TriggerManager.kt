package com.dakuo.novaattribute.trigger

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.attribute.AttributeTrigger
import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.rulib.common.debug
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import taboolib.common.platform.function.warning
import taboolib.platform.BukkitPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * CUSTOM 触发器注册中心
 *
 * 支持两种触发模式:
 * 1. 代码触发: triggerCustomAttribute(entity, attrId, params) — 其他插件直接调用 API
 * 2. 事件触发: registerTrigger(EventTrigger) — 注册事件触发器，属性通过 trigger-event 绑定
 */
object TriggerManager {

    private val triggers = ConcurrentHashMap<String, EventTrigger<*>>()
    private val listeners = ConcurrentHashMap<String, Listener>()

    /**
     * 注册事件触发器
     */
    fun <E : Event> register(trigger: EventTrigger<E>) {
        unregister(trigger.name)

        triggers[trigger.name] = trigger

        val listener = object : Listener {}
        listeners[trigger.name] = listener

        val executor = EventExecutor { _, event ->
            try {
                val e = trigger.eventClass.cast(event) ?: return@EventExecutor
                handleTrigger(trigger, e)
            } catch (_: ClassCastException) {
            }
        }

        Bukkit.getPluginManager().registerEvent(
            trigger.eventClass,
            listener,
            trigger.priority,
            executor,
            BukkitPlugin.getInstance(),
            trigger.ignoreCancelled
        )

        debug("[Trigger] Registered '{name}' → {event}",
            "name" to trigger.name,
            "event" to trigger.eventClass.simpleName
        )
    }

    /**
     * 注销触发器
     */
    fun unregister(name: String) {
        triggers.remove(name)
        listeners.remove(name)?.let { HandlerList.unregisterAll(it) }
    }

    /**
     * 注销所有触发器
     */
    fun unregisterAll() {
        for (name in triggers.keys.toList()) {
            unregister(name)
        }
    }

    fun get(name: String): EventTrigger<*>? = triggers[name]

    fun getAll(): Map<String, EventTrigger<*>> = triggers.toMap()

    /**
     * 代码触发模式: 直接触发某个 CUSTOM 属性的脚本
     */
    fun triggerCustomAttribute(
        caster: LivingEntity,
        attributeId: String,
        target: LivingEntity? = null,
        params: Map<String, Any> = emptyMap()
    ) {
        val attr = AttributeRegistry.get(attributeId) ?: return
        if (attr.trigger != AttributeTrigger.CUSTOM) return
        val scriptName = attr.script ?: return
        if (!ScriptBridge.isLoaded(scriptName)) return

        val attrValue = AttributeManager.get(caster).get(attr.id)
        if (attrValue == 0.0) return

        try {
            debug("[Trigger] Code trigger '{attr}' for {player} (value={value})",
                "attr" to attr.id,
                "player" to caster.name,
                "value" to attrValue
            )
            ScriptBridge.callFunction(scriptName, "execute", caster, target, params, attrValue)
        } catch (e: Exception) {
            warning("[NovaAttribute] Custom trigger script '$scriptName' error: ${e.message}")
        }
    }

    /**
     * 事件触发模式: 触发绑定到指定触发器名称的所有 CUSTOM 属性
     */
    fun triggerByEvent(
        triggerName: String,
        caster: LivingEntity,
        target: LivingEntity? = null,
        params: Map<String, Any> = emptyMap()
    ) {
        val attrs = AttributeRegistry.getByTrigger(AttributeTrigger.CUSTOM)
            .filter { it.triggerEvent == triggerName }

        for (attr in attrs) {
            val scriptName = attr.script ?: continue
            if (!ScriptBridge.isLoaded(scriptName)) continue

            val attrValue = AttributeManager.get(caster).get(attr.id)
            if (attrValue == 0.0) continue

            try {
                debug("[Trigger] Event '{trigger}' → '{attr}' for {player} (value={value})",
                    "trigger" to triggerName,
                    "attr" to attr.id,
                    "player" to caster.name,
                    "value" to attrValue
                )
                ScriptBridge.callFunction(scriptName, "execute", caster, target, params, attrValue)
            } catch (e: Exception) {
                warning("[NovaAttribute] Custom trigger script '$scriptName' error: ${e.message}")
            }
        }
    }

    private fun <E : Event> handleTrigger(trigger: EventTrigger<E>, event: E) {
        if (!trigger.condition(event)) return
        val caster = trigger.caster(event) ?: return
        val target = trigger.target(event)
        val params = trigger.params(event)
        triggerByEvent(trigger.name, caster, target, params)
    }
}
