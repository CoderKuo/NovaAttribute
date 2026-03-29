package com.dakuo.novaattribute.util

import taboolib.module.configuration.Configuration
import kotlin.math.abs

/**
 * 全局大数值格式化 & 解析工具
 * 单位从 config.yml 的 number-format.units 加载，服主可自由扩展
 */
object NumberFormatter {

    private var abbreviationEnabled = true
    private var decimalPlaces = 1

    // 格式化用：按倍数从大到小排列
    private val formatUnits = mutableListOf<Pair<String, Double>>()
    // 解析用：按后缀长度从长到短排列（万亿 优先于 万）
    private val parseUnits = mutableListOf<Pair<String, Double>>()

    fun init(config: Configuration) {
        abbreviationEnabled = config.getBoolean("number-format.enabled", false)
        decimalPlaces = config.getInt("number-format.decimal-places", 1)

        formatUnits.clear()
        parseUnits.clear()

        val section = config.getConfigurationSection("number-format.units")
        if (section != null) {
            for (key in section.getKeys(false)) {
                val multiplier = section.getDouble(key)
                if (multiplier > 0) {
                    formatUnits.add(key to multiplier)
                    parseUnits.add(key to multiplier)
                }
            }
        }

        formatUnits.sortByDescending { it.second }
        parseUnits.sortByDescending { it.first.length }
    }

    fun format(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

        if (!abbreviationEnabled || formatUnits.isEmpty()) {
            return formatRaw(value)
        }

        val absValue = abs(value)
        for ((suffix, multiplier) in formatUnits) {
            if (absValue >= multiplier) {
                val result = value / multiplier
                return "%.${decimalPlaces}f$suffix".format(result)
            }
        }

        return formatRaw(value)
    }

    /**
     * 解析可能带单位后缀的数字字符串
     * 支持所有在 config.yml 中配置的 units 后缀
     * 英文后缀大小写不敏感
     */
    fun parse(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // 纯数字直接解析
        trimmed.toDoubleOrNull()?.let { return it }

        // 按后缀长度从长到短尝试匹配
        for ((suffix, multiplier) in parseUnits) {
            if (trimmed.endsWith(suffix, ignoreCase = true)) {
                val numPart = trimmed.dropLast(suffix.length).toDoubleOrNull() ?: continue
                return numPart * multiplier
            }
        }
        return null
    }

    /**
     * 保留原始精度：整数不带小数点，浮点保留有效位数
     */
    private fun formatRaw(value: Double): String {
        return if (value == value.toLong().toDouble() && abs(value) < Long.MAX_VALUE) {
            value.toLong().toString()
        } else {
            "%.${decimalPlaces}f".format(value).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * 安全化数值：将 NaN/Infinity 转为 0，防止数值污染
     */
    fun sanitize(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return value
    }
}
