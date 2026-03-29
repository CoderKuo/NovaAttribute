package com.dakuo.novaattribute.combat

import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.module.nms.sendPacket

/**
 * 1.12 ~ 1.16 伤害指示器 NMS 实现
 * nmsProxy 会自动将 net.minecraft.server.v1_12_R1.* 重映射到当前版本
 *
 * 关键差异：
 * - 使用 PacketPlayOutSpawnEntityLiving（ArmorStand 是 LivingEntity）
 * - 使用 DataWatcher 直接构造元数据
 * - CraftChatMessage 在 v1_12_R1 包路径下
 */
class NMSIndicatorImplLegacy : NMSIndicator() {

    override fun spawn(player: Player, entityId: Int, loc: Location, text: String) {
        val world = (loc.world as org.bukkit.craftbukkit.v1_12_R1.CraftWorld).handle
        val armorStand = net.minecraft.server.v1_12_R1.EntityArmorStand(world, loc.x, loc.y, loc.z)

        armorStand.setInvisible(true)
        armorStand.setCustomNameVisible(true)
        armorStand.setNoGravity(true)
        armorStand.setMarker(true)
        armorStand.setCustomName(text)
        // 1.12 没有公开 setId，用反射设置
        try {
            val f = net.minecraft.server.v1_12_R1.Entity::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.setInt(armorStand, entityId)
        } catch (_: Exception) {
        }

        // 1.12~1.16 使用 PacketPlayOutSpawnEntityLiving
        player.sendPacket(net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityLiving(armorStand))

        // 元数据：1.12~1.16 使用 DataWatcher 构造 PacketPlayOutEntityMetadata
        player.sendPacket(net.minecraft.server.v1_12_R1.PacketPlayOutEntityMetadata(entityId, armorStand.dataWatcher, true))
    }

    override fun destroy(player: Player, entityId: Int) {
        player.sendPacket(net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy(entityId))
    }
}
