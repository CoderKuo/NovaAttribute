package com.dakuo.novaattribute.core.attribute

import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Configuration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object AttributeRegistry {

    private val attributes = ConcurrentHashMap<String, Attribute>()

    fun register(attr: Attribute) {
        attributes[attr.id] = attr
    }

    fun unregister(id: String) {
        attributes.remove(id)
    }

    fun get(id: String): Attribute? = attributes[id]

    fun getAll(): Map<String, Attribute> = attributes.toMap()

    fun getByTrigger(trigger: AttributeTrigger): List<Attribute> {
        return attributes.values
            .filter { it.trigger == trigger }
            .sortedBy { it.priority }
    }

    fun loadFromConfig(config: Configuration) {
        val section = config.getConfigurationSection("attributes") ?: return
        for (id in section.getKeys(false)) {
            val attrSection = section.getConfigurationSection(id) ?: continue
            val trigger = try {
                AttributeTrigger.valueOf(attrSection.getString("trigger", "PASSIVE")!!.uppercase())
            } catch (e: Exception) {
                AttributeTrigger.PASSIVE
            }
            val messages = if (attrSection.contains("messages")) {
                AttributeMessages(
                    attacker = attrSection.getString("messages.attacker"),
                    victim = attrSection.getString("messages.victim"),
                    self = attrSection.getString("messages.self")
                )
            } else null

            val attr = Attribute(
                id = id,
                name = attrSection.getString("name", id)!!,
                default = attrSection.getDouble("default", 0.0),
                range = attrSection.getBoolean("range", false),
                lorePattern = attrSection.getString("lore-pattern"),
                loreDivisor = attrSection.getDouble("lore-divisor", 1.0),
                trigger = trigger,
                triggerEvent = attrSection.getString("trigger-event"),
                script = attrSection.getString("script"),
                priority = attrSection.getInt("priority", 0),
                combatPower = attrSection.getDouble("combat-power", 0.0),
                interval = attrSection.getLong("interval", 20L),
                messages = messages
            )
            register(attr)
        }
    }

    fun loadFromDirectory(dir: File) {
        if (!dir.exists()) return
        dir.listFiles { f -> f.extension == "yml" }?.forEach { file ->
            val config = Configuration.loadFromFile(file)
            loadFromConfig(config)
        }
        info("[NovaAttribute] Loaded ${attributes.size} attributes.")
    }

    fun reload(dir: File) {
        attributes.clear()
        loadFromDirectory(dir)
    }

    fun size(): Int = attributes.size
}
