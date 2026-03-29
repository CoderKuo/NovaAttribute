package com.dakuo.novaattribute.core.condition

import com.dakuo.novaattribute.script.ScriptBridge
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import taboolib.module.configuration.Configuration

/**
 * 内嵌条件检查（DESIGN.md 4.21）
 * 单属性行级条件，同一物品不同属性可有各自独立的激活条件
 */
object InlineConditionChecker {

    private const val SCRIPT_NAME = "condition_inline"

    private var enabled = true
    var loreSeparator = " / "
        private set

    fun init(config: Configuration) {
        enabled = config.getBoolean("inline-condition.enabled", true)
        loreSeparator = config.getString("inline-condition.lore-separator") ?: " / "
    }

    /**
     * 评估单属性的内嵌条件
     * @param entity 持有者（可能为 null，如展示物品时）
     * @param condition 条件表达式（如 "level>=50"、"class:mage"）
     * @param source 来源标识
     * @param item 物品（可能为 null）
     * @return true=条件满足或关闭, false=条件不满足
     */
    fun check(entity: LivingEntity?, condition: String, source: String = "", item: ItemStack? = null): Boolean {
        if (!enabled) return true
        if (condition.isBlank()) return true

        // 无持有者时默认通过（物品展示/编辑场景）
        if (entity == null) return true

        if (!ScriptBridge.isLoaded(SCRIPT_NAME)) return true

        return try {
            val result = ScriptBridge.callFunction(SCRIPT_NAME, "check", entity, condition, source, item)
            result as? Boolean ?: true
        } catch (_: Exception) {
            true
        }
    }
}
