package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.mantevian.mgames.util.MGDuration
import xyz.mantevian.mgames.util.hours

@Serializable
@SerialName("game_time")
class GameTimeComponent(
	val value: MGDuration = 1.hours()
) : GameComponent