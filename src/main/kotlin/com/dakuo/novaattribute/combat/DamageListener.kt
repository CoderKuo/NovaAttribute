package com.dakuo.novaattribute.combat

import com.dakuo.novaattribute.api.event.NovaAttributeDamageEvent
import com.dakuo.novaattribute.core.attribute.AttributeTrigger
import com.dakuo.novaattribute.combat.DamageIndicator
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.metadata.FixedMetadataValue
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.platform.BukkitPlugin

object DamageListener {

    private const val CTX_KEY = "nova:damage_ctx"
    private const val BYPASS_KEY = "nova:bypass"

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onDamageLow(e: EntityDamageByEntityEvent) {
        val victim = e.entity as? LivingEntity ?: return

        // 检查 bypass 标记（attackTo 发出的伤害）
        if (victim.hasMetadata(BYPASS_KEY)) {
            victim.removeMetadata(BYPASS_KEY, BukkitPlugin.getInstance())
            return
        }

        val attacker = getRealAttacker(e) ?: return

        val ctx = DamageContext(
            attacker = attacker,
            victim = victim,
            cause = e.cause,
            originalDamage = e.damage,
            finalDamage = e.damage,
            isProjectile = e.damager is Projectile
        )

        victim.setMetadata(CTX_KEY, FixedMetadataValue(BukkitPlugin.getInstance(), ctx))
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onDamageHigh(e: EntityDamageByEntityEvent) {
        val victim = e.entity as? LivingEntity ?: return

        val metaList = victim.getMetadata(CTX_KEY)
        val ctx = metaList.firstOrNull()?.value() as? DamageContext ?: return
        victim.removeMetadata(CTX_KEY, BukkitPlugin.getInstance())

        if (e.isCancelled) return

        // ① 调用 damage.nova 计算基础伤害
        ctx.finalDamage = CombatHandler.calculateDamage(ctx)

        // ② 执行 ATTACK 类属性脚本
        CombatHandler.executeAttributeScripts(ctx, AttributeTrigger.ATTACK)

        // ③ 执行 DEFENSE 类属性脚本
        CombatHandler.executeAttributeScripts(ctx, AttributeTrigger.DEFENSE)

        // ③.5 执行 SX2 兼容属性 + 触发 SXDamageEvent
        try {
            com.dakuo.novaattribute.compat.sx2.SX2Bridge.executeDamageAttributes(ctx)
        } catch (_: NoClassDefFoundError) {
        }

        // ④ 抛出 NovaAttributeDamageEvent
        val damageEvent = NovaAttributeDamageEvent(ctx.attacker, ctx.victim, ctx, ctx.finalDamage)
        damageEvent.call()
        if (damageEvent.isCancelled) {
            e.isCancelled = true
            return
        }
        ctx.finalDamage = damageEvent.finalDamage

        // ⑤ 显示伤害指示器 + 发送战斗消息
        DamageIndicator.display(ctx)
        CombatMessage.send(ctx)

        // ⑥ 应用最终伤害
        e.damage = ctx.finalDamage

        // ⑦ 如果击杀，执行 KILL 类属性脚本
        if (victim.health - ctx.finalDamage <= 0) {
            CombatHandler.executeAttributeScripts(ctx, AttributeTrigger.KILL)
        }
    }

    private fun getRealAttacker(e: EntityDamageByEntityEvent): LivingEntity? {
        val damager = e.damager
        if (damager is LivingEntity) return damager
        if (damager is Projectile) {
            val shooter = damager.shooter
            if (shooter is LivingEntity) return shooter
        }
        return null
    }
}
