package com.dakuo.novaattribute.core.cooldown

import org.bukkit.entity.LivingEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CooldownManager {

    // UUID -> (key -> 过期时间戳)
    private val cooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    // 记录哪些 key 需要持久化
    private val persistentKeys = ConcurrentHashMap<UUID, MutableSet<String>>()

    fun set(entity: LivingEntity, key: String, ticks: Long, persistent: Boolean = false) {
        val expireAt = System.currentTimeMillis() + ticks * 50L
        cooldowns.getOrPut(entity.uniqueId) { ConcurrentHashMap() }[key] = expireAt
        if (persistent) {
            persistentKeys.getOrPut(entity.uniqueId) { ConcurrentHashMap.newKeySet() }.add(key)
        }
    }

    /**
     * 从持久化数据恢复冷却（直接设置过期时间戳）
     */
    fun restore(uuid: UUID, key: String, expireAt: Long) {
        cooldowns.getOrPut(uuid) { ConcurrentHashMap() }[key] = expireAt
        persistentKeys.getOrPut(uuid) { ConcurrentHashMap.newKeySet() }.add(key)
    }

    /**
     * 获取需要持久化的冷却数据
     */
    fun getPersistent(uuid: UUID): Map<String, Long> {
        val pKeys = persistentKeys[uuid] ?: return emptyMap()
        val map = cooldowns[uuid] ?: return emptyMap()
        val now = System.currentTimeMillis()
        return pKeys.mapNotNull { key ->
            val expireAt = map[key]
            if (expireAt != null && expireAt > now) key to expireAt else null
        }.toMap()
    }

    fun isReady(entity: LivingEntity, key: String): Boolean {
        val map = cooldowns[entity.uniqueId] ?: return true
        val expireAt = map[key] ?: return true
        if (System.currentTimeMillis() >= expireAt) {
            map.remove(key)
            return true
        }
        return false
    }

    fun getRemaining(entity: LivingEntity, key: String): Long {
        val map = cooldowns[entity.uniqueId] ?: return 0
        val expireAt = map[key] ?: return 0
        val remaining = expireAt - System.currentTimeMillis()
        return if (remaining > 0) remaining / 50L else 0
    }

    fun reset(entity: LivingEntity, key: String) {
        cooldowns[entity.uniqueId]?.remove(key)
    }

    fun cleanup(entity: LivingEntity) {
        cooldowns.remove(entity.uniqueId)
        persistentKeys.remove(entity.uniqueId)
    }

    fun cleanupAll() {
        cooldowns.clear()
        persistentKeys.clear()
    }
}
