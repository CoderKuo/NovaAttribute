package com.dakuo.novaattribute.listener

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.rulib.common.debug
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityShootBowEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 战斗机制监听器
 * 处理弓箭发射、蓄力缓存等需要事件钩子的机制
 */
object MechanicsListener {

    /**
     * 蓄力值缓存
     * Minecraft 在伤害事件之前就重置了攻击冷却，所以需要每 tick 记录，
     * 攻击时读取的是上一 tick 的值（即攻击前的真实蓄力值）
     */
    private val chargeCache = ConcurrentHashMap<UUID, Float>()
    private var chargeTask: taboolib.common.platform.service.PlatformExecutor.PlatformTask? = null

    /** NMS 方法缓存 */
    private var nmsMethod: java.lang.reflect.Method? = null
    private var nmsHasParam = false
    private var nmsResolved = false

    fun getCachedCharge(uuid: UUID): Float? = chargeCache[uuid]

    fun startChargeTracker() {
        chargeTask?.cancel()
        chargeTask = taboolib.common.platform.function.submit(period = 1L) {
            for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
                val value = getPlayerCharge(player)
                if (value != null) {
                    chargeCache[player.uniqueId] = value
                }
            }
        }
    }

    fun stopChargeTracker() {
        chargeTask?.cancel()
        chargeTask = null
        chargeCache.clear()
    }

    fun cleanupCharge(uuid: UUID) {
        chargeCache.remove(uuid)
    }

    private fun getPlayerCharge(player: Player): Float? {
        try {
            return player.attackCooldown
        } catch (_: NoSuchMethodError) {}
        // NMS fallback
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
