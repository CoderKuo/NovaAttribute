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

    private const val SNAPSHOT_KEY = "nova:attr_snapshot"

    /**
     * 获取弹射物上快照的属性值（如果有）
     */
    fun getProjectileSnapshot(projectile: org.bukkit.entity.Entity): Map<String, Double>? {
        val meta = projectile.getMetadata(SNAPSHOT_KEY)
        @Suppress("UNCHECKED_CAST")
        return meta.firstOrNull()?.value() as? Map<String, Double>
    }

    @SubscribeEvent
    fun onShootBow(e: EntityShootBowEvent) {
        val shooter = e.entity as? LivingEntity ?: return
        val map = AttributeManager.getOrNull(shooter) ?: return

        // 快照射击时的属性到弹射物上（防止切换武器后属性变化）
        val snapshot = map.getAll()
        e.projectile.setMetadata(SNAPSHOT_KEY,
            org.bukkit.metadata.FixedMetadataValue(taboolib.platform.BukkitPlugin.getInstance(), snapshot))

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
