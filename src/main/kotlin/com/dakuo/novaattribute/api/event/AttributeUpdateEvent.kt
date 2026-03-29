package com.dakuo.novaattribute.api.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class AttributeUpdateEvent(
    val player: Player,
    val cause: RefreshCause,
    val before: Map<String, Double>,
    val after: Map<String, Double>
) : BukkitProxyEvent()
