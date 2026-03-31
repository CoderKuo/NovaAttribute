package com.dakuo.novaattribute.realizer

import com.dakuo.novaattribute.api.event.VanillaAttributeSyncEvent
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.rulib.common.debug
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.module.configuration.Configuration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 原版属性同步器
 *
 * 将 NovaAttribute 计算值同步到 Bukkit 原版属性系统（AttributeModifier）
 * 使游戏实际效果（血量上限、移动速度等）与属性计算结果一致
 */
object VanillaSync {

    /** 固定 UUID，确保修饰符可被精确移除而不堆积 */
    private val MODIFIER_UUID = UUID.nameUUIDFromBytes("NovaAttribute".toByteArray())

    private var enabled = true

    /**
     * 属性映射定义
     * @param novaId NovaAttribute 属性 ID
     * @param bukkit Bukkit 原版属性枚举
     * @param base Bukkit 原版基础值
     * @param compute 计算修饰符值的函数 (novaValue, base) -> modifierValue
     */
    private data class SyncMapping(
        val novaId: String,
        val bukkit: Attribute,
        val base: Double,
        val compute: (Double, Double) -> Double
    )

    private val mappings = listOf(
        // max_health: NovaAttribute 算出总血量(如100)，Bukkit 基础 20，modifier = value - 20
        SyncMapping("max_health", Attribute.GENERIC_MAX_HEALTH, 20.0) { value, base -> value - base },
        // movement_speed: NovaAttribute 存百分比(如0.3=30%)，Bukkit 基础 0.1，modifier = base * value
        SyncMapping("movement_speed", Attribute.GENERIC_MOVEMENT_SPEED, 0.1) { value, base -> base * value },
        // attack_speed: NovaAttribute 存百分比(如0.5=50%)，Bukkit 基础 4.0，modifier = base * value
        SyncMapping("attack_speed", Attribute.GENERIC_ATTACK_SPEED, 4.0) { value, base -> base * value },
        // knockback_resistance: NovaAttribute 存百分比(如0.5=50%)，Bukkit 0~1 直接用
        SyncMapping("knockback_resistance", Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0.0) { value, _ -> value }
    )

    /** 值缓存：entityUUID -> (novaAttrId -> lastSyncedModifierValue) */
    private val cache = ConcurrentHashMap<UUID, MutableMap<String, Double>>()

    fun init(config: Configuration) {
        enabled = config.getBoolean("vanilla-sync.enabled", true)
        if (enabled) {
            info("[NovaAttribute] Vanilla attribute sync enabled.")
        }
    }

    /**
     * 同步实体的属性到 Bukkit 原版
     * 在 AttributeManager.refresh() 末尾调用
     */
    fun sync(entity: LivingEntity) {
        if (!enabled) return

        val uuid = entity.uniqueId
        val map = AttributeManager.getOrNull(entity) ?: return
        val entityCache = cache.getOrPut(uuid) { mutableMapOf() }

        for (mapping in mappings) {
            val novaValue = map.get(mapping.novaId)
            val modifierValue = mapping.compute(novaValue, mapping.base)

            // 值没变就跳过
            val cached = entityCache[mapping.novaId]
            if (cached != null && cached == modifierValue) continue
            entityCache[mapping.novaId] = modifierValue

            val instance = entity.getAttribute(mapping.bukkit) ?: continue

            // 首次同步时清理其他插件残留的数据
            if (cached == null) {
                // 重置基础值为原版默认（清理旧插件 setBaseValue 残留）
                instance.baseValue = mapping.base
                // 移除其他插件残留的 modifier
                for (mod in instance.modifiers.toList()) {
                    if (mod.uniqueId != MODIFIER_UUID) {
                        try { instance.removeModifier(mod) } catch (_: Exception) {}
                    }
                }
            }

            // max_health 特殊处理：同步前记录血量比例
            val healthRatio = if (mapping.novaId == "max_health" && entity.maxHealth > 0) {
                entity.health / entity.maxHealth
            } else {
                -1.0
            }

            // 移除旧修饰符 + 添加新修饰符
            val modifier = AttributeModifier(
                MODIFIER_UUID,
                "NovaAttr-${mapping.novaId}",
                modifierValue,
                AttributeModifier.Operation.ADD_NUMBER
            )
            try {
                instance.removeModifier(modifier)
            } catch (_: Exception) {
            }
            instance.addModifier(modifier)

            debug("[VanillaSync] {entity} {attr}: modifier={value}",
                "entity" to entity.name,
                "attr" to mapping.novaId,
                "value" to modifierValue
            )

            // 抛出同步事件
            VanillaAttributeSyncEvent(entity, mapping.novaId, modifierValue).call()

            // max_health：同步后恢复血量比例
            if (healthRatio >= 0 && entity.maxHealth > 0) {
                val newHealth = (entity.maxHealth * healthRatio).coerceIn(1.0, entity.maxHealth)
                entity.health = newHealth
            }
        }
    }

    /**
     * 清理实体缓存（玩家退出时调用）
     */
    fun cleanup(uuid: UUID) {
        cache.remove(uuid)
    }

    /**
     * 移除所有实体的修饰符并恢复原版状态（插件关闭时调用）
     */
    fun unsyncAll() {
        if (!enabled) return

        for (player in Bukkit.getOnlinePlayers()) {
            unsync(player)
        }
        cache.clear()
    }

    private fun unsync(entity: LivingEntity) {
        for (mapping in mappings) {
            val instance = entity.getAttribute(mapping.bukkit) ?: continue
            val modifier = AttributeModifier(
                MODIFIER_UUID,
                "NovaAttr-${mapping.novaId}",
                0.0,
                AttributeModifier.Operation.ADD_NUMBER
            )
            try {
                instance.removeModifier(modifier)
            } catch (_: Exception) {
            }
        }

        // max_health 移除后恢复满血
        if (entity is Player) {
            entity.health = entity.maxHealth.coerceAtLeast(1.0)
        }
    }
}
