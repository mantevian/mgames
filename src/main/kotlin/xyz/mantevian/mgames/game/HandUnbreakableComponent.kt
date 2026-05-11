package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Unit
import xyz.mantevian.mgames.util.forEachPlayer

@Serializable
@SerialName("unbreakable")
class HandUnbreakableComponent : GameComponent {
    override fun tick() {
        forEachPlayer { player ->
            val mainhand = player.mainHandItem
            if (mainhand.get(DataComponents.MAX_DAMAGE) != null) {
                mainhand.set(DataComponents.UNBREAKABLE, Unit.INSTANCE)
            }
        }
    }
}