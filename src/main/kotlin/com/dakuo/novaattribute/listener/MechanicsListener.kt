package com.dakuo.novaattribute.listener

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.rulib.common.debug
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityShootBowEvent
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 战斗机制监听器
 * 处理弓箭发射、蓄力缓存等需要事件钩子的机制
 */
object MechanicsListener {

    /**
     * 蓄力值缓存
     * 通过 PacketReceiveEvent 监听客户端挥手数据包（PacketPlayInArmAnimation）
     * 数据包在 Netty 线程到达时冷却还未被 Minecraft 主线程重置，此时获取的是真实蓄力值
     */
    private val chargeCache = ConcurrentHashMap<UUID, Float>()

    /** NMS 方法缓存 */
    private var nmsMethod: java.lang.reflect.Method? = null
    private var nmsHasParam = false
    private var nmsResolved = false

    fun getCachedCharge(uuid: UUID): Float? = chargeCache[uuid]

    fun cleanupCharge(uuid: UUID) {
        chargeCache.remove(uuid)
    }

    /**
     * 监听客户端挥手数据包，在冷却重置前缓存蓄力值
     * PacketPlayInArmAnimation 在 Netty 线程触发，早于主线程处理
     */
    @SubscribeEvent
    fun onPacketReceive(e: taboolib.module.nms.PacketReceiveEvent) {
        if (e.packet.name != "PacketPlayInArmAnimation") return
        val value = getPlayerCharge(e.player)
        if (value != null) {
            chargeCache[e.player.uniqueId] = value
        }
    }

    private fun getPlayerCharge(player: Player): Float? {
        try {
            return player.attackCooldown
        } catch (_: NoSuchMethodError) {}
        // NMS fallback for 1.9~1.14
        try {
            val handle = player.javaClass.getMethod("getHandle").invoke(player)
            if (!nmsResolved) {
                nmsResolved = true
                for (name in arrayOf("n", "s", "dG", "dH", "getAttackCooldown", "w")) {
                    try {
                        nmsMethod = handle.javaClass.getMethod(name, Float::class.javaPrimitiveType)
                        nmsHasParam = true
                        break
                    } catch (_: NoSuchMethodException) {}
                }
                if (nmsMethod == null) {
                    for (name in arrayOf("cW", "dv", "getAttackStrengthScale")) {
                        try {
                            nmsMethod = handle.javaClass.getMethod(name)
                            nmsHasParam = false
                            break
                        } catch (_: NoSuchMethodException) {}
                    }
                }
            }
            val result = if (nmsHasParam) nmsMethod?.invoke(handle, 0.5f) else nmsMethod?.invoke(handle)
            return result as? Float
        } catch (_: Exception) {
            return null
        }
    }

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
