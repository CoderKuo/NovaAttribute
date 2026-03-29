package com.dakuo.novaattribute.core.counter

import com.dakuo.novaattribute.api.event.CounterChangeEvent
import org.bukkit.entity.LivingEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CounterManager {

    // UUID -> (key -> count)
    private val counters = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Int>>()
    // 记录哪些 key 需要持久化
    private val persistentKeys = ConcurrentHashMap<UUID, MutableSet<String>>()

    fun increment(entity: LivingEntity, key: String, amount: Int = 1): Int {
        val map = counters.getOrPut(entity.uniqueId) { ConcurrentHashMap() }
        val oldValue = map.getOrDefault(key, 0)
        val newValue = oldValue + amount
        map[key] = newValue
        CounterChangeEvent(entity, key, oldValue, newValue).call()
        return newValue
    }

    fun get(entity: LivingEntity, key: String): Int {
        return counters[entity.uniqueId]?.getOrDefault(key, 0) ?: 0
    }

    fun set(entity: LivingEntity, key: String, value: Int) {
        val map = counters.getOrPut(entity.uniqueId) { ConcurrentHashMap() }
        val oldValue = map.getOrDefault(key, 0)
        map[key] = value
        if (oldValue != value) {
            CounterChangeEvent(entity, key, oldValue, value).call()
        }
    }

    fun reset(entity: LivingEntity, key: String) {
        val map = counters[entity.uniqueId] ?: return
        val oldValue = map.remove(key) ?: return
        if (oldValue != 0) {
            CounterChangeEvent(entity, key, oldValue, 0).call()
        }
    }

    /**
     * 从持久化数据恢复计数器
     */
    fun restore(entity: LivingEntity, key: String, value: Int) {
        counters.getOrPut(entity.uniqueId) { ConcurrentHashMap() }[key] = value
        persistentKeys.getOrPut(entity.uniqueId) { ConcurrentHashMap.newKeySet() }.add(key)
    }

    /**
     * 标记计数器为持久化
     */
    fun markPersistent(entity: LivingEntity, key: String) {
        persistentKeys.getOrPut(entity.uniqueId) { ConcurrentHashMap.newKeySet() }.add(key)
    }

    /**
     * 获取需要持久化的计数器数据
     */
    fun getPersistent(uuid: UUID): Map<String, Int> {
        val pKeys = persistentKeys[uuid] ?: return emptyMap()
        val map = counters[uuid] ?: return emptyMap()
        return pKeys.mapNotNull { key ->
            val value = map[key]
            if (value != null) key to value else null
        }.toMap()
    }

    fun cleanup(entity: LivingEntity) {
        counters.remove(entity.uniqueId)
        persistentKeys.remove(entity.uniqueId)
    }

    fun cleanupAll() {
        counters.clear()
        persistentKeys.clear()
    }
}
