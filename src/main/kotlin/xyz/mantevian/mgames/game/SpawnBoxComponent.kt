package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.util.math.BlockPos
import xyz.mantevian.mgames.server
import xyz.mantevian.mgames.util.*

@Serializable
@SerialName("spawn_box")
class SpawnBoxComponent(
	@SerialName("world")
	val worldId: String = "minecraft:overworld",

	@SerialName("pos")
	val pos: Vec3i = Vec3i(0, -62, 0)
) : GameComponent {
	override fun init() {
		box(pos, 7, 6, "minecraft:bedrock")
		tpPlayersToWorldBottom()
	}

	override fun finish() {
		forEachPlayer {
			teleport(it, worldById(worldId) ?: server.overworld, BlockPos(pos.x, pos.y, pos.z))
		}
	}
}