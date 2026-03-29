package com.dakuo.novaattribute.core.attribute

import org.bukkit.inventory.ItemStack

class AttributeBundle {

    private val sources = mutableMapOf<String, AttributeData>()
    private val items = mutableMapOf<String, ItemStack>()

    fun source(key: String, block: AttributeData.() -> Unit) {
        sources[key] = AttributeData.build(block)
    }

    fun source(key: String, data: AttributeData) {
        sources[key] = data
    }

    fun item(key: String, item: ItemStack) {
        items[key] = item
    }

    fun getSources(): Map<String, AttributeData> = sources.toMap()

    fun getItems(): Map<String, ItemStack> = items.toMap()

    fun isEmpty(): Boolean = sources.isEmpty() && items.isEmpty()

    companion object {

        fun build(block: AttributeBundle.() -> Unit): AttributeBundle {
            return AttributeBundle().apply(block)
        }
    }
}
