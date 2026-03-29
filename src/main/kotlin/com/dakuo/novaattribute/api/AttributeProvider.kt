package com.dakuo.novaattribute.api

import com.dakuo.novaattribute.core.attribute.AttributeBundle
import org.bukkit.entity.Player

interface AttributeProvider {
    val id: String
    fun provide(player: Player): AttributeBundle
}
