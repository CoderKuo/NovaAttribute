package com.dakuo.novaattribute.compat.mythic

import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.attribute.AttributeTrigger
import com.dakuo.novaattribute.api.event.RefreshCause
import com.dakuo.novaattribute.core.reader.LoreReader
import com.dakuo.novaattribute.script.ScriptBridge
import com.dakuo.novaattribute.trigger.TriggerManager
import com.dakuo.rulib.common.debug
import ink.ptms.um.Mythic
import ink.ptms.um.event.MobDeathEvent
import ink.ptms.um.event.MobSpawnEvent
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning

/**
 * MythicMobs 集成（通过 universal-mythic API）
 *
 * 功能:
 * 1. 怪物生成时注入属性（NovaAttributes / NovaAttributeLines）
 * 2. 等级缩放（NovaAttributeScale）
 * 3. 掉落增强（NovaDropBonus）— 根据击杀者属性值影响掉落数量
 * 4. CUSTOM 触发器绑定（怪物释放 MM 技能时触发 CUSTOM 属性）
 *
 * MythicMobs 怪物配置示例:
 * ```yaml
 * ExampleMob:
 *   Type: ZOMBIE
 *   Health: 100
 *   NovaAttributes:
 *     physical_damage: 50
 *     physical_defense: 30
 *   NovaAttributeScale: 0.1    # 每级属性 +10%
 *   NovaDropBonus:
 *     attr: luck               # 读取击杀者的 luck 属性
 *     multiplier: 0.01         # 每点 luck 增加 1% 掉落数量
 * ```
 */
object MythicMobsHook {

    private var available = false

    fun init() {
        available = try {
            Mythic.isLoaded()
        } catch (_: Exception) {
            false
        }
        if (available) {
            info("[NovaAttribute] MythicMobs integration enabled (via universal-mythic).")
        }
    }

    fun isAvailable(): Boolean = available

    // ====== 怪物生成：属性注入 + 等级缩放 ======

    @SubscribeEvent
    fun onMobSpawn(event: MobSpawnEvent) {
        if (!available) return
        val mob = event.mob ?: return
        val entity = mob.entity as? LivingEntity ?: return
        val config = mob.type.config
        val level = event.level

        var data: AttributeData? = null

        // 方式一：直接属性映射 (NovaAttributes 节点)
        val attrSection = config.getConfigurationSection("NovaAttributes")
        if (attrSection != null) {
            data = AttributeData()
            for (key in attrSection.getKeys(false)) {
                AttributeRegistry.get(key) ?: continue
                val value = attrSection.getDouble(key, 0.0)
                if (value != 0.0) {
                    data.set(key, value)
                }
            }
            if (data.isEmpty()) data = null
        }

        // 方式二：Lore 格式属性行 (NovaAttributeLines 节点)
        if (data == null) {
            val lines = config.getStringList("NovaAttributeLines")
            if (lines.isNotEmpty()) {
                data = LoreReader.readLines(lines)
            }
        }

        if (data == null || data.isEmpty()) return

        // 等级缩放: 所有属性值 × (1 + level × scale)
        val scale = config.getDouble("NovaAttributeScale", 0.0)
        if (scale != 0.0 && level > 0) {
            val factor = 1.0 + level * scale
            val scaled = AttributeData()
            for (attrId in data.keys()) {
                val values = data.get(attrId) ?: continue
                scaled.set(attrId, values.map { it * factor })
            }
            data = scaled
        }

        val finalData = data
        val mobId = mob.id
        submit(delay = 1L) {
            if (entity.isValid && !entity.isDead) {
                AttributeManager.get(entity).update("mythicmobs:$mobId", finalData)
                AttributeManager.refresh(entity, RefreshCause.API_CALL)
                entity.health = entity.maxHealth
                debug("[MythicMobs] Applied attributes to {mob} (level={level}): {attrs}",
                    "mob" to mobId,
                    "level" to level,
                    "attrs" to finalData.keys().joinToString(", ")
                )
            }
        }
    }

    // ====== 掉落增强 ======

    @SubscribeEvent
    fun onMobDeath(event: MobDeathEvent) {
        if (!available) return
        val killer = event.killer ?: return
        val config = event.mob.type.config
        val bonusSection = config.getConfigurationSection("NovaDropBonus") ?: return

        val attrId = bonusSection.getString("attr") ?: return
        val map = AttributeManager.getOrNull(killer) ?: return
        val attrValue = map.get(attrId)
        if (attrValue <= 0) return

        val scriptName = bonusSection.getString("script")
        if (scriptName != null && ScriptBridge.isLoaded(scriptName)) {
            // 脚本模式：完全由脚本控制掉落逻辑
            // 脚本函数签名: onDrop(killer, mobId, level, drops, attrValue)
            // drops 是 MutableList<ItemStack>，脚本可直接操作（增删改）
            try {
                ScriptBridge.callFunction(
                    scriptName, "onDrop",
                    killer, event.mob.id, event.mob.level, event.drop, attrValue
                )
                debug("[MythicMobs] Drop script '{script}' executed for {player} ({attr}={value})",
                    "script" to scriptName,
                    "player" to killer.name,
                    "attr" to attrId,
                    "value" to attrValue
                )
            } catch (e: Exception) {
                warning("[NovaAttribute] Drop bonus script '$scriptName' error: ${e.message}")
            }
        } else {
            // 简单模式：按倍率增加掉落数量
            val multiplier = bonusSection.getDouble("quantity", 0.01)
            val dropMultiplier = 1.0 + attrValue * multiplier
            if (dropMultiplier <= 1.0) return

            val drops = event.drop
            val extraDrops = mutableListOf<ItemStack>()
            for (item in drops) {
                val extraCount = (item.amount * (dropMultiplier - 1.0)).toInt()
                if (extraCount > 0) {
                    val extra = item.clone()
                    extra.amount = extraCount
                    extraDrops.add(extra)
                }
            }
            drops.addAll(extraDrops)

            debug("[MythicMobs] Drop quantity bonus for {player}: {attr}={value}, x{mult}",
                "player" to killer.name,
                "attr" to attrId,
                "value" to attrValue,
                "mult" to String.format("%.2f", dropMultiplier)
            )
        }
    }

}
