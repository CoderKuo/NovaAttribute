package com.dakuo.novaattribute.feature.ui

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.util.NumberFormatter
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.buildItem

/**
 * 属性面板 GUI
 * 布局和样式由 gui/panel.yml 配置驱动
 */
object AttributePanelUI {

    @Config("gui/panel.yml")
    lateinit var config: Configuration

    fun openMain(viewer: Player, target: Player) {
        val map = AttributeManager.get(target)
        val allAttrs = map.getAll().toSortedMap().map { (id, value) ->
            val attr = AttributeRegistry.get(id)
            Triple(id, attr?.name ?: id, value)
        }

        val title = config.getString("title", "§0{player} 的属性")!!
            .replace("{player}", target.name)
        val layout = config.getStringList("layout")
        val rows = layout.size.coerceIn(1, 6)

        // 解析布局
        val layoutArray = layout.map { it.toCharArray().toList() }
        val contentChar = findSlotChar("content") ?: 'A'
        val prevChar = findSlotChar("prev-page") ?: '<'
        val nextChar = findSlotChar("next-page") ?: '>'

        // 属性物品模板
        val attrSection = config.getConfigurationSection("attribute-item")
        val attrMaterial = parseMaterial(attrSection?.getString("material") ?: "lime_dye")
        val zeroMaterial = parseMaterial(attrSection?.getString("zero-material") ?: "gray_dye")
        val attrNameFormat = attrSection?.getString("name") ?: "§e{attr_name}"
        val attrLoreTemplate = attrSection?.getStringList("lore") ?: listOf("§7{id}: §f{value}")

        viewer.openMenu<PageableChest<Triple<String, String, Double>>>(title) {
            rows(rows)
            map(*layoutArray.map { it.joinToString("") }.toTypedArray())

            // 填充边框
            for ((key, _) in getSlotTypes()) {
                if (getSlotType(key) == "frame") {
                    val item = buildSlotItem(key.first())
                    if (item != null) set(key.first(), item)
                }
            }

            slotsBy(contentChar)
            elements { allAttrs }

            onGenerate { _, (id, name, value), _, _ ->
                val attr = AttributeRegistry.get(id)
                val placeholders = mapOf(
                    "{id}" to id,
                    "{value}" to formatAttrValue(id, value, map),
                    "{min}" to NumberFormatter.format(map.getMin(id)),
                    "{max}" to NumberFormatter.format(map.getMax(id)),
                    "{base}" to NumberFormatter.format(map.getBase(id)),
                    "{default}" to NumberFormatter.format(attr?.default ?: 0.0),
                    "{attr_name}" to name
                )

                // 尝试从 attribute-icons 获取自定义完整物品
                val customItem = resolveAttrIcon(id, placeholders)
                if (customItem != null) {
                    customItem
                } else {
                    // 使用默认模板
                    val lore = attrLoreTemplate.map { line -> replacePlaceholders(line, placeholders) }
                    val material = if (value != 0.0) attrMaterial else zeroMaterial
                    buildItem(material) {
                        this.name = replacePlaceholders(attrNameFormat, placeholders)
                        this.lore += lore
                    }
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

            setNextPage(getFirstSlot(nextChar)) { _, hasNext ->
                if (hasNext) buildSlotItem(nextChar)
                    ?: buildItem(XMaterial.ARROW) { this.name = "§a下一页" }
                else buildEmptyItem(nextChar)
            }
            setPreviousPage(getFirstSlot(prevChar)) { _, hasPrev ->
                if (hasPrev) buildSlotItem(prevChar)
                    ?: buildItem(XMaterial.ARROW) { this.name = "§a上一页" }
                else buildEmptyItem(prevChar)
            }
            onClick(lock = true) {}
        }
    }

    fun openDetail(viewer: Player, target: Player, attrId: String) {
        val map = AttributeManager.get(target)
        val attr = AttributeRegistry.get(attrId)
        val displayName = attr?.name ?: attrId

        data class SourceEntry(val source: String, val values: List<Double>, val isRange: Boolean)

        val entries = mutableListOf<SourceEntry>()

        // 默认值条目
        val defaultValue = attr?.default ?: 0.0
        if (defaultValue != 0.0) {
            entries.add(SourceEntry("§7默认值", listOf(defaultValue), false))
        }

        // 各来源条目
        for ((source, data) in map.getSources()) {
            val values = data.get(attrId) ?: continue
            entries.add(SourceEntry(source, values, data.isRange(attrId)))
        }

        // 总计条目
        val totalValue = map.get(attrId)
        val min = map.getMin(attrId)
        val max = map.getMax(attrId)
        if (min != max) {
            entries.add(SourceEntry("§a总计", listOf(min, max), true))
        } else {
            entries.add(SourceEntry("§a总计", listOf(totalValue), false))
        }

        val title = config.getString("detail-title", "§0{attr_name} 的来源")!!
            .replace("{attr_name}", displayName)
        val layout = config.getStringList("layout")
        val rows = layout.size.coerceIn(1, 6)
        val layoutArray = layout.map { it.toCharArray().toList() }
        val contentChar = findSlotChar("content") ?: 'A'
        val prevChar = findSlotChar("prev-page") ?: '<'
        val nextChar = findSlotChar("next-page") ?: '>'

        // 来源图标映射
        val sourceIcons = config.getConfigurationSection("source-icons")
        val defaultIcon = parseMaterial(sourceIcons?.getString("default") ?: "paper")

        // 返回按钮
        val backSection = config.getConfigurationSection("back-button")
        val backMaterial = parseMaterial(backSection?.getString("material") ?: "barrier")
        val backName = backSection?.getString("name") ?: "§c返回"

        viewer.openMenu<PageableChest<SourceEntry>>(title) {
            rows(rows)
            map(*layoutArray.map { it.joinToString("") }.toTypedArray())

            for ((key, _) in getSlotTypes()) {
                if (getSlotType(key) == "frame") {
                    val item = buildSlotItem(key.first())
                    if (item != null) set(key.first(), item)
                }
            }

            // 放置返回按钮（替换一个边框位置：底部中间）
            val backSlot = rows * 9 / 2
            set(backSlot, buildItem(backMaterial) { name = backName }) {
                taboolib.common.platform.function.submit {
                    openMain(viewer, target)
                }
            }

            slotsBy(contentChar)
            elements { entries }

            onGenerate { _, entry, _, _ ->
                val lore = mutableListOf<String>()
                lore += "§7来源: §f${entry.source}"
                when (entry.values.size) {
                    1 -> lore += "§7值: §f${NumberFormatter.format(entry.values[0])}"
                    2 -> {
                        if (entry.isRange) {
                            lore += "§7最小值: §f${NumberFormatter.format(entry.values[0])}"
                            lore += "§7最大值: §f${NumberFormatter.format(entry.values[1])}"
                        } else {
                            lore += "§7基础值: §f${NumberFormatter.format(entry.values[0])}"
                            lore += "§7百分比: §f${NumberFormatter.format(entry.values[1] * 100)}%"
                        }
                    }
                    else -> entry.values.forEachIndexed { i, v ->
                        lore += "§7[$i]: §f${NumberFormatter.format(v)}"
                    }
                }

                val material = when {
                    entry.source == "§a总计" -> parseMaterial("emerald")
                    entry.source == "§7默认值" -> parseMaterial("command_block")
                    else -> {
                        val prefix = entry.source.substringBefore(":")
                        parseMaterial(sourceIcons?.getString(prefix) ?: sourceIcons?.getString("default") ?: "paper")
                    }
                }

                buildItem(material) {
                    this.name = "§e${entry.source}"
                    this.lore += lore
                }
            }

            onClick { event, _ -> event.clickEvent().isCancelled = true }

            setNextPage(getFirstSlot(nextChar)) { _, hasNext ->
                if (hasNext) buildSlotItem(nextChar)
                    ?: buildItem(XMaterial.ARROW) { this.name = "§a下一页" }
                else buildEmptyItem(nextChar)
            }
            setPreviousPage(getFirstSlot(prevChar)) { _, hasPrev ->
                if (hasPrev) buildSlotItem(prevChar)
                    ?: buildItem(XMaterial.ARROW) { this.name = "§a上一页" }
                else buildEmptyItem(prevChar)
            }
            onClick(lock = true) {}
        }
    }

    // ====== 工具方法 ======

    private fun getSlotTypes(): Map<String, String> {
        val section = config.getConfigurationSection("slots") ?: return emptyMap()
        return section.getKeys(false).associateWith { key ->
            section.getString("$key.type", "frame")!!
        }
    }

    private fun getSlotType(key: String): String {
        return config.getString("slots.$key.type", "frame")!!
    }

    private fun findSlotChar(type: String): Char? {
        val section = config.getConfigurationSection("slots") ?: return null
        for (key in section.getKeys(false)) {
            if (section.getString("$key.type") == type) return key.first()
        }
        return null
    }

    private fun buildSlotItem(slotChar: Char): ItemStack? {
        val key = slotChar.toString()
        val section = config.getConfigurationSection("slots.$key.item") ?: return null
        val mat = parseMaterial(section.getString("material") ?: return null)
        return buildItem(mat) {
            name = section.getString("name") ?: " "
        }
    }

    private fun buildEmptyItem(slotChar: Char): ItemStack {
        val key = slotChar.toString()
        val section = config.getConfigurationSection("slots.$key.empty")
        if (section != null) {
            val mat = parseMaterial(section.getString("material") ?: "gray_stained_glass_pane")
            return buildItem(mat) { name = section.getString("name") ?: " " }
        }
        return buildItem(XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " }
    }

    /**
     * 从 attribute-icons 解析完整 ItemStack
     * 优先精确匹配属性 ID，再前缀匹配
     * 返回 null 表示使用默认模板
     */
    private fun resolveAttrIcon(attrId: String, placeholders: Map<String, String>): ItemStack? {
        val section = config.getConfigurationSection("attribute-icons") ?: return null

        // 精确匹配
        var iconSection = section.getConfigurationSection(attrId)

        // 前缀匹配
        if (iconSection == null) {
            for (key in section.getKeys(false)) {
                if (key.endsWith("_") && attrId.startsWith(key)) {
                    iconSection = section.getConfigurationSection(key)
                    break
                }
            }
        }

        if (iconSection == null) return null

        val material = parseMaterial(iconSection.getString("material") ?: return null)
        return buildItem(material) {
            // name: 有自定义就用自定义，没有就用全局模板
            val customName = iconSection.getString("name")
            this.name = replacePlaceholders(
                customName ?: config.getString("attribute-item.name") ?: "§e{attr_name}",
                placeholders
            )

            // lore: 有自定义就用自定义，没有就用全局模板
            val customLore = iconSection.getStringList("lore")
            val loreTemplate = if (customLore.isNotEmpty()) customLore
                else config.getStringList("attribute-item.lore")
            this.lore += loreTemplate.map { replacePlaceholders(it, placeholders) }

            // custom-model-data
            val cmd = iconSection.getInt("custom-model-data", 0)
            if (cmd > 0) {
                this.customModelData = cmd
            }
        }
    }

    private fun replacePlaceholders(text: String, placeholders: Map<String, String>): String {
        var result = text
        for ((key, value) in placeholders) {
            result = result.replace(key, value)
        }
        return result
    }

    private fun parseMaterial(name: String): XMaterial {
        return XMaterial.matchXMaterial(name.uppercase()).orElse(XMaterial.STONE)
    }

    private fun formatAttrValue(id: String, value: Double, map: com.dakuo.novaattribute.core.attribute.AttributeMap): String {
        val attr = AttributeRegistry.get(id)
        val min = map.getMin(id)
        val max = map.getMax(id)
        return if (attr?.range == true && min != max) {
            "${NumberFormatter.format(min)} ~ ${NumberFormatter.format(max)}"
        } else {
            NumberFormatter.format(value)
        }
    }
}
