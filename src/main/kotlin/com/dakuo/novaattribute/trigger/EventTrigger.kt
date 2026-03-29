package com.dakuo.novaattribute.trigger

import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.EventPriority

/**
 * 自定义事件触发器接口
 * 将任何 Bukkit 事件绑定到 CUSTOM 类型属性的触发流程
 *
 * 使用方式:
 * 1. 实现此接口
 * 2. 通过 NovaAttributeAPI.registerTrigger() 注册
 * 3. CUSTOM 属性通过 trigger-event: "触发器名称" 绑定
 */
interface EventTrigger<E : Event> {

    /** 触发器名称，如 "SKILL_CAST"、"INTERACT" */
    val name: String

    /** 绑定的 Bukkit 事件类型 */
    val eventClass: Class<E>

    /** 监听优先级 */
    val priority: EventPriority get() = EventPriority.NORMAL

    /** 是否忽略已取消的事件 */
    val ignoreCancelled: Boolean get() = false

    /** 事件触发条件（返回 false 则不处理） */
    fun condition(event: E): Boolean = true

    /** 从事件中提取施放者（属性持有者），返回 null 则跳过 */
    fun caster(event: E): LivingEntity?

    /** 从事件中提取目标（可选） */
    fun target(event: E): LivingEntity? = null

    /** 从事件中提取额外参数（传递给属性脚本） */
    fun params(event: E): Map<String, Any> = emptyMap()
}
