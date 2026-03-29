package com.dakuo.novaattribute.combat

import com.dakuo.novaattribute.combat.DamageContext
import com.dakuo.novaattribute.util.NumberFormatter
import com.dakuo.rulib.common.lang.RandomUtil
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Configuration
import kotlin.random.Random

object DamageIndicator {

    private var enabled = true
    private var offsetY = 1.5
    private var duration = 30L
    private var viewRange = 64.0
    private val formats = mutableMapOf<String, String>()

    fun init(config: Configuration) {
        enabled = config.getBoolean("damage-indicator.enabled", true)
        offsetY = config.getDouble("damage-indicator.offset-y", 1.5)
        duration = config.getLong("damage-indicator.duration", 30)
        viewRange = config.getDouble("damage-indicator.view-range", 64.0)

        formats.clear()
        val formatSection = config.getConfigurationSection("damage-indicator.format")
        if (formatSection != null) {
            for (key in formatSection.getKeys(false)) {
                formats[key] = formatSection.getString(key) ?: ""
            }
        }
        if (formats.isEmpty()) {
            formats["physical"] = "§c-{damage}"
            formats["magic"] = "§b✦ -{damage}"
            formats["true"] = "§4♦ -{damage}"
            formats["critical"] = "§6§l★ -{damage}"
            formats["heal"] = "§a+{damage}"
            formats["miss"] = "§7Miss"
        }
    }

    fun display(ctx: DamageContext) {
        if (!enabled) return

        val text = formatDamage(ctx)
        val loc = ctx.victim.location.clone().add(
            RandomUtil.randomDouble(-0.5, 0.5),
            offsetY + RandomUtil.randomDouble(0.0, 0.3),
            RandomUtil.randomDouble(-0.5, 0.5)
        )

        val entityId = Random.nextInt(100000, 999999)
        val rangeSquared = viewRange * viewRange

        // 向附近玩家发送虚拟实体数据包
        val nearby = loc.world?.players?.filter {
            it.location.distanceSquared(loc) < rangeSquared
        } ?: return

        for (player in nearby) {
            NMSIndicator.INSTANCE.spawn(player, entityId, loc, text)
        }

        // 延迟后销毁虚拟实体
        submit(delay = duration) {
            for (player in nearby) {
                if (player.isOnline) {
                    NMSIndicator.INSTANCE.destroy(player, entityId)
                }
            }
        }
    }

    private fun formatDamage(ctx: DamageContext): String {
        val damageType = ctx.getProperty("damageType") as? String ?: "physical"
        val isCritical = ctx.getProperty("isCritical") as? Boolean ?: false

        val template = if (isCritical) {
            formats["critical"] ?: "§6§l★ -{damage}"
        } else {
            formats[damageType] ?: formats["physical"] ?: "§c-{damage}"
        }

        return template.replace("{damage}", NumberFormatter.format(ctx.finalDamage))
    }
}
