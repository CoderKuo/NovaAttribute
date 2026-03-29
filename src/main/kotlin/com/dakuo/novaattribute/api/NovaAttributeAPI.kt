package com.dakuo.novaattribute.api

import com.dakuo.novaattribute.core.attribute.*
import com.dakuo.novaattribute.api.event.RefreshCause
import com.dakuo.novaattribute.core.buff.Buff
import com.dakuo.novaattribute.core.buff.BuffManager
import com.dakuo.novaattribute.core.cooldown.CooldownManager
import com.dakuo.novaattribute.core.counter.CounterManager
import com.dakuo.novaattribute.core.reader.ItemAttributeReader
import com.dakuo.novaattribute.feature.lore.LoreGenerator
import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.novaattribute.trigger.EventTrigger
import com.dakuo.novaattribute.trigger.TriggerManager
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import taboolib.platform.BukkitPlugin

object NovaAttributeAPI {

    // ====== 来源管理 ======

    fun updateSource(entity: LivingEntity, source: String, data: AttributeData) {
        AttributeManager.get(entity).update(source, data)
    }

    fun updateSource(entity: LivingEntity, source: String, item: ItemStack) {
        val data = ItemAttributeReader.read(item)
        if (!data.isEmpty()) {
            AttributeManager.get(entity).update(source, data)
        }
    }

    fun updateSource(entity: LivingEntity, namespace: String, items: Map<String, ItemStack>) {
        val map = AttributeManager.get(entity)
        for ((key, item) in items) {
            val data = ItemAttributeReader.read(item)
            if (!data.isEmpty()) {
                map.update("$namespace:$key", data)
            }
        }
    }

    fun removeSource(entity: LivingEntity, source: String) {
        AttributeManager.get(entity).remove(source)
    }

    fun removeByNamespace(entity: LivingEntity, namespace: String) {
        AttributeManager.get(entity).removeByNamespace(namespace)
    }

    // ====== 属性查询 ======

    fun getAttribute(entity: LivingEntity, attributeId: String): Double {
        return AttributeManager.get(entity).get(attributeId)
    }

    fun getAttributes(entity: LivingEntity): Map<String, Double> {
        return AttributeManager.get(entity).getAll()
    }

    fun getAttributeRandom(entity: LivingEntity, attributeId: String): Double {
        return AttributeManager.get(entity).getRandom(attributeId)
    }

    fun refresh(entity: LivingEntity, cause: RefreshCause = RefreshCause.API_CALL) {
        AttributeManager.refresh(entity, cause)
    }

    // ====== Buff ======

    fun addBuff(entity: LivingEntity, buff: Buff): Boolean {
        return BuffManager.add(entity, buff)
    }

    fun removeBuff(entity: LivingEntity, buffId: String) {
        BuffManager.remove(entity, buffId)
    }

    fun hasBuff(entity: LivingEntity, buffId: String): Boolean {
        return BuffManager.has(entity, buffId)
    }

    fun getBuffs(entity: LivingEntity): List<Buff> {
        return BuffManager.getAll(entity)
    }

    fun getBuffRemaining(entity: LivingEntity, buffId: String): Long {
        return BuffManager.getRemaining(entity, buffId)
    }

    fun getBuffStacks(entity: LivingEntity, buffId: String): Int {
        return BuffManager.getStacks(entity, buffId)
    }

    // ====== 冷却 ======

    fun setCooldown(entity: LivingEntity, key: String, ticks: Long) {
        CooldownManager.set(entity, key, ticks)
    }

    fun isCooldownReady(entity: LivingEntity, key: String): Boolean {
        return CooldownManager.isReady(entity, key)
    }

    fun getCooldownRemaining(entity: LivingEntity, key: String): Long {
        return CooldownManager.getRemaining(entity, key)
    }

    // ====== 计数器 ======

    fun incrementCounter(entity: LivingEntity, key: String, amount: Int = 1): Int {
        return CounterManager.increment(entity, key, amount)
    }

    fun getCounter(entity: LivingEntity, key: String): Int {
        return CounterManager.get(entity, key)
    }

    fun resetCounter(entity: LivingEntity, key: String) {
        CounterManager.reset(entity, key)
    }

    // ====== Provider ======

    fun registerProvider(provider: AttributeProvider) {
        AttributeManager.registerProvider(provider)
    }

    fun unregisterProvider(id: String) {
        AttributeManager.unregisterProvider(id)
    }

    // ====== 属性注册 ======

    fun registerAttribute(attr: Attribute) {
        AttributeRegistry.register(attr)
    }

    fun unregisterAttribute(id: String) {
        AttributeRegistry.unregister(id)
    }

    // ====== 战斗力 ======

    fun getCombatPower(entity: LivingEntity): Double {
        // 优先使用脚本计算
        if (ScriptBridge.isLoaded("combat_power") && ScriptBridge.hasFunction("combat_power", "calculate")) {
            try {
                val result = ScriptBridge.callFunction("combat_power", "calculate", entity)
                val power = (result as? Number)?.toDouble()
                if (power != null && power >= 0) return power
            } catch (_: Exception) {}
        }

        // 回退：使用属性系数线性计算
        val map = AttributeManager.get(entity)
        var power = 0.0
        for ((attrId, value) in map.getAll()) {
            val attr = AttributeRegistry.get(attrId) ?: continue
            if (attr.combatPower != 0.0) {
                power += value * attr.combatPower
            }
        }
        return power
    }

    // ====== Lore 生成 ======

    fun rebuildLore(item: ItemStack): ItemStack {
        return LoreGenerator.rebuild(item)
    }

    // ====== CUSTOM 触发器 ======

    /**
     * 代码触发模式: 直接触发某个 CUSTOM 属性的脚本
     * @param entity 属性持有者
     * @param attributeId CUSTOM 类型的属性 ID
     * @param target 可选目标
     * @param params 额外参数（传递给脚本）
     */
    fun triggerCustomAttribute(
        entity: LivingEntity,
        attributeId: String,
        target: LivingEntity? = null,
        params: Map<String, Any> = emptyMap()
    ) {
        TriggerManager.triggerCustomAttribute(entity, attributeId, target, params)
    }

    /**
     * 注册事件触发器（其他插件将 Bukkit 事件绑定到属性触发流程）
     */
    fun <E : Event> registerTrigger(trigger: EventTrigger<E>) {
        TriggerManager.register(trigger)
    }

    /**
     * 注销事件触发器
     */
    fun unregisterTrigger(name: String) {
        TriggerManager.unregister(name)
    }

    // ====== 血量条缩放 ======

    /**
     * 设置玩家血量条缩放值
     * @param player 玩家
     * @param scale 缩放值（20.0 = 10颗心）
     */
    fun setHealthScale(player: Player, scale: Double) {
        com.dakuo.novaattribute.realizer.HealthScaleManager.setScale(player, scale)
    }

    // ====== 伤害控制 ======

    fun attackTo(attacker: LivingEntity, victim: LivingEntity, damage: Double) {
        victim.setMetadata("nova:bypass", FixedMetadataValue(BukkitPlugin.getInstance(), true))
        victim.damage(damage, attacker)
    }
}
