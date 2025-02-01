package xyz.mantevian.mgames

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.MinecraftServer
import xyz.mantevian.mgames.bingo.BingoGame

class MG(val server: MinecraftServer, val storage: MGStorage) {
	val bingo = BingoGame(this)
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
		storage.state = GameState.PLAYING
		storage.time.set(0)

		when (storage.game) {
			GameType.BINGO -> bingo.start()
			else -> {}
		}
	}

	private fun tick() {
		when (storage.state) {
			GameState.WAITING -> {
				util.infiniteEffect(StatusEffects.RESISTANCE, 4)
				util.infiniteEffect(StatusEffects.SATURATION)
				util.infiniteEffect(StatusEffects.NIGHT_VISION)
			}
			GameState.PLAYING -> {
				storage.time.inc()
			}
			else -> {}
		}
		when (storage.game) {
			GameType.BINGO -> bingo.tick()
			else -> {}
		}
	}
}