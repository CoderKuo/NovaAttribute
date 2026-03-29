package com.dakuo.novaattribute.combat

import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket

/**
 * 1.17+ 伤害指示器 NMS 实现
 * nmsProxy 会自动将 net.minecraft.* 类路径重映射到当前版本
 */
class NMSIndicatorImpl : NMSIndicator() {

    override fun spawn(player: Player, entityId: Int, loc: Location, text: String) {
        val world = (loc.world as org.bukkit.craftbukkit.v1_20_R3.CraftWorld).handle
        val armorStand = net.minecraft.world.entity.decoration.EntityArmorStand(world, loc.x, loc.y, loc.z)

        armorStand.setInvisible(true)
        armorStand.setCustomNameVisible(true)
        armorStand.setNoGravity(true)
        armorStand.setMarker(true)
        armorStand.setCustomName(org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage.fromStringOrNull(text))
        armorStand.setId(entityId)

        // 生成数据包
        player.sendPacket(net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity(armorStand))

        // 元数据包
        try {
            // 1.19.3+ 使用 getNonDefaultValues
            val packedItems = armorStand.entityData.getNonDefaultValues()
            if (packedItems != null) {
                player.sendPacket(net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata(entityId, packedItems))
            }
        } catch (e: NoSuchMethodError) {
            // 1.17 ~ 1.19.2 使用 packDirty
            try {
                val dirty = armorStand.entityData.packDirty()
                if (dirty != null) {
                    player.sendPacket(net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata(entityId, dirty))
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun destroy(player: Player, entityId: Int) {
        player.sendPacket(net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy(entityId))
    }
}
