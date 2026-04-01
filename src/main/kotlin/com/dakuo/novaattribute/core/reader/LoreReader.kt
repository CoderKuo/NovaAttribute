package com.dakuo.novaattribute.core.reader

import com.dakuo.novaattribute.core.attribute.Attribute
import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.condition.InlineConditionChecker
import com.dakuo.novaattribute.util.NumberFormatter
import com.dakuo.rulib.common.regex.Regex as RuRegex
import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack
import taboolib.module.configuration.Configuration

object LoreReader {

    private var globalEnabled = true
    private var separator = Regex("[：:] ?")

    // 两段式匹配: +100(+10%) 或 (+100)(+10%)
    private val TWO_PART = Regex("^(.*?)\\(([^)]*%)\\)$")

    // 按显示名长度降序排列，长名称优先匹配（"物理攻击力" 优先于 "物理攻击"）
    private var nameLookup: List<Pair<String, Attribute>> = emptyList()

    fun init(config: Configuration) {
        globalEnabled = config.getBoolean("lore-format.enabled", true)
        val sep = config.getString("lore-format.separator") ?: "[：:] ?"
        separator = Regex(sep)
    }

    /**
     * 根据已注册属性的显示名构建查找表
     * 需在 AttributeRegistry 加载后调用
     */
    fun rebuildLookup() {
        nameLookup = AttributeRegistry.getAll().values.mapNotNull { attr ->
            val stripped = ChatColor.stripColor(attr.name) ?: return@mapNotNull null
            stripped to attr
        }.sortedByDescending { it.first.length }
    }

    fun read(item: ItemStack): AttributeData {
        val result = AttributeData()
        val meta = item.itemMeta ?: return result
        val lore = meta.lore ?: return result

        val customAttrs = AttributeRegistry.getAll().values.filter { it.lorePattern != null }

        val sep = InlineConditionChecker.loreSeparator

        for (line in lore) {
            val stripped = ChatColor.stripColor(line) ?: continue

            // 分离内嵌条件: "暴击率: +25% / 等级>=50" → valuePart="暴击率: +25%", condition="等级>=50"
            val sepIdx = stripped.indexOf(sep)
            val valuePart: String
            val condition: String?
            if (sepIdx > 0) {
                valuePart = stripped.substring(0, sepIdx).trim()
                condition = stripped.substring(sepIdx + sep.length).trim()
            } else {
                valuePart = stripped
                condition = null
            }

            // 1. 优先使用属性自定义 lore-pattern
            if (tryCustomPattern(valuePart, customAttrs, result, condition)) continue

            // 2. 全局按属性显示名匹配
            if (globalEnabled) {
                tryGlobalMatch(valuePart, result, condition)
            }
        }
        return result
    }

    /**
     * 从文本行列表中读取属性（供 MythicMobs 等外部系统使用）
     * 文本格式与物品 Lore 一致，如 "物理攻击: 100"
     */
    fun readLines(lines: List<String>): AttributeData? {
        if (lines.isEmpty()) return null
        val result = AttributeData()
        val customAttrs = AttributeRegistry.getAll().values.filter { it.lorePattern != null }
        val sep = InlineConditionChecker.loreSeparator

        for (line in lines) {
            val stripped = ChatColor.stripColor(line) ?: continue
            val sepIdx = stripped.indexOf(sep)
            val valuePart: String
            val condition: String?
            if (sepIdx > 0) {
                valuePart = stripped.substring(0, sepIdx).trim()
                condition = stripped.substring(sepIdx + sep.length).trim()
            } else {
                valuePart = stripped
                condition = null
            }
            if (tryCustomPattern(valuePart, customAttrs, result, condition)) continue
            if (globalEnabled) {
                tryGlobalMatch(valuePart, result, condition)
            }
        }
        return if (result.isEmpty()) null else result
    }

    // ====== 自定义正则匹配（原有逻辑）======

    private fun tryCustomPattern(
        stripped: String,
        attrs: Collection<Attribute>,
        result: AttributeData,
        condition: String? = null
    ): Boolean {
        for (attr in attrs) {
            val pattern = attr.lorePattern ?: continue
            val matchResult = try {
                RuRegex.match(pattern, stripped)
            } catch (e: Exception) {
                taboolib.common.platform.function.warning("[NovaAttribute] Lore pattern error for '${attr.id}': ${e.message}")
                continue
            } ?: continue

            val values = mutableListOf<Double>()
            for (i in 1..matchResult.groupCount()) {
                val raw = matchResult.group(i)?.replace(",", "")?.trim() ?: continue
                val num = NumberFormatter.parse(raw) ?: continue
                values.add(num / attr.loreDivisor)
            }
            if (values.isEmpty()) continue

            mergeValues(result, attr.id, values)
            if (condition != null) result.setCondition(attr.id, condition)
            return true
        }
        return false
    }

    // ====== 全局名称匹配 ======

    private fun tryGlobalMatch(stripped: String, result: AttributeData, condition: String? = null) {
        for ((name, attr) in nameLookup) {
            if (!stripped.startsWith(name)) continue
            val rest = stripped.substring(name.length)
            // 分隔符必须紧跟属性名，防止 "物理攻击力" 匹配到 "物理攻击"
            val sepMatch = separator.find(rest) ?: continue
            if (sepMatch.range.first != 0) continue
            val valueStr = rest.substring(sepMatch.range.last + 1).trim()
            if (valueStr.isEmpty()) continue

            val parsed = parseValueString(valueStr, attr.loreDivisor) ?: continue
            mergeValues(result, attr.id, parsed.values)
            if (parsed.isRange) result.markRange(attr.id)
            if (condition != null) result.setCondition(attr.id, condition)
            return
        }
    }

    private class ParseResult(val values: List<Double>, val isRange: Boolean)

    /**
     * 解析值字符串，支持格式:
     *   100, +100, (+100)           → 固定值
     *   10%, +10%, (+10%)           → 百分比（由 loreDivisor 处理）
     *   100~200, 100-200             → 范围值（-号需前接数字/字母，与负号区分）
     *   +100(+10%), (+100)(+10%)    → 基础值 + 百分比加成
     *   100万, 100亿~200亿           → 带单位后缀
     */
    private fun parseValueString(raw: String, loreDivisor: Double): ParseResult? {
        val s = raw.trim()

        // 两段式: +100(+10%) 或 (+100)(+10%) 或 (+10%)
        val twoPart = TWO_PART.find(s)
        if (twoPart != null) {
            val basePart = twoPart.groupValues[1].trim()
            val pctPart = twoPart.groupValues[2].trim()
            val pct = parseSingleValue(pctPart)
            if (pct != null) {
                val base = if (basePart.isEmpty()) 0.0 else parseSingleValue(basePart)
                if (base != null) {
                    return ParseResult(listOf(base / loreDivisor, pct / 100.0), false)
                }
            }
        }

        // 范围值: 100~200 或 100-200
        val range = tryParseRange(s)
        if (range != null) {
            return ParseResult(listOf(range.first / loreDivisor, range.second / loreDivisor), true)
        }

        // 百分比修饰符: +10%, (+10%) 等
        // 仅当属性本身不是百分比属性时（loreDivisor==1），% 表示乘算加成存入 values[1]
        // 百分比属性（loreDivisor!=1，如暴击几率）的 % 是装饰性的，由 loreDivisor 处理
        if (loreDivisor == 1.0 && isPercentModifier(s)) {
            val value = parseSingleValue(s)
            if (value != null) {
                return ParseResult(listOf(0.0, value / 100.0), false)
            }
        }

        // 简单值
        val value = parseSingleValue(s)
        if (value != null) {
            return ParseResult(listOf(value / loreDivisor), false)
        }

        return null
    }

    /**
     * 尝试解析范围值，优先 ~ 其次 -
     * - 区分负号：只有前接数字、字母、右括号的 - 才视为范围分隔符
     */
    private fun tryParseRange(s: String): Pair<Double, Double>? {
        // 优先 ~
        val tildeIdx = s.indexOf('~')
        if (tildeIdx > 0) {
            val v1 = parseSingleValue(s.substring(0, tildeIdx).trim())
            val v2 = parseSingleValue(s.substring(tildeIdx + 1).trim())
            if (v1 != null && v2 != null) return v1 to v2
        }
        // 其次 -（前一个字符必须是数字/字母/右括号，排除负号）
        for (i in 1 until s.length) {
            if (s[i] == '-' && s[i - 1].let { it.isDigit() || it.isLetter() || it == ')' }) {
                val v1 = parseSingleValue(s.substring(0, i).trim())
                val v2 = parseSingleValue(s.substring(i + 1).trim())
                if (v1 != null && v2 != null) return v1 to v2
            }
        }
        return null
    }

    /**
     * 检测值字符串是否为百分比修饰符格式
     */
    private fun isPercentModifier(s: String): Boolean {
        val t = s.trim()
        // 10%, +10%, -5%
        if (t.endsWith("%")) return true
        // (+10%), (-5%)
        if (t.endsWith("%)") && t.contains("(")) return true
        return false
    }

    /**
     * 解析单个数值，自动剥离括号、+号、%号、千分位逗号
     */
    private fun parseSingleValue(raw: String): Double? {
        var s = raw.trim()
        if (s.isEmpty()) return null
        // 剥离外层括号
        if (s.startsWith("(") && s.endsWith(")")) {
            s = s.substring(1, s.length - 1).trim()
        }
        // 剥离 % 后缀
        s = s.removeSuffix("%").trim()
        // 剥离 + 前缀（保留 - 表示负数）
        if (s.startsWith("+")) s = s.substring(1).trim()
        // 剥离千分位逗号: 1,000,000 → 1000000
        s = s.replace(",", "")
        // NumberFormatter.parse 处理单位后缀（万/亿/K/M 等）
        return NumberFormatter.parse(s)
    }

    // ====== 公共工具 ======

    private fun mergeValues(result: AttributeData, attrId: String, values: List<Double>) {
        val existing = result.get(attrId)
        if (existing != null) {
            // 统一为相同长度再相加
            // 固定值 [10000] 合并到范围值 [1, 2] 时，展开为 [10000, 10000]
            // 范围值 [1, 2] 合并到固定值 [10000] 时，展开为 [10000, 10000]
            val targetLen = maxOf(existing.size, values.size)
            val a = expandToLength(existing, targetLen)
            val b = expandToLength(values, targetLen)
            val merged = mutableListOf<Double>()
            for (i in 0 until targetLen) {
                merged.add(a[i] + b[i])
            }
            result.set(attrId, merged)
            // 任一方是范围值就标记
            if (targetLen > 1) result.markRange(attrId)
        } else {
            result.set(attrId, values)
        }
    }

    /**
     * 将值列表展开到指定长度
     * 单值 [100] 展开为 [100, 100]（固定值同时作为 min 和 max）
     */
    private fun expandToLength(values: List<Double>, targetLen: Int): List<Double> {
        if (values.size >= targetLen) return values
        val result = values.toMutableList()
        val fill = values.last() // 用最后一个值填充
        while (result.size < targetLen) {
            result.add(fill)
        }
        return result
    }
}
