package com.dakuo.novaattribute.core.buff

import com.dakuo.novaattribute.core.attribute.AttributeData
import org.bukkit.entity.LivingEntity

data class Buff(
    val id: String,
    val data: AttributeData,
    val duration: Long,
    val stackable: Boolean = false,
    val persistent: Boolean = false,
    val expireAt: Long,
    var stacks: Int = 1,
    val source: LivingEntity? = null
) {
    fun isExpired(): Boolean {
        if (duration < 0) return false
        return System.currentTimeMillis() >= expireAt
    }

    fun getRemaining(): Long {
        if (duration < 0) return -1
        val remaining = expireAt - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }
}
