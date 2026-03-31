package com.dakuo.novaattribute.core.condition

import com.dakuo.novaattribute.script.ScriptBridge
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.configuration.Configuration
import taboolib.module.nms.ItemTagType
import taboolib.module.nms.getItemTag

/**
 * 装备条件检查（DESIGN.md 4.15）
 *
 * 两种条件来源：
 * 1. NBT: NovaCondition 键存储条件 map
 * 2. Lore: 匹配 lore-patterns 中配置的关键词提取条件
 *
 * 两种来源会合并后传给条件脚本统一判定
 */
object ConditionChecker {

    private const val CONDITION_KEY = "NovaCondition"
    private const val SCRIPT_NAME = "condition_default"

    private var enabled = true
    private val lorePatterns = mutableMapOf<String, String>()

    fun init(config: Configuration) {
        enabled = config.getBoolean("condition.enabled", true)

        // 加载 Lore 条件关键词映射
        lorePatterns.clear()
        val section = config.getConfigurationSection("condition.lore-patterns")
        if (section != null) {
            for (key in section.getKeys(false)) {
                lorePatterns[key] = section.getString(key) ?: ""
            }
        }
        // 默认关键词（配置中未定义时使用）
        if (lorePatterns.isEmpty()) {
            lorePatterns["level"] = "需要等级|等级限制|Lv\\.|Level"
            lorePatterns["class"] = "限制职业|需要职业|职业限制"
            lorePatterns["permission"] = "需要权限"
        }
    }

    fun check(player: Player, item: ItemStack, source: String): Boolean {
        if (!enabled) return true

        // 合并 NBT + Lore 条件
        val nbtConditions = readConditionsFromNBT(item) ?: emptyMap()
        val loreConditions = readConditionsFromLore(item)
        val conditions = mutableMapOf<String, Any>()
        conditions.putAll(loreConditions)
        conditions.putAll(nbtConditions) // NBT 优先覆盖 Lore

        if (conditions.isEmpty()) return true
        if (!ScriptBridge.isLoaded(SCRIPT_NAME)) return true

        return try {
            val result = ScriptBridge.callFunction(SCRIPT_NAME, "check", player, conditions, item, source)
            result as? Boolean ?: true
        } catch (e: Exception) {
            taboolib.common.platform.function.warning("[NovaAttribute] Condition script error: ${e.message}")
            true
        }
    }

    /**
     * 从 NBT 读取条件 map
     */
    private fun readConditionsFromNBT(item: ItemStack): Map<String, Any>? {
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

    /**
     * 从 Lore 读取条件
     *
     * Lore 格式示例:
     *   §7需要等级: 50
     *   §7限制职业: 战士/法师
     *   §7需要权限: vip.sword
     */
    private fun readConditionsFromLore(item: ItemStack): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val meta = item.itemMeta ?: return result
        val lore = meta.lore ?: return result

        for (line in lore) {
            val stripped = ChatColor.stripColor(line)?.trim() ?: continue
            for ((condKey, patternStr) in lorePatterns) {
                val patterns = patternStr.split("|")
                for (pattern in patterns) {
                    val regex = Regex("(?:$pattern)[：:\\s]*(.*)")
                    val match = regex.find(stripped) ?: continue
                    val value = match.groupValues[1].trim()
                    if (value.isNotEmpty()) {
                        // 尝试转数字，失败则存字符串
                        val numValue = value.toDoubleOrNull()
                        result[condKey] = numValue ?: value
                    }
                    break
                }
            }
        }
        return result
    }
}
