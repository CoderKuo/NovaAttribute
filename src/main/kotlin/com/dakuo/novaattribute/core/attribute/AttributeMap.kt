package com.dakuo.novaattribute.core.attribute

import com.dakuo.novaattribute.core.condition.InlineConditionChecker
import org.bukkit.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap

class AttributeMap(val entity: LivingEntity) {

    private val sources = ConcurrentHashMap<String, AttributeData>()
    private val cache = ConcurrentHashMap<String, Double>()

    @Volatile
    private var dirty = true

    // ====== 来源管理 ======

    fun update(source: String, data: AttributeData) {
        sources[source] = data
        markDirty()
    }

    fun remove(source: String) {
        if (sources.remove(source) != null) {
            markDirty()
        }
    }

    fun removeByNamespace(namespace: String) {
        val prefix = "$namespace:"
        val removed = sources.keys.filter { it.startsWith(prefix) || it == namespace }
        removed.forEach { sources.remove(it) }
        if (removed.isNotEmpty()) markDirty()
    }

    fun hasSource(source: String): Boolean = sources.containsKey(source)

    fun getSourceData(source: String): AttributeData? = sources[source]?.copy()

    fun getSources(): Map<String, AttributeData> = sources.toMap()

    // ====== 属性查询 ======

    fun get(attributeId: String): Double {
        if (dirty) recalculate()
        return cache[attributeId] ?: (AttributeRegistry.get(attributeId)?.default ?: 0.0)
    }

    fun getAll(): Map<String, Double> {
        if (dirty) recalculate()
        return cache.filterKeys { !it.endsWith("_min") && !it.endsWith("_max") }
    }

    fun getRandom(attributeId: String): Double {
        if (dirty) recalculate()
        val value = cache[attributeId] ?: return AttributeRegistry.get(attributeId)?.default ?: 0.0
        val minKey = "${attributeId}_min"
        val maxKey = "${attributeId}_max"
        val min = cache[minKey]
        val max = cache[maxKey]
        if (min != null && max != null && min != max) {
            return min + Math.random() * (max - min)
        }
        return value
    }

    fun getMin(attributeId: String): Double {
        if (dirty) recalculate()
        return cache["${attributeId}_min"] ?: get(attributeId)
    }

    fun getMax(attributeId: String): Double {
        if (dirty) recalculate()
        return cache["${attributeId}_max"] ?: get(attributeId)
    }

    fun getBase(attributeId: String): Double {
        // 不含 buff 来源的基础值
        var result = 0.0
        for ((source, data) in sources) {
            if (source.startsWith("buff:")) continue
            val values = data.get(attributeId) ?: continue
            result += values.firstOrNull() ?: 0.0
        }
        return result
    }

    // ====== 脏标记与重算 ======

    fun markDirty() {
        dirty = true
    }

    fun recalculate() {
        cache.clear()

        // 收集所有属性 ID
        val allAttrIds = mutableSetOf<String>()
        for (data in sources.values) {
            allAttrIds.addAll(data.keys())
        }
        // 也包含注册表中有默认值的属性
        for (attr in AttributeRegistry.getAll().values) {
            allAttrIds.add(attr.id)
        }

        // 对每个属性，收集所有来源的数值列表
        for (attrId in allAttrIds) {
            val allValues = mutableListOf<List<Double>>()
            var hasRange = false
            for ((source, data) in sources) {
                val values = data.get(attrId) ?: continue
                // 内嵌条件检查：条件不满足则跳过该来源的此属性
                val condition = data.getCondition(attrId)
                if (condition != null && !InlineConditionChecker.check(entity, condition, source)) {
                    continue
                }
                allValues.add(values)
                if (data.isRange(attrId)) hasRange = true
            }
            val attr = AttributeRegistry.get(attrId)
            val defaultValue = attr?.default ?: 0.0

            if (allValues.isEmpty()) {
                cache[attrId] = defaultValue
                continue
            }

            // 属性定义标记 range 或来源数据解析为范围值，都走范围合并
            if (attr?.range == true || hasRange) {
                // 范围属性：分别累加 min 和 max
                var minSum = 0.0
                var maxSum = 0.0
                for (values in allValues) {
                    minSum += values.getOrElse(0) { 0.0 }
                    maxSum += values.getOrElse(1) { values.getOrElse(0) { 0.0 } }
                }
                val minVal = defaultValue + minSum
                val maxVal = defaultValue + maxSum
                cache[attrId] = (minVal + maxVal) / 2.0
                cache["${attrId}_min"] = minVal
                cache["${attrId}_max"] = maxVal
            } else {
                // 非范围属性：调用 combine 脚本（支持加算+乘算）
                val combined = CombineHelper.combine(attrId, allValues, defaultValue)
                cache[attrId] = combined
            }
        }

        dirty = false
    }

    fun cleanup() {
        sources.clear()
        cache.clear()
    }

    fun sourceCount(): Int = sources.size
}
