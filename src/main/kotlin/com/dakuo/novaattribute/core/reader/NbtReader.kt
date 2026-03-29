package com.dakuo.novaattribute.core.reader

import com.dakuo.novaattribute.core.attribute.AttributeData
import org.bukkit.inventory.ItemStack
import taboolib.module.nms.ItemTag
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.ItemTagType
import taboolib.module.nms.getItemTag

object NbtReader {

    private const val TAG_KEY = "NovaAttr"

    fun read(item: ItemStack): AttributeData {
        val result = AttributeData()
        val tag: ItemTag
        try {
            tag = item.getItemTag()
        } catch (_: Exception) {
            return result
        }

        val novaTag = tag[TAG_KEY] ?: return result
        if (novaTag.type != ItemTagType.COMPOUND) return result

        val compound = novaTag.asCompound()
        for (attrId in compound.keys) {
            val entry = compound[attrId] ?: continue
            when (entry.type) {
                // 直接数值: NovaAttr.physical_damage = 100.0
                ItemTagType.DOUBLE, ItemTagType.FLOAT, ItemTagType.INT,
                ItemTagType.LONG, ItemTagType.SHORT, ItemTagType.BYTE -> {
                    result.set(attrId, entry.asDouble())
                }
                // 列表: NovaAttr.physical_damage = [100.0, 0.1]
                ItemTagType.LIST -> {
                    val list = entry.asList()
                    val values = mutableListOf<Double>()
                    for (i in 0 until list.size) {
                        values.add(list[i].asDouble())
                    }
                    if (values.isNotEmpty()) {
                        result.set(attrId, values)
                    }
                }
                // 复合: NovaAttr.physical_damage = {min: 50, max: 100} 或 {values: [...], condition: "..."}
                ItemTagType.COMPOUND -> {
                    val sub = entry.asCompound()
                    if (sub.containsKey("min") && sub.containsKey("max")) {
                        // 范围值（可带条件）
                        val min = sub["min"]!!.asDouble()
                        val max = sub["max"]!!.asDouble()
                        result.set(attrId, min, max)
                        result.markRange(attrId)
                        val condition = sub["condition"]
                        if (condition != null) {
                            result.setCondition(attrId, condition.asString())
                        }
                    } else if (sub.containsKey("values")) {
                        // 带条件的属性值: { values: [0.25], condition: "level>=50" }
                        val valueEntry = sub["values"]!!
                        val values = mutableListOf<Double>()
                        if (valueEntry.type == ItemTagType.LIST) {
                            val list = valueEntry.asList()
                            for (i in 0 until list.size) {
                                values.add(list[i].asDouble())
                            }
                        } else {
                            values.add(valueEntry.asDouble())
                        }
                        if (values.isNotEmpty()) {
                            result.set(attrId, values)
                            val condition = sub["condition"]
                            if (condition != null) {
                                result.setCondition(attrId, condition.asString())
                            }
                        }
                    }
                }
                else -> {}
            }
        }
        return result
    }
}
