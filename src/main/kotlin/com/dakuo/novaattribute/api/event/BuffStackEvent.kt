package com.dakuo.novaattribute.api.event

import com.dakuo.novaattribute.core.buff.Buff
import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

class BuffStackEvent(
    val entity: LivingEntity,
    val buff: Buff,
    val oldStacks: Int,
    val newStacks: Int
) : BukkitProxyEvent()
