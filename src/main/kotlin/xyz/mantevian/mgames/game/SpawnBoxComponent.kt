package xyz.mantevian.mgames.game

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.core.Vec3i
import xyz.mantevian.mgames.util.box
import xyz.mantevian.mgames.util.tpPlayers

@Serializable
@SerialName("spawn_box")
class SpawnBoxComponent(
    @SerialName("world")
    val worldId: String = "minecraft:overworld",

    @SerialName("pos")
    @Contextual
    val pos: Vec3i = Vec3i(0, -63, 0),

    @SerialName("block")
    val block: String = "minecraft:bedrock"
) : GameComponent {
    override fun init() {
        box(pos, 9, 6, block)
        tpPlayers(Vec3i(pos.x, pos.y + 1, pos.z))
    }

    override fun finish() {
        tpPlayers(Vec3i(pos.x, pos.y + 1, pos.z))
    }
}