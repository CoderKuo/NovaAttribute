package com.dakuo.novaattribute.feature.ui

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.util.NumberFormatter
import org.bukkit.entity.Player
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.buildItem

/**
 * 属性面板 GUI（DESIGN.md 4.14）
 */
object AttributePanelUI {

    /**
     * 主面板：分页展示所有属性，左键查看来源明细
     */
    fun openMain(viewer: Player, target: Player) {
        val map = AttributeManager.get(target)
        val allAttrs = map.getAll().toSortedMap().map { (id, value) ->
            val attr = AttributeRegistry.get(id)
            Triple(id, attr?.name ?: id, value)
        }

        viewer.openMenu<PageableChest<Triple<String, String, Double>>>("§0${target.name} 的属性") {
            rows(6)
            map(
                "#########",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "###<#>###"
            )
            set('#', buildItem(XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " })
            slotsBy('@')
            elements { allAttrs }
            onGenerate { _, (id, name, value), _, _ ->
                val attr = AttributeRegistry.get(id)
                val lore = mutableListOf<String>()
                lore += "§7ID: §f$id"
                lore += "§7当前值: §f${NumberFormatter.format(value)}"
                if (attr != null) {
                    if (attr.range) {
                        lore += "§7最小值: §f${NumberFormatter.format(map.getMin(id))}"
                        lore += "§7最大值: §f${NumberFormatter.format(map.getMax(id))}"
                    }
                    lore += "§7基础值: §f${NumberFormatter.format(map.getBase(id))}"
                    lore += "§7默认值: §f${NumberFormatter.format(attr.default)}"
                }
                lore += ""
                lore += "§e左键 §7查看来源明细"

                val material = if (value > 0) XMaterial.LIME_DYE else XMaterial.GRAY_DYE
                buildItem(material) {
                    this.name = "§e$name"
                    this.lore += lore
                }
            }
            onClick { event, (id, _, _) ->
                event.clickEvent().isCancelled = true
                if (event.clickEvent().isLeftClick) {
                    taboolib.common.platform.function.submit {
                        openDetail(viewer, target, id)
                    }
                }
            }
            setNextPage(getFirstSlot('>')) { _, hasNext ->
                buildItem(if (hasNext) XMaterial.ARROW else XMaterial.GRAY_STAINED_GLASS_PANE) {
                    name = if (hasNext) "§a下一页" else " "
                }
            }
            setPreviousPage(getFirstSlot('<')) { _, hasPrev ->
                buildItem(if (hasPrev) XMaterial.ARROW else XMaterial.GRAY_STAINED_GLASS_PANE) {
                    name = if (hasPrev) "§a上一页" else " "
                }
            }
            onClick(lock = true) {}
        }
    }

    /**
     * 来源明细：展示某属性的所有来源贡献
     */
    fun openDetail(viewer: Player, target: Player, attrId: String) {
        val map = AttributeManager.get(target)
        val attr = AttributeRegistry.get(attrId)
        val displayName = attr?.name ?: attrId

        // 收集所有来源对该属性的贡献
        data class SourceEntry(val source: String, val values: List<Double>)

        val entries = mutableListOf<SourceEntry>()
        for ((source, data) in map.getSources()) {
            val values = data.get(attrId) ?: continue
            entries.add(SourceEntry(source, values))
        }

        viewer.openMenu<PageableChest<SourceEntry>>("§0$displayName 的来源") {
            rows(6)
            map(
                "#########",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "###<B>###"
            )
            set('#', buildItem(XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " })
            set('B', buildItem(XMaterial.BARRIER) { name = "§c返回" }) {
                taboolib.common.platform.function.submit {
                    openMain(viewer, target)
                }
            }
            slotsBy('@')
            elements { entries }
            onGenerate { _, entry, _, _ ->
                val lore = mutableListOf<String>()
                lore += "§7来源: §f${entry.source}"
                when (entry.values.size) {
                    1 -> lore += "§7值: §f${NumberFormatter.format(entry.values[0])}"
                    2 -> {
                        if (attr != null && attr.range) {
                            lore += "§7最小值: §f${NumberFormatter.format(entry.values[0])}"
                            lore += "§7最大值: §f${NumberFormatter.format(entry.values[1])}"
                        } else {
                            lore += "§7基础值: §f${NumberFormatter.format(entry.values[0])}"
                            lore += "§7百分比: §f${NumberFormatter.format(entry.values[1] * 100)}%"
                        }
                    }
                    else -> {
                        for ((i, v) in entry.values.withIndex()) {
                            lore += "§7[$i]: §f${NumberFormatter.format(v)}"
                        }
                    }
                }

                val material = when {
                    entry.source.startsWith("equipment:") -> XMaterial.DIAMOND_CHESTPLATE
                    entry.source.startsWith("buff:") -> XMaterial.POTION
                    else -> XMaterial.PAPER
                }
                buildItem(material) {
                    this.name = "§e${entry.source}"
                    this.lore += lore
                }
            }
            onClick { event, _ ->
                event.clickEvent().isCancelled = true
            }
            setNextPage(getFirstSlot('>')) { _, hasNext ->
                buildItem(if (hasNext) XMaterial.ARROW else XMaterial.GRAY_STAINED_GLASS_PANE) {
                    name = if (hasNext) "§a下一页" else " "
                }
            }
            setPreviousPage(getFirstSlot('<')) { _, hasPrev ->
                buildItem(if (hasPrev) XMaterial.ARROW else XMaterial.GRAY_STAINED_GLASS_PANE) {
                    name = if (hasPrev) "§a上一页" else " "
                }
            }
            onClick(lock = true) {}
        }
    }
}
