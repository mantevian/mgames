package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.level.gamerules.GameRules
import xyz.mantevian.mgames.server

@Serializable
@SerialName("keep_inventory")
class KeepInventoryComponent : GameComponent {
    override fun start() {
        server.gameRules.set(GameRules.KEEP_INVENTORY, true, server)
    }

    override fun reset() {
        server.gameRules.set(GameRules.KEEP_INVENTORY, false, server)
    }
}