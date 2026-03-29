package com.dakuo.novaattribute.listener

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.rulib.common.debug
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityShootBowEvent
import taboolib.common.platform.event.SubscribeEvent

/**
 * 战斗机制监听器
 * 处理弓箭发射等需要事件钩子的机制
 */
object MechanicsListener {

    @SubscribeEvent
    fun onShootBow(e: EntityShootBowEvent) {
        val shooter = e.entity as? LivingEntity ?: return
        val map = AttributeManager.getOrNull(shooter) ?: return

        // 箭矢速度加成
        val arrowSpeed = map.get("arrow_speed")
        if (arrowSpeed > 0) {
            val velocity = e.projectile.velocity
            e.projectile.velocity = velocity.multiply(1.0 + arrowSpeed / 100.0)
            debug("[Mechanics] Arrow speed +{speed}% for {player}",
                "speed" to arrowSpeed,
                "player" to shooter.name
            )
        }
    }
}
