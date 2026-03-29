package com.dakuo.novaattribute.core.attribute

class AttributeData {

    private val data = mutableMapOf<String, List<Double>>()
    private val conditions = mutableMapOf<String, String>()
    private val ranges = mutableSetOf<String>()

    fun set(attrId: String, vararg values: Double) {
        data[attrId] = values.toList()
    }

    fun set(attrId: String, values: List<Double>) {
        data[attrId] = values.toList()
    }

    fun get(attrId: String): List<Double>? = data[attrId]

    fun getFirst(attrId: String): Double = data[attrId]?.firstOrNull() ?: 0.0

    fun keys(): Set<String> = data.keys

    fun isEmpty(): Boolean = data.isEmpty()

    fun toMap(): Map<String, List<Double>> = data.toMap()

    // ====== 内嵌条件 ======

    fun setCondition(attrId: String, condition: String) {
        conditions[attrId] = condition
    }

    fun getCondition(attrId: String): String? = conditions[attrId]

    fun hasConditions(): Boolean = conditions.isNotEmpty()

    fun getConditions(): Map<String, String> = conditions.toMap()

    // ====== 范围标记 ======

    fun markRange(attrId: String) {
        ranges.add(attrId)
    }

    fun isRange(attrId: String): Boolean = attrId in ranges

    fun merge(other: AttributeData) {
        for ((attrId, values) in other.data) {
            val existing = data[attrId]
            if (existing != null) {
                val merged = mutableListOf<Double>()
                val maxLen = maxOf(existing.size, values.size)
                for (i in 0 until maxLen) {
                    val a = existing.getOrElse(i) { 0.0 }
                    val b = values.getOrElse(i) { 0.0 }
                    merged.add(a + b)
                }
                data[attrId] = merged
            } else {
                data[attrId] = values.toList()
            }
        }
        ranges.addAll(other.ranges)
    }

    fun copy(): AttributeData {
        val clone = AttributeData()
        for ((k, v) in data) {
            clone.data[k] = v.toList()
        }
        for ((k, v) in conditions) {
            clone.conditions[k] = v
        }
        clone.ranges.addAll(ranges)
        return clone
    }

    companion object {

        fun build(block: AttributeData.() -> Unit): AttributeData {
            return AttributeData().apply(block)
        }

        fun fromMap(map: Map<String, List<Double>>): AttributeData {
            val data = AttributeData()
            for ((k, v) in map) {
                data.data[k] = v.toList()
            }
            return data
        }
    }
}
