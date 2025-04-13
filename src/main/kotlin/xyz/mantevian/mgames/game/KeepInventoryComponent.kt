package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.mantevian.mgames.util.executeCommand

@Serializable
@SerialName("keep_inventory")
class KeepInventoryComponent : GameComponent {
	override fun start() {
		executeCommand("gamerule keepInventory true")
	}

	override fun reset() {
		executeCommand("gamerule keepInventory false")
	}
}