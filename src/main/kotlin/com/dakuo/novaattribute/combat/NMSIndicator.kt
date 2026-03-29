package com.dakuo.novaattribute.combat

import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.util.unsafeLazy
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.nmsProxy

abstract class NMSIndicator {

    abstract fun spawn(player: Player, entityId: Int, location: Location, text: String)

    abstract fun destroy(player: Player, entityId: Int)

    companion object {
        val INSTANCE by unsafeLazy {
            if (MinecraftVersion.isUniversal) {
                // 1.17+
                nmsProxy<NMSIndicator>()
            } else {
                // 1.12 ~ 1.16
                nmsProxy<NMSIndicator>("{name}ImplLegacy")
            }
        }
    }
}
