package com.dakuo.novaattribute.listener

import com.dakuo.novaattribute.api.event.RefreshCause
import com.dakuo.novaattribute.core.attribute.AttributeManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockDispenseArmorEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object EquipmentListener {

    // 装备内容哈希缓存，用于检测物品内容变更（如其他插件修改 Lore）
    private val equipmentHashes = ConcurrentHashMap<UUID, Int>()
    private var checkTask: PlatformExecutor.PlatformTask? = null

    // ====== 事件监听（物品位置变更）======

    @SubscribeEvent
    fun onInventoryClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        // 任何可能影响装备栏的点击都触发刷新
        // 包括：盔甲栏、快捷栏、Shift点击、数字键切换、拖拽等
        scheduleRefresh(player)
    }

    @SubscribeEvent
    fun onInventoryClose(e: org.bukkit.event.inventory.InventoryCloseEvent) {
        val player = e.player as? Player ?: return
        scheduleRefresh(player)
    }

    @SubscribeEvent
    fun onItemHeld(e: PlayerItemHeldEvent) {
        scheduleRefresh(e.player)
    }

    @SubscribeEvent
    fun onSwapHand(e: PlayerSwapHandItemsEvent) {
        scheduleRefresh(e.player)
    }

    @SubscribeEvent
    fun onDrop(e: PlayerDropItemEvent) {
        scheduleRefresh(e.player)
    }

    @SubscribeEvent
    fun onRespawn(e: PlayerRespawnEvent) {
        scheduleRefresh(e.player)
    }

    @SubscribeEvent
    fun onDispenseArmor(e: BlockDispenseArmorEvent) {
        val entity = e.targetEntity as? Player ?: return
        scheduleRefresh(entity)
    }

    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        equipmentHashes.remove(e.player.uniqueId)
    }

    // ====== 装备内容变更检测 ======

    fun startContentCheck(intervalTicks: Long = 20L) {
        checkTask?.cancel()
        checkTask = submit(period = intervalTicks) {
            for (player in Bukkit.getOnlinePlayers()) {
                val hash = computeEquipmentHash(player)
                val prev = equipmentHashes.put(player.uniqueId, hash)
                if (prev != null && prev != hash) {
                    AttributeManager.refresh(player, RefreshCause.EQUIPMENT_CHANGE)
                }
            }
        }
    }

    private fun computeEquipmentHash(player: Player): Int {
        val inv = player.inventory
        var hash = inv.itemInMainHand.hashCode()
        hash = 31 * hash + inv.itemInOffHand.hashCode()
        hash = 31 * hash + (inv.helmet?.hashCode() ?: 0)
        hash = 31 * hash + (inv.chestplate?.hashCode() ?: 0)
        hash = 31 * hash + (inv.leggings?.hashCode() ?: 0)
        hash = 31 * hash + (inv.boots?.hashCode() ?: 0)
        return hash
    }

    // ====== 内部 ======

    private fun scheduleRefresh(player: Player) {
        submit(delay = 1L) {
            if (player.isOnline) {
                // 更新哈希缓存，避免定时检测再次触发重复刷新
                equipmentHashes[player.uniqueId] = computeEquipmentHash(player)
                AttributeManager.refresh(player, RefreshCause.EQUIPMENT_CHANGE)
            }
        }
    }
}
