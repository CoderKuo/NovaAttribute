package com.dakuo.novaattribute.api.event

import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

class AttributeChangeEvent(
    val entity: LivingEntity,
    val attributeId: String,
    val oldValue: Double,
    val newValue: Double
) : BukkitProxyEvent()
