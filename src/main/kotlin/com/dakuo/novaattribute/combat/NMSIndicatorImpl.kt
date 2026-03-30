package com.dakuo.novaattribute.combat

import org.bukkit.Location
import org.bukkit.entity.Player
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

        // 生成数据包：尝试单参数构造（1.17~1.20），失败则用全参数构造（1.21+）
        val spawnPacket = try {
            net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity(armorStand)
        } catch (_: NoSuchMethodError) {
            net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity(
                entityId,
                armorStand.getUUID(),
                loc.x,
                loc.y,
                loc.z,
                0f,
                0f,
                armorStand.getType(),
                0,
                net.minecraft.world.phys.Vec3D(0.0, 0.0, 0.0),
                0.0
            )
        }
        player.sendPacket(spawnPacket)

        // 元数据包
        try {
            // 1.19.3+ 使用 getNonDefaultValues
            val packedItems = armorStand.entityData.getNonDefaultValues()
            if (packedItems != null) {
                player.sendPacket(net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata(entityId, packedItems))
            }
        } catch (_: NoSuchMethodError) {
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
