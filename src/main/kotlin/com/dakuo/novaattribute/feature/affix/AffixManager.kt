package com.dakuo.novaattribute.feature.affix

import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.novascript.NovaCompiled
import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Configuration
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * 属性词条系统
 * 检测物品 Lore 中的特定文本，匹配成功时附加一整套属性
 * 支持两种模式：
 *   1. attributes 模式：静态属性映射 + 表达式求值
 *   2. script 模式：脚本动态决定属性
 */
object AffixManager {

    data class Affix(
        val id: String,
        val type: MatchType,
        val pattern: String,
        val regex: Pattern?,
        val loreValues: Boolean,
        val scriptName: String?,
        val attributes: Map<String, String>
    )

    enum class MatchType { CONTAINS, REGEX }

    /**
     * 供脚本调用的属性写入器
     * 脚本通过 result.set("属性ID", 值) 写入属性
     * lore-values 模式下自动除以 loreDivisor
     */
    class AffixResult(private val loreValues: Boolean) {
        val data = AttributeData()

        fun set(attrId: String, value: Any) {
            val raw = (value as? Number)?.toDouble() ?: return
            val finalValue = if (loreValues) {
                val divisor = AttributeRegistry.get(attrId)?.loreDivisor ?: 1.0
                raw / divisor
            } else {
                raw
            }
            val existing = data.getFirst(attrId)
            data.set(attrId, existing + finalValue)
        }
    }

    private data class CachedExpr(val compiled: NovaCompiled, val paramCount: Int)

    private val affixes = mutableListOf<Affix>()
    private val exprCache = ConcurrentHashMap<String, CachedExpr>()

    fun init(dataFolder: File) {
        affixes.clear()
        exprCache.clear()
        val file = File(dataFolder, "affixes.yml")
        if (!file.exists()) return
        val config = Configuration.loadFromFile(file)

        val section = config.getConfigurationSection("affixes") ?: return
        for (id in section.getKeys(false)) {
            val affixSection = section.getConfigurationSection(id) ?: continue
            val typeStr = affixSection.getString("type", "contains")!!
            val matchType = if (typeStr.equals("regex", ignoreCase = true)) MatchType.REGEX else MatchType.CONTAINS
            val matchStr = affixSection.getString("match") ?: continue
            val loreValues = affixSection.getBoolean("lore-values", true)
            val scriptName = affixSection.getString("script")

            val regex = if (matchType == MatchType.REGEX) {
                try {
                    Pattern.compile(matchStr)
                } catch (e: Exception) {
                    warning("[NovaAttribute] Invalid regex in affix '$id': ${e.message}")
                    continue
                }
            } else null

            // attributes 模式
            val attributes = mutableMapOf<String, String>()
            if (scriptName == null) {
                val attrSection = affixSection.getConfigurationSection("attributes")
                if (attrSection != null) {
                    for (attrId in attrSection.getKeys(false)) {
                        val rawValue = attrSection.get(attrId) ?: continue
                        attributes[attrId] = rawValue.toString()
                    }
                }
                if (attributes.isEmpty()) continue
            }

            affixes.add(Affix(id, matchType, matchStr, regex, loreValues, scriptName, attributes))
        }
        if (affixes.isNotEmpty()) {
            info("[NovaAttribute] Loaded ${affixes.size} affixes.")
        }
    }

    /**
     * 匹配物品 Lore 中的所有词条，返回汇总属性
     */
    fun match(item: ItemStack): AttributeData {
        val data = AttributeData()
        if (affixes.isEmpty()) return data

        val meta = item.itemMeta ?: return data
        val lore = meta.lore ?: return data

        for (line in lore) {
            val stripped = ChatColor.stripColor(line) ?: line
            for (affix in affixes) {
                when (affix.type) {
                    MatchType.CONTAINS -> {
                        if (stripped.contains(affix.pattern)) {
                            applyAffix(data, affix, emptyList())
                        }
                    }
                    MatchType.REGEX -> {
                        val matcher = affix.regex!!.matcher(stripped)
                        if (matcher.find()) {
                            val groups = (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
                            applyAffix(data, affix, groups)
                        }
                    }
                }
            }
        }

        return data
    }

    private fun applyAffix(data: AttributeData, affix: Affix, groups: List<String>) {
        if (affix.scriptName != null) {
            applyScript(data, affix, groups)
        } else {
            applyAttributes(data, affix, groups)
        }
    }

    /**
     * script 模式：调用脚本的 resolve(result, groups) 函数
     */
    private fun applyScript(data: AttributeData, affix: Affix, groups: List<String>) {
        if (!ScriptBridge.isLoaded(affix.scriptName!!)) {
            warning("[NovaAttribute] Affix '${affix.id}' script '${affix.scriptName}' not loaded")
            return
        }
        try {
            val result = AffixResult(affix.loreValues)
            ScriptBridge.callFunction(affix.scriptName, "resolve", result, groups)
            if (!result.data.isEmpty()) {
                data.merge(result.data)
            }
        } catch (e: Exception) {
            warning("[NovaAttribute] Affix '${affix.id}' script error: ${e.message}")
        }
    }

    /**
     * attributes 模式：静态属性映射 + 表达式求值
     */
    private fun applyAttributes(data: AttributeData, affix: Affix, groups: List<String>) {
        for ((attrId, expression) in affix.attributes) {
            val rawValue = evaluateExpression(expression, groups)
            if (rawValue == 0.0) continue

            val value = if (affix.loreValues) {
                val divisor = AttributeRegistry.get(attrId)?.loreDivisor ?: 1.0
                rawValue / divisor
            } else {
                rawValue
            }

            val existing = data.getFirst(attrId)
            data.set(attrId, existing + value)
        }
    }

    /**
     * 计算表达式，支持任意 NovaScript 表达式
     * 编译缓存: 将 {N} 替换为参数名编译一次，后续只传参调用
     */
    private fun evaluateExpression(template: String, groups: List<String>): Double {
        val trimmed = template.trim()
        // 纯数字快速路径
        trimmed.toDoubleOrNull()?.let { return it }
        // 有捕获组引用，传入实际值
        val args = if (trimmed.contains("{")) {
            groups.map { it.toDoubleOrNull() ?: 0.0 }
        } else {
            emptyList()
        }
        return evalCached(trimmed, args)
    }

    private fun evalCached(template: String, args: List<Double>): Double {
        return try {
            val cached = exprCache.getOrPut(template) {
                val maxGroup = Regex("\\{(\\d+)}").findAll(template)
                    .map { it.groupValues[1].toInt() }
                    .maxOrNull() ?: 0
                var code = template
                for (i in 1..maxGroup) {
                    code = code.replace("{$i}", "v$i")
                }
                val params = (1..maxGroup).joinToString(", ") { "v$it" }
                val compiled = ScriptBridge.compile("fun eval($params) { return $code }")
                CachedExpr(compiled, maxGroup)
            }
            val paddedArgs = Array<Any>(cached.paramCount) { i ->
                if (i < args.size) args[i] as Any else 0.0 as Any
            }
            val result = cached.compiled.call("eval", *paddedArgs)
            (result as? Number)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            warning("[NovaAttribute] Affix expression eval failed: '$template' → ${e.message}")
            0.0
        }
    }
}
