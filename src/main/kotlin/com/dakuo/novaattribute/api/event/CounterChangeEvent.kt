package com.dakuo.novaattribute.api.event

import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

class CounterChangeEvent(
    val entity: LivingEntity,
    val key: String,
    val oldValue: Int,
    val newValue: Int
) : BukkitProxyEvent()
