package com.dakuo.novaattribute.core.condition

import com.dakuo.novaattribute.script.ScriptBridge
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.configuration.Configuration
import taboolib.module.nms.ItemTagType
import taboolib.module.nms.getItemTag

/**
 * 装备条件检查（DESIGN.md 4.15）
 * 从 NBT NovaCondition 键读取条件 map，传给条件脚本判定
 */
object ConditionChecker {

    private const val CONDITION_KEY = "NovaCondition"
    private const val SCRIPT_NAME = "condition_default"

    private var enabled = true

    fun init(config: Configuration) {
        enabled = config.getBoolean("condition.enabled", true)
    }

    /**
     * 检查玩家是否满足物品的装备条件
     * @param player 玩家
     * @param item 物品
     * @param source 来源标识（如 "equipment:mainhand"）
     * @return true=条件满足或无条件, false=条件不满足
     */
    fun check(player: Player, item: ItemStack, source: String): Boolean {
        if (!enabled) return true

        val conditions = readConditions(item) ?: return true
        if (conditions.isEmpty()) return true

        // 检查脚本是否已加载
        if (!ScriptBridge.isLoaded(SCRIPT_NAME)) return true

        return try {
            val result = ScriptBridge.callFunction(SCRIPT_NAME, "check", player, conditions, item, source)
            result as? Boolean ?: true
        } catch (_: Exception) {
            true
        }
    }

    /**
     * 从 NBT 读取条件 map
     * NBT 格式: NovaCondition: { level: 50, class: "warrior/mage", source: "equipment:mainhand" }
     */
    private fun readConditions(item: ItemStack): Map<String, Any>? {
        val tag = try {
            item.getItemTag()
        } catch (_: Exception) {
            return null
        }

        val condTag = tag[CONDITION_KEY] ?: return null
        if (condTag.type != ItemTagType.COMPOUND) return null

        val compound = condTag.asCompound()
        val result = mutableMapOf<String, Any>()
        for (key in compound.keys) {
            val entry = compound[key] ?: continue
            when (entry.type) {
                ItemTagType.STRING -> result[key] = entry.asString()
                ItemTagType.INT -> result[key] = entry.asInt()
                ItemTagType.DOUBLE -> result[key] = entry.asDouble()
                ItemTagType.FLOAT -> result[key] = entry.asFloat().toDouble()
                ItemTagType.LONG -> result[key] = entry.asLong()
                else -> result[key] = entry.asString()
            }
        }
        return result
    }
}
