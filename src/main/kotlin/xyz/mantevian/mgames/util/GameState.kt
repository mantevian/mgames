package xyz.mantevian.mgames.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameState {
	@SerialName("not_init")
	NOT_INIT,

	@SerialName("waiting")
	WAITING,

	@SerialName("playing")
	PLAYING
}