package com.dakuo.novaattribute.script

import com.dakuo.novascript.ScriptSetup
import com.dakuo.novaattribute.core.attribute.AttributeManager
import com.dakuo.novaattribute.core.cooldown.CooldownManager
import com.dakuo.novaattribute.core.counter.CounterManager
import com.dakuo.novaattribute.feature.mob.MobElementManager
import com.dakuo.novaattribute.trigger.TriggerManager
import com.dakuo.rulib.common.lang.Duration
import com.dakuo.rulib.common.lang.NumberFormat
import com.dakuo.rulib.common.lang.RandomUtil
import com.dakuo.rulib.common.lang.WeightRandom
import com.dakuo.rulib.common.lang.formatDuration
import com.dakuo.rulib.common.item.matcher.ItemTextMatcher
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.function.submit
import taboolib.platform.BukkitPlugin
import taboolib.platform.compat.replacePlaceholder

object ScriptBinding {

    fun configure(setup: ScriptSetup) {

        // ====== 属性查询 ======

        setup.defineFunction("getAttr") { entity, attrId ->
            val e = entity as? LivingEntity ?: return@defineFunction 0.0
            AttributeManager.get(e).get(attrId as String)
        }

        setup.defineFunction("getAttrRandom") { entity, attrId ->
            val e = entity as? LivingEntity ?: return@defineFunction 0.0
            AttributeManager.get(e).getRandom(attrId as String)
        }

        setup.defineFunction("getAttrMin") { entity, attrId ->
            val e = entity as? LivingEntity ?: return@defineFunction 0.0
            AttributeManager.get(e).getMin(attrId as String)
        }

        setup.defineFunction("getAttrMax") { entity, attrId ->
            val e = entity as? LivingEntity ?: return@defineFunction 0.0
            AttributeManager.get(e).getMax(attrId as String)
        }

        // ====== 实体工具 ======

        setup.defineFunction("getNearbyEntities") { entity, radius ->
            val e = entity as? LivingEntity ?: return@defineFunction emptyList<LivingEntity>()
            val r = (radius as Number).toDouble()
            e.getNearbyEntities(r, r, r).filterIsInstance<LivingEntity>()
        }

        setup.defineFunction("safeHeal") { entity, amount ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            val heal = (amount as Number).toDouble()
            e.health = (e.health + heal).coerceIn(0.0, e.maxHealth)
            null
        }

        setup.defineFunction("isType") { entity, type ->
            val e = entity as? LivingEntity ?: return@defineFunction false
            e.type.name.equals(type as String, ignoreCase = true)
        }

        setup.defineFunction("isAlive") { entity ->
            val e = entity as? LivingEntity ?: return@defineFunction false
            !e.isDead
        }

        setup.defineFunction("isPlayer") { entity ->
            entity is Player
        }

        // ====== 类型转换 ======

        setup.defineFunction("toNumber") { value ->
            when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        }

        // ====== PAPI (数值) ======

        setup.defineFunction("placeholderDouble") { player, text, defaultValue ->
            val p = player as? Player ?: return@defineFunction (defaultValue as? Number)?.toDouble() ?: 0.0
            val result = (text as String).replacePlaceholder(p)
            result.toDoubleOrNull() ?: (defaultValue as? Number)?.toDouble() ?: 0.0
        }

        // ====== 概率 / 数学 ======

        setup.defineFunction("chance") { probability ->
            RandomUtil.randomDouble(0.0, 1.0) < (probability as Number).toDouble()
        }

        setup.defineFunction("random") { min, max ->
            RandomUtil.randomDouble((min as Number).toDouble(), (max as Number).toDouble())
        }

        setup.defineFunction("randomInt") { min, max ->
            RandomUtil.randomInt((min as Number).toInt(), (max as Number).toInt())
        }

        setup.defineFunction("max") { a, b ->
            maxOf((a as Number).toDouble(), (b as Number).toDouble())
        }

        setup.defineFunction("min") { a, b ->
            minOf((a as Number).toDouble(), (b as Number).toDouble())
        }

        setup.defineFunction("clamp") { value, min, max ->
            (value as Number).toDouble().coerceIn(
                (min as Number).toDouble(),
                (max as Number).toDouble()
            )
        }

        // ====== 权重随机 ======

        setup.defineFunctionVararg("weightRandom") { args ->
            // weightRandom("itemA", 10, "itemB", 30, "itemC", 60)
            val wr = WeightRandom.create<Any>()
            var i = 0
            while (i + 1 < args.size) {
                val obj = args[i] ?: run { i += 2; continue }
                val weight = (args[i + 1] as? Number)?.toDouble() ?: 0.0
                wr.add(obj, weight)
                i += 2
            }
            wr.next()
        }

        // ====== 物品匹配 ======

        setup.defineFunction("matchItem") { item, expression ->
            val stack = item as? ItemStack ?: return@defineFunction false
            ItemTextMatcher().matches(stack, expression as String)
        }

        // ====== 冷却 ======

        setup.defineFunction("cooldownReady") { entity, key ->
            val e = entity as? LivingEntity ?: return@defineFunction false
            CooldownManager.isReady(e, key as String)
        }

        setup.defineFunction("setCooldown") { entity, key, ticks ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            CooldownManager.set(e, key as String, (ticks as Number).toLong())
            null
        }

        // ====== 计数器 ======

        setup.defineFunction("increment") { entity, key, amount ->
            val e = entity as? LivingEntity ?: return@defineFunction 0
            CounterManager.increment(e, key as String, (amount as? Number)?.toInt() ?: 1)
        }

        setup.defineFunction("getCounter") { entity, key ->
            val e = entity as? LivingEntity ?: return@defineFunction 0
            CounterManager.get(e, key as String)
        }

        setup.defineFunction("resetCounter") { entity, key ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            CounterManager.reset(e, key as String)
            null
        }

        // ====== 消息 ======

        setup.defineFunction("sendMessage") { entity, message ->
            (entity as? Player)?.sendMessage(message as String)
            null
        }


        setup.defineFunctionVararg("sendTitle") { args ->
            val player = args.getOrNull(0) as? Player ?: return@defineFunctionVararg null
            val title = args.getOrNull(1) as? String ?: ""
            val subtitle = args.getOrNull(2) as? String ?: ""
            val fadeIn = (args.getOrNull(3) as? Number)?.toInt() ?: 10
            val stay = (args.getOrNull(4) as? Number)?.toInt() ?: 70
            val fadeOut = (args.getOrNull(5) as? Number)?.toInt() ?: 20
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut)
            null
        }

        setup.defineFunction("playSound") { entity, sound, volume, pitch ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            try {
                val s = Sound.valueOf((sound as String).uppercase())
                e.world.playSound(e.location, s, (volume as Number).toFloat(), (pitch as Number).toFloat())
            } catch (_: Exception) {
            }
            null
        }

        // ====== 物品构建 ======

        setup.defineFunction("buildItem") { material, amount ->
            val mat = org.bukkit.Material.matchMaterial(material as String)
                ?: return@defineFunction null
            ItemStack(mat, (amount as Number).toInt().coerceAtLeast(1))
        }

        setup.defineFunction("setItemName") { item, name ->
            val stack = item as? ItemStack ?: return@defineFunction item
            val meta = stack.itemMeta ?: return@defineFunction stack
            meta.setDisplayName(name as String)
            stack.itemMeta = meta
            stack
        }

        setup.defineFunction("setItemLore") { item, lore ->
            val stack = item as? ItemStack ?: return@defineFunction item
            val meta = stack.itemMeta ?: return@defineFunction stack
            @Suppress("UNCHECKED_CAST")
            meta.lore = (lore as? List<*>)?.map { it.toString() }
            stack.itemMeta = meta
            stack
        }

        // ====== 战斗机制 ======

        setup.defineFunction("getAttackCooldown") { entity ->
            (entity as? Player)?.attackCooldown?.toDouble() ?: 1.0
        }

        // ====== 效果链（属性脚本间通信） ======

        setup.defineFunction("markTriggered") { ctx, name ->
            val c = ctx as? com.dakuo.novaattribute.combat.DamageContext ?: return@defineFunction null
            c.setProperty("triggered:${name as String}", true)
            null
        }

        setup.defineFunction("isTriggered") { ctx, name ->
            val c = ctx as? com.dakuo.novaattribute.combat.DamageContext ?: return@defineFunction false
            c.getProperty("triggered:${name as String}") == true
        }

        // ====== 伤害控制 ======

        setup.defineFunction("attackTo") { attacker, victim, damage ->
            val a = attacker as? LivingEntity ?: return@defineFunction null
            val v = victim as? LivingEntity ?: return@defineFunction null
            v.setMetadata("nova:bypass", FixedMetadataValue(BukkitPlugin.getInstance(), true))
            v.damage((damage as Number).toDouble(), a)
            null
        }

        // ====== 状态效果 ======

        setup.defineFunction("applyEffect") { entity, effectName, duration, amplifier ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            val type = PotionEffectType.getByName((effectName as String).uppercase())
                ?: return@defineFunction null
            e.addPotionEffect(PotionEffect(type, (duration as Number).toInt(), (amplifier as Number).toInt()))
            null
        }

        setup.defineFunction("removeEffect") { entity, effectName ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            val type = PotionEffectType.getByName((effectName as String).uppercase())
                ?: return@defineFunction null
            e.removePotionEffect(type)
            null
        }

        setup.defineFunction("hasEffect") { entity, effectName ->
            val e = entity as? LivingEntity ?: return@defineFunction false
            val type = PotionEffectType.getByName((effectName as String).uppercase())
                ?: return@defineFunction false
            e.hasPotionEffect(type)
        }

        setup.defineFunction("setFire") { entity, ticks ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            e.fireTicks = (ticks as Number).toInt()
            null
        }

        setup.defineFunction("strikeLightning") { entity ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            e.world.strikeLightningEffect(e.location)
            null
        }

        setup.defineFunction("knockback") { target, source, strength ->
            val t = target as? LivingEntity ?: return@defineFunction null
            val s = source as? LivingEntity ?: return@defineFunction null
            val str = (strength as Number).toDouble()
            val direction = t.location.toVector().subtract(s.location.toVector()).normalize().multiply(str)
            direction.setY(0.4)
            t.velocity = direction
            null
        }

        // ====== 经验 ======

        setup.defineFunction("giveExp") { player, amount ->
            val p = player as? Player ?: return@defineFunction null
            p.giveExp((amount as Number).toInt())
            null
        }

        // ====== 怪物元素 ======

        setup.defineFunction("getMobWeakness") { entity ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            MobElementManager.getWeakness(e)
        }

        // ====== 延迟任务 ======

        setup.defineFunction("delay") { ticks, callback ->
            val t = (ticks as Number).toLong()
            submit(delay = t) {
                if (callback != null) {
                    try {
                        // 找一个已加载的脚本作为上下文来执行回调
                        val scripts = com.dakuo.novascript.NovaScriptAPI.getScripts("NovaAttribute")
                        val ns = scripts.firstOrNull()?.let { "NovaAttribute:$it" } ?: return@submit
                        com.dakuo.novascript.NovaScriptAPI.invokeCallback(ns, callback)
                    } catch (_: Exception) {
                    }
                }
            }
            null
        }

        // ====== PAPI ======

        setup.defineFunction("placeholder") { player, text ->
            val p = player as? Player ?: return@defineFunction text
            (text as String).replacePlaceholder(p)
        }

        // ====== CUSTOM 触发器 ======

        setup.defineFunction("triggerCustom") { entity, attrId ->
            val e = entity as? LivingEntity ?: return@defineFunction null
            TriggerManager.triggerCustomAttribute(e, attrId as String)
            null
        }

        setup.defineFunctionVararg("triggerCustomEx") { args ->
            val entity = args.getOrNull(0) as? LivingEntity ?: return@defineFunctionVararg null
            val attrId = args.getOrNull(1) as? String ?: return@defineFunctionVararg null
            val target = args.getOrNull(2) as? LivingEntity
            @Suppress("UNCHECKED_CAST")
            val params = args.getOrNull(3) as? Map<String, Any> ?: emptyMap()
            TriggerManager.triggerCustomAttribute(entity, attrId, target, params)
            null
        }

        // ====== Rulib 格式化工具 ======

        setup.defineFunction("formatShort") { value ->
            NumberFormat.formatShort((value as Number).toDouble())
        }

        setup.defineFunction("formatGrouped") { value ->
            NumberFormat.formatGrouped((value as Number).toDouble())
        }

        setup.defineFunction("formatPercent") { value, digits ->
            NumberFormat.formatPercent((value as Number).toDouble(), (digits as? Number)?.toInt() ?: 1)
        }

        setup.defineFunction("toRoman") { value ->
            NumberFormat.toRoman((value as Number).toInt())
        }

        setup.defineFunction("parseDuration") { str ->
            Duration.parse(str as String).toMillis()
        }

        setup.defineFunction("formatDuration") { millis ->
            (millis as Number).toLong().formatDuration()
        }
    }
}
