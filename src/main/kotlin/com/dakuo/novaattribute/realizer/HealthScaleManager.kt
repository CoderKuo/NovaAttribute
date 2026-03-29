package com.dakuo.novaattribute.realizer

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.module.configuration.Configuration

/**
 * 血量条缩放管理器
 *
 * RPG 服务器中玩家血量可能达到成百上千，原版血条会显示异常。
 * 通过 healthScale 将血条固定为指定刻度（默认 20.0 = 10 颗心），
 * 血条按百分比显示当前血量。
 */
object HealthScaleManager {

    private var enabled = false
    private var scale = 20.0

    fun init(config: Configuration) {
        enabled = config.getBoolean("health-scale.enabled", false)
        scale = config.getDouble("health-scale.value", 20.0)
        if (enabled) {
            // 对已在线的玩家应用
            Bukkit.getOnlinePlayers().forEach(::apply)
            info("[NovaAttribute] Health scale enabled (scale=$scale).")
        }
    }

    fun isEnabled(): Boolean = enabled

    fun getScale(): Double = scale

    /**
     * 设置血量条缩放值（可通过 API/脚本动态调用）
     */
    fun setScale(player: Player, value: Double) {
        player.isHealthScaled = true
        player.healthScale = value.coerceAtLeast(1.0)
    }

    /**
     * 对玩家应用默认缩放
     */
    fun apply(player: Player) {
        if (!enabled) return
        player.isHealthScaled = true
        player.healthScale = scale
    }

    /**
     * 插件关闭时恢复所有在线玩家的血条
     */
    fun disable() {
        if (!enabled) return
        for (player in Bukkit.getOnlinePlayers()) {
            player.isHealthScaled = false
        }
    }

    @SubscribeEvent
    fun onJoin(e: PlayerJoinEvent) {
        apply(e.player)
    }

    @SubscribeEvent
    fun onRespawn(e: PlayerRespawnEvent) {
        apply(e.player)
    }
}
