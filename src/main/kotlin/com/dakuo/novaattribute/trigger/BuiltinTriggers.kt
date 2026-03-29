package com.dakuo.novaattribute.trigger

import org.bukkit.entity.LivingEntity
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent

/**
 * 内置事件触发器
 * 开箱即用，CUSTOM 属性通过 trigger-event 绑定：
 *   trigger-event: "INTERACT"
 *   trigger-event: "CONSUME"
 *   trigger-event: "SNEAK"
 *   trigger-event: "SPRINT"
 */
object BuiltinTriggers {

    fun registerAll() {
        TriggerManager.register(InteractTrigger)
        TriggerManager.register(ConsumeTrigger)
        TriggerManager.register(SneakTrigger)
        TriggerManager.register(SprintTrigger)
    }

    /** 玩家右键交互 */
    object InteractTrigger : EventTrigger<PlayerInteractEvent> {
        override val name = "INTERACT"
        override val eventClass = PlayerInteractEvent::class.java
        override fun condition(event: PlayerInteractEvent) =
            event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK
        override fun caster(event: PlayerInteractEvent): LivingEntity = event.player
        override fun params(event: PlayerInteractEvent): Map<String, Any> {
            val map = mutableMapOf<String, Any>("action" to event.action.name)
            event.item?.let { map["item"] = it }
            return map
        }
    }

    /** 玩家消耗物品（吃东西/喝药水） */
    object ConsumeTrigger : EventTrigger<PlayerItemConsumeEvent> {
        override val name = "CONSUME"
        override val eventClass = PlayerItemConsumeEvent::class.java
        override fun caster(event: PlayerItemConsumeEvent): LivingEntity = event.player
        override fun params(event: PlayerItemConsumeEvent) = mapOf<String, Any>("item" to event.item)
    }

    /** 玩家进入潜行 */
    object SneakTrigger : EventTrigger<PlayerToggleSneakEvent> {
        override val name = "SNEAK"
        override val eventClass = PlayerToggleSneakEvent::class.java
        override fun condition(event: PlayerToggleSneakEvent) = event.isSneaking
        override fun caster(event: PlayerToggleSneakEvent): LivingEntity = event.player
    }

    /** 玩家进入疾跑 */
    object SprintTrigger : EventTrigger<PlayerToggleSprintEvent> {
        override val name = "SPRINT"
        override val eventClass = PlayerToggleSprintEvent::class.java
        override fun condition(event: PlayerToggleSprintEvent) = event.isSprinting
        override fun caster(event: PlayerToggleSprintEvent): LivingEntity = event.player
    }
}
