package com.dakuo.novaattribute.feature.mob

import org.bukkit.ChatColor
import org.bukkit.entity.LivingEntity
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 怪物元素弱点系统
 * 配置了弱点的怪物只受指定元素类型的伤害
 */
object MobElementManager {

    // 实体类型 → 有效伤害元素列表
    private val typeWeakness = mutableMapOf<String, List<String>>()
    // 自定义名称(去色) → 有效伤害元素列表
    private val nameWeakness = mutableMapOf<String, List<String>>()

    fun init(dataFolder: File) {
        typeWeakness.clear()
        nameWeakness.clear()

        val file = File(dataFolder, "mob-elements.yml")
        if (!file.exists()) return
        val config = Configuration.loadFromFile(file)

        val typesSection = config.getConfigurationSection("types")
        if (typesSection != null) {
            for (key in typesSection.getKeys(false)) {
                val list = typesSection.getStringList(key)
                if (list.isNotEmpty()) {
                    typeWeakness[key.uppercase()] = list.map { it.lowercase() }
                }
            }
        }

        val namesSection = config.getConfigurationSection("names")
        if (namesSection != null) {
            for (key in namesSection.getKeys(false)) {
                val list = namesSection.getStringList(key)
                if (list.isNotEmpty()) {
                    nameWeakness[key] = list.map { it.lowercase() }
                }
            }
        }
    }

    /**
     * 获取怪物的弱点元素列表
     * @return 有效伤害类型列表，null 表示受所有伤害
     */
    fun getWeakness(entity: LivingEntity): List<String>? {
        // 优先匹配自定义名称
        val customName = entity.customName
        if (customName != null) {
            val stripped = ChatColor.stripColor(customName) ?: customName
            nameWeakness[stripped]?.let { return it }
        }
        // 再匹配实体类型
        return typeWeakness[entity.type.name]
    }
}
