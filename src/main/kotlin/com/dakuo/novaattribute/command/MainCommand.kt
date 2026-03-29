package com.dakuo.novaattribute.command

import com.dakuo.novaattribute.api.NovaAttributeAPI
import com.dakuo.novaattribute.api.event.RefreshCause
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.attribute.AttributeRegistry
import com.dakuo.novaattribute.core.buff.Buff
import com.dakuo.novaattribute.core.buff.BuffManager
import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.combat.CombatMessage
import com.dakuo.novaattribute.core.condition.ConditionChecker
import com.dakuo.novaattribute.core.condition.InlineConditionChecker
import com.dakuo.novaattribute.combat.DamageIndicator
import com.dakuo.novaattribute.core.reader.LoreReader
import com.dakuo.novaattribute.feature.lore.LoreGenerator
import com.dakuo.novaattribute.script.ScriptManager
import com.dakuo.novaattribute.feature.ui.AttributePanelUI
import com.dakuo.novaattribute.util.Mirror
import com.dakuo.novaattribute.util.NumberFormatter
import taboolib.module.configuration.Configuration
import com.dakuo.rulib.common.Paginator
import com.dakuo.rulib.common.lang.Duration
import com.dakuo.rulib.common.lang.formatDuration
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.releaseResourceFile

@CommandHeader(name = "novaattribute", aliases = ["na", "novaa"], permission = "novaattribute.admin")
object MainCommand {

    @CommandBody
    val main = mainCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§6[NovaAttribute] §f可用子命令:")
            sender.sendMessage("§e/na reload §7- 重载配置和脚本")
            sender.sendMessage("§e/na lookup [player] §7- 查看属性")
            sender.sendMessage("§e/na power [player] §7- 查看战斗力")
            sender.sendMessage("§e/na buff <player> <id> <duration> [attr] [value] §7- 添加 Buff")
            sender.sendMessage("§e/na unbuff <player> <id> §7- 移除 Buff")
            sender.sendMessage("§e/na debug [player] §7- 调试信息")
            sender.sendMessage("§e/na panel [player] §7- 属性面板")
            sender.sendMessage("§e/na refresh [player] §7- 刷新属性")
            sender.sendMessage("§e/na mirror §7- 性能分析报告")
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, _, _ ->
            val dataFolder = Bukkit.getPluginManager().getPlugin("NovaAttribute")?.dataFolder
            if (dataFolder != null) {
                // 释放缺失的资源文件
                releaseResourceFile("config.yml", replace = false)
                releaseResourceFile("attributes/default.yml", replace = false)
                releaseResourceFile("lore-template.yml", replace = false)

                // 重载主配置
                val config = Configuration.loadFromFile(java.io.File(dataFolder, "config.yml"))

                // 重载属性定义
                AttributeRegistry.reload(java.io.File(dataFolder, "attributes"))

                // 重载各子系统
                LoreReader.init(config)
                LoreReader.rebuildLookup()
                NumberFormatter.init(config)
                DamageIndicator.init(config)
                CombatMessage.init(config)
                ConditionChecker.init(config)
                InlineConditionChecker.init(config)

                // 重载 Lore 生成器
                val loreTemplateFile = java.io.File(dataFolder, "lore-template.yml")
                if (loreTemplateFile.exists()) {
                    LoreGenerator.init(Configuration.loadFromFile(loreTemplateFile))
                }

                // 重载脚本
                ScriptManager.reload()

                // 刷新所有在线玩家属性
                Bukkit.getOnlinePlayers().forEach {
                    AttributeManager.refresh(it, RefreshCause.RELOAD)
                }
                sender.sendMessage("§6[NovaAttribute] §a重载完成！属性: ${AttributeRegistry.size()}")
            } else {
                sender.sendMessage("§6[NovaAttribute] §c重载失败")
            }
        }
    }

    @CommandBody
    val lookup = subCommand {
        dynamic("player", optional = true) {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("page", optional = true) {
                execute<CommandSender> { sender, ctx, _ ->
                    showLookup(sender, Bukkit.getPlayerExact(ctx["player"]), ctx["page"].toIntOrNull() ?: 1)
                }
            }
            execute<CommandSender> { sender, ctx, _ ->
                showLookup(sender, Bukkit.getPlayerExact(ctx["player"]))
            }
        }
        execute<CommandSender> { sender, _, _ ->
            showLookup(sender, sender as? Player)
        }
    }

    @CommandBody
    val power = subCommand {
        dynamic("player", optional = true) {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, ctx, _ ->
                showPower(sender, Bukkit.getPlayerExact(ctx["player"]))
            }
        }
        execute<CommandSender> { sender, _, _ ->
            showPower(sender, sender as? Player)
        }
    }

    @CommandBody
    val buff = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("id") {
                dynamic("duration") {
                    dynamic("attr", optional = true) {
                        dynamic("value", optional = true) {
                        }
                    }
                    execute<CommandSender> { sender, ctx, _ ->
                        val player = Bukkit.getPlayerExact(ctx["player"]) ?: run {
                            sender.sendMessage("§6[NovaAttribute] §c玩家不在线")
                            return@execute
                        }
                        val id = ctx["id"]
                        val durationStr = ctx["duration"]
                        // 支持 tick 数字或 Duration 格式（1d2h30m10s）
                        val durationMs: Long = durationStr.toLongOrNull()?.let { it * 50L } ?: try {
                            Duration.parse(durationStr).toMillis()
                        } catch (_: Exception) {
                            sender.sendMessage("§6[NovaAttribute] §c无效的持续时间，支持 tick 数字或 1d2h30m10s 格式")
                            return@execute
                        }
                        val attrId = ctx["attr"]
                        val value = ctx["value"].toDoubleOrNull()

                        val data = AttributeData()
                        if (attrId != null && value != null) {
                            data.set(attrId, value)
                        }

                        val expireAt = if (durationMs < 0) Long.MAX_VALUE
                        else System.currentTimeMillis() + durationMs

                        val b = Buff(
                            id = id,
                            data = data,
                            duration = durationMs,
                            expireAt = expireAt
                        )
                        BuffManager.add(player, b)
                        val displayDuration = if (durationMs < 0) "永久" else durationMs.formatDuration()
                        sender.sendMessage("§6[NovaAttribute] §a已为 ${player.name} 添加 Buff: $id ($displayDuration)")
                    }
                }
            }
        }
    }

    @CommandBody
    val unbuff = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("id") {
                execute<CommandSender> { sender, ctx, _ ->
                    val player = Bukkit.getPlayerExact(ctx["player"]) ?: run {
                        sender.sendMessage("§6[NovaAttribute] §c玩家不在线")
                        return@execute
                    }
                    val id = ctx["id"]
                    BuffManager.remove(player, id)
                    sender.sendMessage("§6[NovaAttribute] §a已移除 ${player.name} 的 Buff: $id")
                }
            }
        }
    }

    @CommandBody
    val debug = subCommand {
        dynamic("player", optional = true) {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, ctx, _ ->
                showDebug(sender, Bukkit.getPlayerExact(ctx["player"]))
            }
        }
        execute<CommandSender> { sender, _, _ ->
            showDebug(sender, sender as? Player)
        }
    }

    @CommandBody
    val panel = subCommand {
        dynamic("player", optional = true) {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<Player> { sender, ctx, _ ->
                val target = Bukkit.getPlayerExact(ctx["player"]) ?: run {
                    sender.sendMessage("§6[NovaAttribute] §c玩家不在线")
                    return@execute
                }
                AttributePanelUI.openMain(sender, target)
            }
        }
        execute<Player> { sender, _, _ ->
            AttributePanelUI.openMain(sender, sender)
        }
    }

    @CommandBody
    val refresh = subCommand {
        dynamic("player", optional = true) {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, ctx, _ ->
                doRefresh(sender, Bukkit.getPlayerExact(ctx["player"]))
            }
        }
        execute<CommandSender> { sender, _, _ ->
            doRefresh(sender, sender as? Player)
        }
    }

    @CommandBody
    val mirror = subCommand {
        dynamic("action", optional = true) {
            execute<CommandSender> { sender, ctx, _ ->
                if (ctx["action"].equals("clear", ignoreCase = true)) {
                    Mirror.clear()
                    sender.sendMessage("§6[NovaAttribute] §a性能数据已清空")
                } else {
                    Mirror.report().forEach { sender.sendMessage(it) }
                }
            }
        }
        execute<CommandSender> { sender, _, _ ->
            Mirror.report().forEach { sender.sendMessage(it) }
        }
    }

    // ====== 辅助函数 ======

    private fun formatDisplay(value: Double, isPercent: Boolean, divisor: Double): String {
        if (isPercent) {
            val display = value * divisor
            // 整数百分比不带小数点
            return if (display == display.toLong().toDouble()) {
                "${display.toLong()}%"
            } else {
                "${NumberFormatter.format(display)}%"
            }
        }
        return NumberFormatter.format(value)
    }

    private fun showLookup(sender: CommandSender, player: Player?, page: Int = 1) {
        if (player == null) {
            sender.sendMessage("§6[NovaAttribute] §c请指定玩家")
            return
        }
        val map = AttributeManager.get(player)
        val attrs = map.getAll()
        val items = attrs.toSortedMap().map { (id, value) ->
            val attr = AttributeRegistry.get(id)
            val name = attr?.name ?: id
            val divisor = attr?.loreDivisor ?: 1.0
            val isPercent = divisor != 1.0
            val min = map.getMin(id)
            val max = map.getMax(id)
            val display = if (min != max) {
                "${formatDisplay(min, isPercent, divisor)} ~ ${formatDisplay(max, isPercent, divisor)}"
            } else {
                formatDisplay(value, isPercent, divisor)
            }
            Triple(id, name, display)
        }
        val sources = map.getSources()

        if (sender is Player) {
            val paginator = Paginator(items, 10)
            paginator.send(
                sender, page,
                header = "§6[NovaAttribute] §f${player.name} 的属性 §7(来源: ${sources.size})",
                command = "/novaattribute lookup ${player.name} {page}"
            ) { (id, name, display), _ ->
                "  §e$name §7($id): §f$display"
            }
        } else {
            sender.sendMessage("§6[NovaAttribute] §f${player.name} 的属性:")
            for ((id, name, display) in items) {
                sender.sendMessage("  §e$name §7($id): §f$display")
            }
            sender.sendMessage("§7来源数: ${sources.size}")
        }
    }

    private fun showPower(sender: CommandSender, player: Player?) {
        if (player == null) {
            sender.sendMessage("§6[NovaAttribute] §c请指定玩家")
            return
        }
        val combat = NovaAttributeAPI.getCombatPower(player)
        sender.sendMessage("§6[NovaAttribute] §f${player.name} 的战斗力: §e${NumberFormatter.format(combat)}")
    }

    private fun showDebug(sender: CommandSender, player: Player?) {
        if (player == null) {
            sender.sendMessage("§6[NovaAttribute] §c请指定玩家")
            return
        }
        val map = AttributeManager.get(player)
        sender.sendMessage("§6[NovaAttribute] §f${player.name} 调试信息:")
        sender.sendMessage("§7来源列表:")
        for ((source, data) in map.getSources()) {
            sender.sendMessage("  §e$source §7→ ${data.keys().joinToString(", ")}")
        }
        sender.sendMessage("§7Buff 列表:")
        for (buff in BuffManager.getAll(player)) {
            val remaining = if (buff.duration < 0) "永久" else buff.getRemaining().formatDuration()
            sender.sendMessage("  §e${buff.id} §7x${buff.stacks} ($remaining)")
        }
    }

    private fun doRefresh(sender: CommandSender, player: Player?) {
        if (player == null) {
            sender.sendMessage("§6[NovaAttribute] §c请指定玩家")
            return
        }
        AttributeManager.refresh(player, RefreshCause.API_CALL)
        sender.sendMessage("§6[NovaAttribute] §a已刷新 ${player.name} 的属性")
    }
}
