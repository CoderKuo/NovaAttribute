package com.dakuo.novaattribute.core.attribute

import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.novaattribute.util.NumberFormatter

/**
 * 属性汇总辅助类
 * 将所有来源的同一属性数值交给 combine 脚本计算
 */
object CombineHelper {

    fun combine(attrId: String, allValues: List<List<Double>>, defaultValue: Double): Double {
        val result = try {
            val raw = ScriptBridge.callFunction(
                "combine", "combine",
                attrId, allValues, defaultValue
            )
            (raw as? Number)?.toDouble() ?: defaultValue
        } catch (e: Exception) {
            // 脚本未加载或执行失败时使用默认汇总：简单求和第一段数值
            fallbackCombine(allValues, defaultValue)
        }
        return NumberFormatter.sanitize(result)
    }

    private fun fallbackCombine(allValues: List<List<Double>>, defaultValue: Double): Double {
        if (allValues.isEmpty()) return defaultValue
        // 默认汇总逻辑：
        // 第一段(index 0) = 加算，全部累加
        // 第二段(index 1) = 乘算百分比，全部累加后 (1 + sum)
        var additive = 0.0
        var multiplicative = 0.0
        for (values in allValues) {
            additive += values.getOrElse(0) { 0.0 }
            multiplicative += values.getOrElse(1) { 0.0 }
        }
        val base = defaultValue + additive
        return if (multiplicative != 0.0) {
            base * (1.0 + multiplicative)
        } else {
            base
        }
    }
}
