package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.component.DataComponentTypes
import net.minecraft.util.Hand
import net.minecraft.util.Unit
import xyz.mantevian.mgames.util.forEachPlayer

@Serializable
@SerialName("unbreakable")
class HandUnbreakableComponent : GameComponent {
	override fun tick() {
		forEachPlayer { player ->
			val mainhand = player.getStackInHand(Hand.MAIN_HAND)
			if (mainhand.get(DataComponentTypes.MAX_DAMAGE) != null) {
				mainhand.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE)
			}
		}
	}
}