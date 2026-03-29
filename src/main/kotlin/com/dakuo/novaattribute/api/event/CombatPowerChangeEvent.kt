package com.dakuo.novaattribute.api.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class CombatPowerChangeEvent(
    val player: Player,
    val oldPower: Double,
    val newPower: Double
) : BukkitProxyEvent()
