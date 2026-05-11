package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.util.forEachPlayer

@Serializable
@SerialName("mining_efficiency")
class PlayersMiningEfficiencyComponent(var value: Double = 0.0) : GameComponent {
    override fun tick() {
        forEachPlayer { player ->
            player.attributes
                .getInstance(Attributes.MINING_EFFICIENCY)
                ?.addOrReplacePermanentModifier(
                    AttributeModifier(
                        Identifier.parse("mantevian:$MOD_ID/mining_efficiency"),
                        value,
                        AttributeModifier.Operation.ADD_VALUE
                    )
                )
        }
    }
}