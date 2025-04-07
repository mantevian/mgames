package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.mantevian.mgames.game
import xyz.mantevian.mgames.server
import xyz.mantevian.mgames.util.GameState

@Serializable
@SerialName("world_size")
class WorldSizeComponent(var value: Int = 10000) : GameComponent {
	override fun start() {
		update(this.value)
	}

	fun update(value: Int) {
		this.value = value

		if (game.state == GameState.PLAYING) {
			server.worlds.forEach {
				it.worldBorder.size = value.toDouble() * 2.0
			}
		}
	}
}