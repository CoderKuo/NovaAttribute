package com.dakuo.novaattribute

import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.buff.BuffManager
import com.dakuo.novaattribute.combat.CombatMessage
import com.dakuo.novaattribute.realizer.PeriodicScheduler
import com.dakuo.novaattribute.core.condition.ConditionChecker
import com.dakuo.novaattribute.core.condition.InlineConditionChecker
import com.dakuo.novaattribute.core.cooldown.CooldownManager
import com.dakuo.novaattribute.core.counter.CounterManager
import com.dakuo.novaattribute.combat.DamageIndicator
import com.dakuo.novaattribute.core.reader.LoreReader
import com.dakuo.novaattribute.feature.affix.AffixManager
import com.dakuo.novaattribute.feature.lore.LoreGenerator
import com.dakuo.novaattribute.listener.EquipmentListener
import com.dakuo.novaattribute.feature.mob.MobElementManager
import com.dakuo.novaattribute.feature.storage.StorageManager
import com.dakuo.novaattribute.compat.mythic.MythicMobsHook
import com.dakuo.novaattribute.realizer.HealthScaleManager
import com.dakuo.novaattribute.realizer.VanillaSync
import com.dakuo.novaattribute.trigger.BuiltinTriggers
import com.dakuo.novaattribute.trigger.TriggerManager
import com.dakuo.novaattribute.util.NumberFormatter
import com.dakuo.novaattribute.script.ScriptLibraryExport
import com.dakuo.novaattribute.script.ScriptManager
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration
import java.io.File

object NovaAttribute : Plugin() {

    override fun onLoad() {
        // 尽早注册 NovaScript 全局函数库（所有 onLoad 先于所有 onEnable）
        // 确保其他插件在 onEnable 加载脚本时能找到 novaattr 库
        try {
            ScriptLibraryExport.register()
        } catch (_: UninitializedPropertyAccessException) {
            // NovaScript delegate 尚未初始化（NovaScript 未安装或未加载），跳过库注册
            // 会在 onEnable 中重试
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to register script library: ${e.message}")
        }

        // 释放默认资源文件
        releaseResourceFile("config.yml", replace = false)
        releaseResourceFile("attributes/default.yml", replace = false)
        releaseResourceFile("scripts/combine.nova", replace = false)
        releaseResourceFile("scripts/damage.nova", replace = false)
        releaseResourceFile("scripts/combat_power.nova", replace = false)
        releaseResourceFile("scripts/critical.nova", replace = false)
        releaseResourceFile("scripts/dodge.nova", replace = false)
        releaseResourceFile("scripts/lifesteal.nova", replace = false)
        releaseResourceFile("scripts/health_regen.nova", replace = false)
        releaseResourceFile("scripts/thorns.nova", replace = false)
        releaseResourceFile("scripts/damage_reduction.nova", replace = false)
        releaseResourceFile("scripts/block.nova", replace = false)
        releaseResourceFile("scripts/toughness.nova", replace = false)
        releaseResourceFile("scripts/ignite.nova", replace = false)
        releaseResourceFile("scripts/poison.nova", replace = false)
        releaseResourceFile("scripts/wither_effect.nova", replace = false)
        releaseResourceFile("scripts/slow.nova", replace = false)
        releaseResourceFile("scripts/blind.nova", replace = false)
        releaseResourceFile("scripts/lightning.nova", replace = false)
        releaseResourceFile("scripts/knockback_enhance.nova", replace = false)
        releaseResourceFile("scripts/kill_heal.nova", replace = false)
        releaseResourceFile("scripts/exp_bonus.nova", replace = false)
        releaseResourceFile("scripts/affix_elemental.nova", replace = false)
        releaseResourceFile("scripts/drop_bonus.nova", replace = false)
        releaseResourceFile("attributes/mechanics.yml", replace = false)
        releaseResourceFile("mob-elements.yml", replace = false)
        releaseResourceFile("affixes.yml", replace = false)
        releaseResourceFile("conditions/default.nova", replace = false)
        releaseResourceFile("conditions/inline.nova", replace = false)
        releaseResourceFile("lore-template.yml", replace = false)
    }

    override fun onEnable() {
        val dataFolder = getDataFolder()
        val config = Configuration.loadFromFile(File(dataFolder, "config.yml"))

        // 1. 加载属性定义
        val attrDir = File(dataFolder, "attributes")
        AttributeRegistry.loadFromDirectory(attrDir)

        // 2. 初始化 Lore 全局解析
        LoreReader.init(config)
        LoreReader.rebuildLookup()

        // 3. 初始化脚本系统
        ScriptManager.init(dataFolder)
        ScriptManager.loadAll()

        // 4. 启动 Buff 过期检查
        BuffManager.startTickTask()

        // 5. 初始化数值格式化、伤害指示器和战斗消息
        NumberFormatter.init(config)
        DamageIndicator.init(config)
        CombatMessage.init(config)

        // 6. 初始化条件系统
        ConditionChecker.init(config)
        InlineConditionChecker.init(config)

        // 7. 初始化 Lore 生成器
        val loreTemplateFile = File(dataFolder, "lore-template.yml")
        if (loreTemplateFile.exists()) {
            val loreConfig = Configuration.loadFromFile(loreTemplateFile)
            LoreGenerator.init(loreConfig)
        }

        // 8. 初始化数据持久化（使用 Rulib 数据库模块）
        StorageManager.init(config)

        // 9. 初始化词条系统和怪物元素弱点系统
        AffixManager.init(dataFolder)
        MobElementManager.init(dataFolder)

        // 10. 启动装备内容变更检测（每秒检查物品是否被外部修改）
        EquipmentListener.startContentCheck()
        // 启动蓄力值追踪（每 tick 缓存，攻击事件读上一 tick 的值）
        com.dakuo.novaattribute.listener.MechanicsListener.startChargeTracker()

        // 11. 启动 PERIODIC 属性调度器
        PeriodicScheduler.start()

        // 12. 注册内置事件触发器
        BuiltinTriggers.registerAll()

        // 13. 初始化 Realizer（原版属性同步 + 血量条缩放）
        VanillaSync.init(config)
        HealthScaleManager.init(config)

        // 14. 初始化 MythicMobs 集成（可选）
        try {
            MythicMobsHook.init()
        } catch (_: NoClassDefFoundError) {
            info("[NovaAttribute] MythicMobs not found, skipping hook.")
        } catch (e: Exception) {
            warning("[NovaAttribute] MythicMobs hook failed: ${e.message}")
            e.printStackTrace()
        }

        // 15. 初始化 SX2 兼容层
        try {
            com.dakuo.novaattribute.compat.sx2.SX2Bridge.init()
        } catch (_: NoClassDefFoundError) {
        }

        // 16. 重试注册脚本库（onLoad 时 NovaScript 可能未就绪）
        try {
            ScriptLibraryExport.register()
        } catch (_: Exception) {
            warning("[NovaAttribute] NovaScript not available, script library not registered.")
        }

        info("[NovaAttribute] Plugin enabled! Attributes: ${AttributeRegistry.size()}")
    }

    override fun onDisable() {
        // 同步保存所有在线玩家的持久化数据
        if (StorageManager.isEnabled()) {
            for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
                val uuid = player.uniqueId
                val persistentBuffs = BuffManager.getAll(player).filter { it.persistent }
                val persistentCooldowns = CooldownManager.getPersistent(uuid)
                val persistentCounters = CounterManager.getPersistent(uuid)
                StorageManager.saveBuffs(uuid, persistentBuffs)
                StorageManager.saveCooldowns(uuid, persistentCooldowns)
                StorageManager.saveCounters(uuid, persistentCounters)
            }
        }

        // 恢复原版状态
        VanillaSync.unsyncAll()
        HealthScaleManager.disable()

        // SX2 兼容清理
        try {
            com.dakuo.novaattribute.compat.sx2.SX2Bridge.disable()
        } catch (_: NoClassDefFoundError) {
        }

        // 停止调度器和触发器
        com.dakuo.novaattribute.listener.MechanicsListener.stopChargeTracker()
        PeriodicScheduler.stop()
        TriggerManager.unregisterAll()
        try {
            ScriptLibraryExport.unregister()
        } catch (_: Exception) {
        }

        // 清理所有数据
        AttributeManager.cleanup()
        BuffManager.cleanupAll()
        CooldownManager.cleanupAll()
        CounterManager.cleanupAll()
        ScriptManager.reload() // 卸载所有脚本

        info("[NovaAttribute] Plugin disabled.")
    }

    private fun getDataFolder(): File {
        return taboolib.platform.BukkitPlugin.getInstance().dataFolder
    }
}
