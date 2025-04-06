package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.mantevian.mgames.server

@Serializable
@SerialName("world_size")
class WorldSizeComponent(var value: Int = 10000) : GameComponent {
	override fun start() {
		server.worlds.forEach {
			it.worldBorder.size = value.toDouble()
		}
	}
}