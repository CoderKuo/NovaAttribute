package com.dakuo.novaattribute.core.attribute

import com.dakuo.novaattribute.api.AttributeProvider
import com.dakuo.novaattribute.api.event.AttributeChangeEvent
import com.dakuo.novaattribute.api.event.AttributeRefreshEvent
import com.dakuo.novaattribute.api.event.AttributeUpdateEvent
import com.dakuo.novaattribute.api.event.RefreshCause
import com.dakuo.novaattribute.core.buff.BuffManager
import com.dakuo.novaattribute.realizer.PeriodicScheduler
import com.dakuo.novaattribute.core.condition.ConditionChecker
import com.dakuo.novaattribute.realizer.VanillaSync
import com.dakuo.novaattribute.core.cooldown.CooldownManager
import com.dakuo.novaattribute.core.counter.CounterManager
import com.dakuo.novaattribute.core.reader.ItemAttributeReader
import com.dakuo.novaattribute.feature.storage.StorageManager
import com.dakuo.novaattribute.util.Mirror
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import java.util.concurrent.ConcurrentHashMap

object AttributeManager {

    private val maps = ConcurrentHashMap<java.util.UUID, AttributeMap>()
    private val providers = ConcurrentHashMap<String, AttributeProvider>()

    fun get(entity: LivingEntity): AttributeMap {
        return maps.getOrPut(entity.uniqueId) { AttributeMap(entity) }
    }

    fun getOrNull(entity: LivingEntity): AttributeMap? {
        return maps[entity.uniqueId]
    }

    fun remove(entity: LivingEntity) {
        maps.remove(entity.uniqueId)?.cleanup()
    }

    // ====== Provider 管理 ======

    fun registerProvider(provider: AttributeProvider) {
        providers[provider.id] = provider
    }

    fun unregisterProvider(id: String) {
        providers.remove(id)
    }

    // ====== 防抖 ======

    /** 上次刷新时间戳（entityUUID -> nanoTime） */
    private val lastRefreshTime = ConcurrentHashMap<java.util.UUID, Long>()

    /** 防抖间隔（纳秒），50ms 内同一实体不重复刷新 */
    private const val DEBOUNCE_NS = 50_000_000L

    // ====== 刷新流程 ======

    fun refresh(entity: LivingEntity, cause: RefreshCause = RefreshCause.API_CALL) {
        // 防抖：短时间内不重复刷新同一实体
        val now = System.nanoTime()
        val last = lastRefreshTime[entity.uniqueId]
        if (last != null && (now - last) < DEBOUNCE_NS && cause != RefreshCause.RELOAD) {
            return
        }
        lastRefreshTime[entity.uniqueId] = now

        val map = get(entity)
        val before = map.getAll()

        // ① 如果是玩家，读取装备属性
        if (entity is Player) {
            Mirror.time("refresh:equipment") { readEquipment(entity, map) }
        }

        // ② 抛出 AttributeRefreshEvent (PRE)
        if (entity is Player) {
            val event = AttributeRefreshEvent(entity, cause, emptyMap())
            event.call()
        }

        // ③ 调用所有 Provider
        if (entity is Player) {
            for (provider in providers.values) {
                try {
                    val bundle = provider.provide(entity)
                    // 解析 Provider 返回的物品
                    for ((key, item) in bundle.getItems()) {
                        val data = ItemAttributeReader.read(item)
                        if (!data.isEmpty()) {
                            map.update("${provider.id}:$key", data)
                        }
                    }
                    // 直接数据来源
                    for ((key, data) in bundle.getSources()) {
                        map.update("${provider.id}:$key", data)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // ③.5 计算公式属性
        if (entity is Player) {
            try {
                com.dakuo.novaattribute.feature.formula.FormulaManager.apply(entity)
            } catch (e: Exception) {
                taboolib.common.platform.function.warning("[NovaAttribute] Formula apply error: ${e.message}")
            }
        }

        // ④ 合并 Buff 属性
        val buffData = BuffManager.getCombinedData(entity)
        if (!buffData.isEmpty()) {
            map.update("buff:__combined__", buffData)
        } else {
            map.remove("buff:__combined__")
        }

        // ⑤ 重算属性
        Mirror.time("refresh:recalculate") { map.recalculate() }

        val after = map.getAll()

        // ⑥ 抛出 AttributeUpdateEvent (POST)
        if (entity is Player) {
            AttributeUpdateEvent(entity, cause, before, after).call()
        }

        // ⑦ 逐属性对比，分发 AttributeChangeEvent
        for (attrId in (before.keys + after.keys)) {
            val oldVal = before[attrId] ?: 0.0
            val newVal = after[attrId] ?: 0.0
            if (oldVal != newVal) {
                AttributeChangeEvent(entity, attrId, oldVal, newVal).call()
            }
        }

        // ⑧ 同步原版属性（max_health, movement_speed 等）
        VanillaSync.sync(entity)
    }

    private fun readEquipment(player: Player, map: AttributeMap) {
        // 主手
        val mainHand = player.inventory.itemInMainHand
        readEquipmentSlot(player, map, mainHand, "equipment:mainhand")

        // 副手
        val offHand = player.inventory.itemInOffHand
        readEquipmentSlot(player, map, offHand, "equipment:offhand")

        // 盔甲
        val armorSlots = mapOf(
            "helmet" to player.inventory.helmet,
            "chestplate" to player.inventory.chestplate,
            "leggings" to player.inventory.leggings,
            "boots" to player.inventory.boots
        )
        for ((slot, item) in armorSlots) {
            readEquipmentSlot(player, map, item, "equipment:$slot")
        }
    }

    private fun readEquipmentSlot(player: Player, map: AttributeMap, item: org.bukkit.inventory.ItemStack?, source: String) {
        if (item == null || item.amount == 0 || item.type.name.endsWith("AIR")) {
            map.remove(source)
            return
        }
        // 装备条件检查
        if (!ConditionChecker.check(player, item, source)) {
            map.remove(source)
            return
        }
        val data = ItemAttributeReader.read(item)
        if (!data.isEmpty()) {
            map.update(source, data)
        } else {
            map.remove(source)
        }
    }

    // ====== 生命周期事件 ======

    @SubscribeEvent
    fun onJoin(e: PlayerJoinEvent) {
        if (StorageManager.isEnabled()) {
            val uuid = e.player.uniqueId
            submit(async = true) {
                val buffs = StorageManager.loadBuffs(uuid)
                val cooldowns = StorageManager.loadCooldowns(uuid)
                val counters = StorageManager.loadCounters(uuid)
                submit {
                    // 主线程恢复数据
                    for (buff in buffs) {
                        BuffManager.add(e.player, buff)
                    }
                    for ((key, expireAt) in cooldowns) {
                        CooldownManager.restore(uuid, key, expireAt)
                    }
                    for ((key, value) in counters) {
                        CounterManager.restore(e.player, key, value)
                    }
                    refresh(e.player, RefreshCause.PLAYER_JOIN)
                }
            }
        } else {
            refresh(e.player, RefreshCause.PLAYER_JOIN)
        }
    }

    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        if (StorageManager.isEnabled()) {
            // 在清理前捕获持久化数据
            val uuid = e.player.uniqueId
            val persistentBuffs = BuffManager.getAll(e.player).filter { it.persistent }
            val persistentCooldowns = CooldownManager.getPersistent(uuid)
            val persistentCounters = CounterManager.getPersistent(uuid)
            submit(async = true) {
                StorageManager.saveBuffs(uuid, persistentBuffs)
                StorageManager.saveCooldowns(uuid, persistentCooldowns)
                StorageManager.saveCounters(uuid, persistentCounters)
            }
        }
        lastRefreshTime.remove(e.player.uniqueId)
        VanillaSync.cleanup(e.player.uniqueId)
        PeriodicScheduler.cleanup(e.player.uniqueId)
        BuffManager.cleanup(e.player)
        CooldownManager.cleanup(e.player)
        CounterManager.cleanup(e.player)
        remove(e.player)
    }

    @SubscribeEvent
    fun onEntityDeath(e: EntityDeathEvent) {
        val entity = e.entity
        if (entity !is Player) {
            BuffManager.cleanup(entity)
            remove(entity)
        }
    }

    fun cleanup() {
        maps.values.forEach { it.cleanup() }
        maps.clear()
        providers.clear()
        lastRefreshTime.clear()
    }
}
