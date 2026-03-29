package com.dakuo.novaattribute.api.event

import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

/**
 * 原版属性同步事件
 * 每当 VanillaSync 将某个属性同步到 Bukkit 原版属性系统后触发
 *
 * @param entity 同步的实体
 * @param attributeId NovaAttribute 属性 ID（如 "max_health"）
 * @param value 同步后的修饰符值
 */
class VanillaAttributeSyncEvent(
    val entity: LivingEntity,
    val attributeId: String,
    val value: Double
) : BukkitProxyEvent()
