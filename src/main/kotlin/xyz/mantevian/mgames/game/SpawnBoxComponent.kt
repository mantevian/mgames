package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.mantevian.mgames.util.Vec3i
import xyz.mantevian.mgames.util.box
import xyz.mantevian.mgames.util.tpPlayers

@Serializable
@SerialName("spawn_box")
class SpawnBoxComponent(
	@SerialName("world")
	val worldId: String = "minecraft:overworld",

	@SerialName("pos")
	val pos: Vec3i = Vec3i(0, -63, 0),

	@SerialName("block")
	val block: String = "minecraft:bedrock"
) : GameComponent {
	override fun init() {
		box(pos, 9, 6, block)
		tpPlayers(pos.up())
	}

	override fun finish() {
		tpPlayers(pos.up())
	}
}