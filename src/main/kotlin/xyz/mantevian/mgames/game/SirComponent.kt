package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.mantevian.mgames.SirPlayer

@Serializable
@SerialName("skyblock_item_randomizer")
class SirComponent : GameComponent {
	@SerialName("life_count")
	var lifeCount: Int = 3

	@SerialName("players")
	val players: MutableList<SirPlayer> = mutableListOf()

	@SerialName("radius")
	var radius: Int = 75
}