package com.dakuo.novaattribute.combat

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent

class DamageContext(
    val attacker: LivingEntity,
    val victim: LivingEntity,
    val cause: EntityDamageEvent.DamageCause,
    val originalDamage: Double,
    var finalDamage: Double,
    val isProjectile: Boolean = false,
    val properties: MutableMap<String, Any> = mutableMapOf()
) {
    fun getProperty(key: String): Any? = properties[key]

    fun setProperty(key: String, value: Any) {
        properties[key] = value
    }

    // 供脚本调用（参数用 Number 兼容 NovaScript 传入的各种数值类型）
    fun getDamage(): Double = finalDamage
    fun setDamage(value: Number) { finalDamage = value.toDouble() }
    fun isProjectileHit(): Boolean = isProjectile
}
