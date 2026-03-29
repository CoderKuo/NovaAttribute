package com.dakuo.novaattribute.api.event

import com.dakuo.novaattribute.combat.DamageContext
import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

class NovaAttributeDamageEvent(
    val attacker: LivingEntity,
    val victim: LivingEntity,
    val context: DamageContext,
    var finalDamage: Double
) : BukkitProxyEvent()
