package com.dakuo.novaattribute.core.attribute

data class Attribute(
    val id: String,
    val name: String,
    val default: Double = 0.0,
    val range: Boolean = false,
    val lorePattern: String? = null,
    val loreDivisor: Double = 1.0,
    val trigger: AttributeTrigger = AttributeTrigger.PASSIVE,
    val triggerEvent: String? = null,
    val script: String? = null,
    val priority: Int = 0,
    val combatPower: Double = 0.0,
    val interval: Long = 20L,
    val messages: AttributeMessages? = null
)

enum class AttributeTrigger {
    PASSIVE,
    ATTACK,
    DEFENSE,
    KILL,
    PERIODIC,
    CUSTOM
}

data class AttributeMessages(
    val attacker: String? = null,
    val victim: String? = null,
    val self: String? = null
)
