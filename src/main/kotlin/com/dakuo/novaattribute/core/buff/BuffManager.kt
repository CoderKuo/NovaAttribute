package com.dakuo.novaattribute.core.buff

import com.dakuo.novaattribute.api.event.BuffAddEvent
import com.dakuo.novaattribute.api.event.BuffRemoveCause
import com.dakuo.novaattribute.api.event.BuffRemoveEvent
import com.dakuo.novaattribute.api.event.BuffStackEvent
import com.dakuo.novaattribute.api.event.RefreshCause
import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.attribute.AttributeManager
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BuffManager {

    private val buffs = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Buff>>()
    private var tickTask: PlatformExecutor.PlatformTask? = null

    fun add(entity: LivingEntity, buff: Buff): Boolean {
        val event = BuffAddEvent(entity, buff)
        event.call()
        if (event.isCancelled) return false

        val map = buffs.getOrPut(entity.uniqueId) { ConcurrentHashMap() }
        val existing = map[buff.id]

        if (existing != null && buff.stackable) {
            val oldStacks = existing.stacks
            existing.stacks++
            BuffStackEvent(entity, existing, oldStacks, existing.stacks).call()
        } else {
            map[buff.id] = buff
        }

        AttributeManager.refresh(entity, RefreshCause.BUFF_CHANGE)
        return true
    }

    fun remove(entity: LivingEntity, buffId: String, cause: BuffRemoveCause = BuffRemoveCause.API_CALL) {
        val map = buffs[entity.uniqueId] ?: return
        val buff = map.remove(buffId) ?: return
        BuffRemoveEvent(entity, buff, cause).call()
        AttributeManager.refresh(entity, RefreshCause.BUFF_CHANGE)
    }

    fun has(entity: LivingEntity, buffId: String): Boolean {
        return buffs[entity.uniqueId]?.containsKey(buffId) == true
    }

    fun getAll(entity: LivingEntity): List<Buff> {
        return buffs[entity.uniqueId]?.values?.toList() ?: emptyList()
    }

    fun getRemaining(entity: LivingEntity, buffId: String): Long {
        val buff = buffs[entity.uniqueId]?.get(buffId) ?: return 0
        return buff.getRemaining()
    }

    fun getStacks(entity: LivingEntity, buffId: String): Int {
        return buffs[entity.uniqueId]?.get(buffId)?.stacks ?: 0
    }

    fun getCombinedData(entity: LivingEntity): AttributeData {
        val result = AttributeData()
        val map = buffs[entity.uniqueId] ?: return result
        for (buff in map.values) {
            result.merge(buff.data)
        }
        return result
    }

    fun cleanup(entity: LivingEntity) {
        val map = buffs.remove(entity.uniqueId) ?: return
        for (buff in map.values) {
            BuffRemoveEvent(entity, buff, BuffRemoveCause.DEATH).call()
        }
    }

    fun startTickTask() {
        tickTask = submit(period = 20L) {
            for ((uuid, map) in buffs) {
                val expired = map.values.filter { it.isExpired() }
                if (expired.isEmpty()) continue

                for (buff in expired) {
                    map.remove(buff.id)
                    val entity = org.bukkit.Bukkit.getServer().getEntity(uuid) as? LivingEntity
                    if (entity != null) {
                        BuffRemoveEvent(entity, buff, BuffRemoveCause.EXPIRED).call()
                    }
                }

                val entity = org.bukkit.Bukkit.getServer().getEntity(uuid) as? LivingEntity
                if (entity != null) {
                    AttributeManager.refresh(entity, RefreshCause.BUFF_CHANGE)
                }

                if (map.isEmpty()) {
                    buffs.remove(uuid)
                }
            }
        }
    }

    fun stopTickTask() {
        tickTask?.cancel()
        tickTask = null
    }

    fun cleanupAll() {
        stopTickTask()
        buffs.clear()
    }
}
