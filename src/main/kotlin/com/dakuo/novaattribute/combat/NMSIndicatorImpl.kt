package com.dakuo.novaattribute.combat

import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.module.nms.sendPacket

class NMSIndicatorImpl : NMSIndicator() {

    override fun spawn(player: Player, entityId: Int, loc: Location, text: String) {
        val world = (loc.world as NMSCraftWorld).handle
        val armorStand = NMSEntityArmorStand(world, loc.x, loc.y, loc.z)

        armorStand.setInvisible(true)
        armorStand.setCustomNameVisible(true)
        armorStand.setNoGravity(true)
        armorStand.setMarker(true)
        armorStand.setCustomName(NMSCraftChatMessage.fromStringOrNull(text))
        armorStand.setId(entityId)

        // 发送生成数据包（全参数构造，兼容 1.21+）
        player.sendPacket(NMSPacketSpawnEntity(
            entityId,
            armorStand.getUUID(),
            loc.x,
            loc.y,
            loc.z,
            0f,
            0f,
            armorStand.getType(),
            0,
            NMSVec3D(0.0, 0.0, 0.0),
            0.0
        ))

        // 发送元数据包（1.19.3+ 使用 getNonDefaultValues）
        val packedItems = armorStand.entityData.getNonDefaultValues() ?: return
        player.sendPacket(NMSPacketMetadata(entityId, packedItems))
    }

    override fun destroy(player: Player, entityId: Int) {
        player.sendPacket(NMSPacketDestroy(entityId))
    }
}

private typealias NMSEntityArmorStand = net.minecraft.world.entity.decoration.EntityArmorStand
private typealias NMSCraftWorld = org.bukkit.craftbukkit.v1_20_R3.CraftWorld
private typealias NMSCraftChatMessage = org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage
private typealias NMSPacketSpawnEntity = net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity
private typealias NMSPacketDestroy = net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy
private typealias NMSPacketMetadata = net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata
private typealias NMSVec3D = net.minecraft.world.phys.Vec3D
