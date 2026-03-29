package com.dakuo.novaattribute.combat

import com.dakuo.novaattribute.util.NumberFormatter
import org.bukkit.entity.Player
import taboolib.module.configuration.Configuration

object CombatMessage {

    private var enabled = true
    private var attackerFormat = ""
    private var victimFormat = ""
    private var criticalAttackerFormat = ""
    private var missAttackerFormat = ""
    private var missVictimFormat = ""

    fun init(config: Configuration) {
        enabled = config.getBoolean("combat.message.enabled", true)
        attackerFormat = config.getString("combat.message.attacker", "§7你对 §f{victim} §7造成了 §c{damage} §7点伤害") ?: ""
        victimFormat = config.getString("combat.message.victim", "§7你受到了 §f{attacker} §7的 §c{damage} §7点伤害") ?: ""
        criticalAttackerFormat = config.getString("combat.message.critical", "§6✦暴击！§7你对 §f{victim} §7造成了 §c{damage} §7点伤害") ?: ""
        missAttackerFormat = config.getString("combat.message.miss-attacker", "§7{victim} §a闪避了§7你的攻击") ?: ""
        missVictimFormat = config.getString("combat.message.miss-victim", "§a你闪避了 §f{attacker} §a的攻击") ?: ""
    }

    fun send(ctx: DamageContext) {
        if (!enabled) return

        val damageType = ctx.getProperty("damageType") as? String ?: "physical"
        val isCritical = ctx.getProperty("isCritical") as? Boolean ?: false
        val isMiss = damageType == "miss"
        val damage = NumberFormatter.format(ctx.finalDamage)

        // 攻击者消息
        val attacker = ctx.attacker
        if (attacker is Player) {
            val msg = when {
                isMiss -> missAttackerFormat
                isCritical -> criticalAttackerFormat
                else -> attackerFormat
            }
            if (msg.isNotEmpty()) {
                attacker.sendMessage(replacePlaceholders(msg, ctx, damage))
            }
        }

        // 受击者消息
        val victim = ctx.victim
        if (victim is Player) {
            val msg = if (isMiss) missVictimFormat else victimFormat
            if (msg.isNotEmpty()) {
                victim.sendMessage(replacePlaceholders(msg, ctx, damage))
            }
        }
    }

    private fun replacePlaceholders(template: String, ctx: DamageContext, damage: String): String {
        return template
            .replace("{attacker}", ctx.attacker.name)
            .replace("{victim}", ctx.victim.name)
            .replace("{damage}", damage)
    }

}
