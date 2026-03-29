package com.dakuo.novaattribute.api.event

import com.dakuo.novaattribute.core.buff.Buff
import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

class BuffRemoveEvent(
    val entity: LivingEntity,
    val buff: Buff,
    val cause: BuffRemoveCause
) : BukkitProxyEvent()

enum class BuffRemoveCause {
    EXPIRED,
    API_CALL,
    REPLACED,
    DEATH
}
