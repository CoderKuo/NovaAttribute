package com.dakuo.novaattribute.feature.papi

import com.dakuo.novaattribute.api.NovaAttributeAPI
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.buff.BuffManager
import com.dakuo.novaattribute.core.counter.CounterManager
import com.dakuo.novaattribute.util.NumberFormatter
import org.bukkit.entity.Player
import taboolib.platform.compat.PlaceholderExpansion

object NovaPlaceholder : PlaceholderExpansion {

    override val identifier: String = "novaattribute"

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        player ?: return ""
        val map = AttributeManager.get(player)

        return when {
            // %nova_combat_power%
            args == "combat_power" -> {
                NumberFormatter.format(NovaAttributeAPI.getCombatPower(player))
            }

            // %nova_buff_count%
            args == "buff_count" -> {
                BuffManager.getAll(player).size.toString()
            }

            // %nova_buff_list%
            args == "buff_list" -> {
                BuffManager.getAll(player).joinToString(", ") { it.id }
            }

            // %nova_source_count%
            args == "source_count" -> {
                map.sourceCount().toString()
            }

            // %nova_base_<attrId>%
            args.startsWith("base_") -> {
                val attrId = args.removePrefix("base_")
                formatValue(attrId, map.getBase(attrId))
            }

            // %nova_min_<attrId>%
            args.startsWith("min_") -> {
                val attrId = args.removePrefix("min_")
                formatValue(attrId, map.getMin(attrId))
            }

            // %nova_max_<attrId>%
            args.startsWith("max_") -> {
                val attrId = args.removePrefix("max_")
                formatValue(attrId, map.getMax(attrId))
            }

            // %nova_buff_<id>_remaining%
            args.startsWith("buff_") && args.endsWith("_remaining") -> {
                val buffId = args.removePrefix("buff_").removeSuffix("_remaining")
                val remaining = BuffManager.getRemaining(player, buffId)
                "${remaining / 20}s"
            }

            // %nova_buff_<id>_stacks%
            args.startsWith("buff_") && args.endsWith("_stacks") -> {
                val buffId = args.removePrefix("buff_").removeSuffix("_stacks")
                BuffManager.getStacks(player, buffId).toString()
            }

            // %nova_counter_<key>%
            args.startsWith("counter_") -> {
                val key = args.removePrefix("counter_")
                CounterManager.get(player, key).toString()
            }

            // %nova_<attrId>% — 直接属性查询
            else -> {
                AttributeRegistry.get(args) ?: return ""
                formatValue(args, map.get(args))
            }
        }
    }

    private fun formatValue(attrId: String, value: Double): String {
        val attr = AttributeRegistry.get(attrId)
        // 百分比属性（loreDivisor != 1.0 的属性通常是百分比类）
        if (attr != null && attr.loreDivisor != 1.0) {
            return "%.1f%%".format(value * 100)
        }
        return NumberFormatter.format(value)
    }
}
