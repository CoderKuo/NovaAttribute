package com.dakuo.novaattribute.core.reader

import com.dakuo.novaattribute.feature.affix.AffixManager
import com.dakuo.novaattribute.core.attribute.AttributeData
import org.bukkit.inventory.ItemStack

object ItemAttributeReader {

    fun read(item: ItemStack?): AttributeData {
        if (item == null || item.type == org.bukkit.Material.AIR) return AttributeData()
        // NBT 优先，Lore 兜底
        val nbtData = NbtReader.read(item)
        val baseData = if (!nbtData.isEmpty()) nbtData else LoreReader.read(item)
        // 词条属性（始终检查 Lore 文本，与 NBT/Lore 属性来源无关）
        val affixData = AffixManager.match(item)
        if (!affixData.isEmpty()) {
            baseData.merge(affixData)
        }
        return baseData
    }
}
