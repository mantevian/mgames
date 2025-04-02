package xyz.mantevian.mgames

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.MinecraftServer
import xyz.mantevian.mgames.bingo.BingoGame
import xyz.mantevian.mgames.bingo.BingoTaskSourceSet

class MG(
	val server: MinecraftServer,
	val storage: MGStorage,
	bingoTaskSourceSet: BingoTaskSourceSet,
	bingoSplashes: List<String>
) {
	val bingo = BingoGame(this, bingoTaskSourceSet, bingoSplashes)
	val util = MGUtil(this)
	val worldgen = MGWorldgen(this)

	init {
		ServerTickEvents.END_SERVER_TICK.register { tick() }
	}

	fun executeCommand(command: String) {
		server.commandManager.executeWithPrefix(server.commandSource, command)
	}

	fun initGame(game: GameType) {
		storage.game = game
		storage.state = GameState.WAITING

		when (game) {
			GameType.BINGO -> bingo.init()
		}

		util.resetPlayersMinecraftStats()
	}

	fun startGame() {
		val started = when (storage.game) {
			GameType.BINGO -> bingo.start()
			else -> false
		}

		if (!started) {
			return
		}

		storage.state = GameState.PLAYING
	}

	private fun tick() {
		when (storage.state) {
			GameState.WAITING -> {
				util.infiniteEffectForEveryone(StatusEffects.RESISTANCE)
				util.infiniteEffectForEveryone(StatusEffects.SATURATION)
				util.infiniteEffectForEveryone(StatusEffects.NIGHT_VISION)
			}

			GameState.PLAYING -> {
				storage.time.inc()
				util.forEachPlayer {
					if (storage.time.getTicks() >= 0) {
						it.sendMessageToClient(standardText(storage.time.formatHourMinSec()), true)
					}
				}
			}

			else -> {}
		}
		when (storage.game) {
			GameType.BINGO -> bingo.tick()
			else -> {}
		}
	}
}