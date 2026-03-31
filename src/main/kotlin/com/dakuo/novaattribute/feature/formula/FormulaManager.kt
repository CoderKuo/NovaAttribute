package com.dakuo.novaattribute.feature.formula

import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.novascript.NovaCompiled
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Configuration
import taboolib.platform.compat.replacePlaceholder
import java.io.File

/**
 * 公式属性管理器
 *
 * 从 formulas.yml 读取属性公式，每次属性刷新时自动计算并注入。
 * 公式支持 PAPI 占位符和完整的 NovaLang 脚本语法。
 *
 * 简单公式（单行表达式）:
 *   max_health: "20 + %player_level% * 5"
 *
 * 复杂公式（多行脚本，必须返回数值）:
 *   physical_damage: |
 *     var level = toNumber(placeholder(entity, "%player_level%"))
 *     var classBonus = 0
 *     if (placeholder(entity, "%class_name%") == "warrior") {
 *         classBonus = level * 3
 *     }
 *     return 1 + level * 2 + classBonus
 */
object FormulaManager {

    private data class Formula(
        val attrId: String,
        val expression: String,
        val compiled: NovaCompiled?
    )

    private val formulas = mutableListOf<Formula>()
    private var enabled = false

    fun init(dataFolder: File) {
        formulas.clear()
        val file = File(dataFolder, "formulas.yml")
        if (!file.exists()) {
            enabled = false
            return
        }
        val config = Configuration.loadFromFile(file)
        val section = config.getConfigurationSection("formulas") ?: return

        for (attrId in section.getKeys(false)) {
            val expr = section.getString(attrId) ?: continue
            if (expr.isBlank()) continue

            // 编译为字节码（支持复杂脚本）
            val compiled = try {
                ScriptBridge.compile("fun calculate(entity) {\n$expr\n}")
            } catch (e: Exception) {
                warning("[NovaAttribute] Formula compile error for '$attrId': ${e.message}")
                null
            }
            formulas.add(Formula(attrId, expr, compiled))
        }

        enabled = formulas.isNotEmpty()
        if (enabled) {
            info("[NovaAttribute] Loaded ${formulas.size} attribute formulas.")
        }
    }

    fun isEnabled(): Boolean = enabled

    /**
     * 计算公式并注入到实体的属性来源
     * 在 AttributeManager.refresh() 的 Provider 阶段之后调用
     */
    fun apply(entity: LivingEntity) {
        if (!enabled || entity !is Player) return

        val data = AttributeData()
        for (formula in formulas) {
            val compiled = formula.compiled ?: continue
            try {
                // 替换 PAPI 占位符后执行脚本
                val result = compiled.call("calculate", entity)
                val value = (result as? Number)?.toDouble() ?: continue
                if (value != 0.0) {
                    data.set(formula.attrId, value)
                }
            } catch (e: Exception) {
                warning("[NovaAttribute] Formula error for '${formula.attrId}': ${e.message}")
            }
        }

        if (!data.isEmpty()) {
            AttributeManager.get(entity).update("formula:__combined__", data)
        } else {
            AttributeManager.get(entity).remove("formula:__combined__")
        }
    }

    fun reload(dataFolder: File) {
        init(dataFolder)
    }
}
