package com.dakuo.novaattribute.feature.lore

import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.reader.NbtReader
import org.bukkit.inventory.ItemStack
import taboolib.module.configuration.Configuration
import java.text.DecimalFormat

/**
 * Lore 自动生成（DESIGN.md 4.10）
 * 根据 NBT 属性数据自动生成展示 Lore
 */
object LoreGenerator {

    private var enabled = false
    private var markerStart = "§k§r§n§o§v§a§s"
    private var markerEnd = "§k§r§n§o§v§a§e"
    private var header = listOf<String>()
    private var footer = listOf<String>()
    private var formatDefault = "§7{name}: §f{value}"
    private var formatRange = "§7{name}: §f{min}~{max}"
    private var formatPercent = "§7{name}: §f{value}%"
    private var formatPercentBonus = "§7{name}: §a+{percent}%"
    private var formatTwoPart = "§7{name}: §f{base}§7(§a+{percent}%§7)"
    private var sortOrder = listOf<String>()
    private var showZero = false
    private var decimalFormat = DecimalFormat("#.#")

    fun init(config: Configuration) {
        enabled = true
        markerStart = config.getString("marker.start") ?: markerStart
        markerEnd = config.getString("marker.end") ?: markerEnd
        header = config.getStringList("header")
        footer = config.getStringList("footer")
        formatDefault = config.getString("format.default") ?: formatDefault
        formatRange = config.getString("format.range") ?: formatRange
        formatPercent = config.getString("format.percent") ?: formatPercent
        formatPercentBonus = config.getString("format.percent-bonus") ?: formatPercentBonus
        formatTwoPart = config.getString("format.two-part") ?: formatTwoPart
        sortOrder = config.getStringList("sort-order")
        showZero = config.getBoolean("show-zero", false)
        val places = config.getInt("decimal-places", 1)
        decimalFormat = DecimalFormat("#.${"#".repeat(places)}")
    }

    fun isEnabled(): Boolean = enabled

    /**
     * 根据 NBT 属性数据生成 Lore 并写入物品
     */
    fun rebuild(item: ItemStack): ItemStack {
        if (!enabled) return item
        val data = NbtReader.read(item)
        val attrLines = generate(data)

        val meta = item.itemMeta ?: return item
        val lore = meta.lore?.toMutableList() ?: mutableListOf()

        replaceLoreSection(lore, attrLines)

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    /**
     * 根据 AttributeData 生成属性 Lore 行
     */
    fun generate(data: AttributeData): List<String> {
        if (data.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()

        // 按排序顺序生成
        val sorted = sortAttributes(data.keys())
        for (attrId in sorted) {
            val values = data.get(attrId) ?: continue
            val attr = AttributeRegistry.get(attrId)
            val displayName = attr?.name ?: attrId

            val line = formatAttribute(attrId, displayName, values, attr?.loreDivisor ?: 1.0)
            if (line != null) {
                lines.add(line)
            }
        }

        return lines
    }

    private fun formatAttribute(attrId: String, name: String, values: List<Double>, loreDivisor: Double): String? {
        if (values.isEmpty()) return null

        val v0 = values[0]
        val v1 = values.getOrNull(1) ?: 0.0

        // 两段式: base + percent
        if (values.size >= 2 && v0 != 0.0 && v1 != 0.0 && loreDivisor == 1.0) {
            if (!showZero && v0 == 0.0 && v1 == 0.0) return null
            return formatTwoPart
                .replace("{name}", name)
                .replace("{base}", fmt(v0))
                .replace("{percent}", fmt(v1 * 100))
        }

        // 纯百分比加成: values=[0, pct]
        if (values.size >= 2 && v0 == 0.0 && v1 != 0.0 && loreDivisor == 1.0) {
            if (!showZero && v1 == 0.0) return null
            return formatPercentBonus
                .replace("{name}", name)
                .replace("{percent}", fmt(v1 * 100))
        }

        // 范围值: [min, max]（当属性标记为 range 且非百分比）
        val attr = AttributeRegistry.get(attrId)
        if (attr != null && attr.range && values.size >= 2 && loreDivisor == 1.0) {
            if (!showZero && v0 == 0.0 && v1 == 0.0) return null
            return formatRange
                .replace("{name}", name)
                .replace("{min}", fmt(v0))
                .replace("{max}", fmt(v1))
        }

        // 百分比属性（loreDivisor != 1）
        if (loreDivisor != 1.0) {
            val display = v0 * loreDivisor
            if (!showZero && display == 0.0) return null
            return formatPercent
                .replace("{name}", name)
                .replace("{value}", fmt(display))
        }

        // 单值
        if (!showZero && v0 == 0.0) return null
        return formatDefault
            .replace("{name}", name)
            .replace("{value}", fmt(v0))
    }

    private fun sortAttributes(attrIds: Set<String>): List<String> {
        val result = mutableListOf<String>()
        // 先按 sortOrder 顺序添加
        for (id in sortOrder) {
            if (id in attrIds) result.add(id)
        }
        // 再添加未在 sortOrder 中的属性
        for (id in attrIds) {
            if (id !in result) result.add(id)
        }
        return result
    }

    /**
     * 在现有 Lore 中找标记区间替换，或追加到末尾
     */
    private fun replaceLoreSection(lore: MutableList<String>, attrLines: List<String>) {
        val fullSection = mutableListOf<String>()
        fullSection.add(markerStart)
        fullSection.addAll(header)
        fullSection.addAll(attrLines)
        fullSection.addAll(footer)
        fullSection.add(markerEnd)

        // 如果属性为空，移除已有标记区段
        if (attrLines.isEmpty()) {
            val startIdx = lore.indexOfFirst { it.contains(markerStart) }
            val endIdx = lore.indexOfFirst { it.contains(markerEnd) }
            if (startIdx >= 0 && endIdx >= startIdx) {
                for (i in endIdx downTo startIdx) lore.removeAt(i)
            }
            return
        }

        // 寻找已有标记区段
        val startIdx = lore.indexOfFirst { it.contains(markerStart) }
        val endIdx = lore.indexOfFirst { it.contains(markerEnd) }

        if (startIdx >= 0 && endIdx >= startIdx) {
            // 替换现有区段
            for (i in endIdx downTo startIdx) lore.removeAt(i)
            lore.addAll(startIdx, fullSection)
        } else {
            // 追加到末尾
            lore.addAll(fullSection)
        }
    }

    private fun fmt(value: Double): String {
        if (value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }
        return decimalFormat.format(value)
    }
}
