package com.dakuo.novaattribute.api.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class AttributeRefreshEvent(
    val player: Player,
    val cause: RefreshCause,
    val additions: Map<String, Any>
) : BukkitProxyEvent()
