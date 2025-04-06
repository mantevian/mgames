package xyz.mantevian.mgames

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.MinecraftServer
import xyz.mantevian.mgames.game.Game
import xyz.mantevian.mgames.util.*

const val MOD_ID = "mgames"
lateinit var server: MinecraftServer
lateinit var resourceManager: ResourceManager
var initialized = false
var game = Game()

class Main : ModInitializer {
	override fun onInitialize() {
		resourceManager = ResourceManager()

		ServerLifecycleEvents.SERVER_STARTING.register { s ->
			server = s
			initialized = true

			game = load()
		}

		ServerLifecycleEvents.BEFORE_SAVE.register { _, _, _ ->
			if (initialized) {
				save()
			}
		}

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ ->
			if (!initialized) {
				return@register
			}
		}

		ServerTickEvents.END_SERVER_TICK.register {
			tick()
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, env ->
			registerCommands(dispatcher, registryAccess, env)
		}

		MGItems.init()
	}

	private fun tick() {
		when (game.state) {
			GameState.WAITING -> {
				infiniteEffectForEveryone(StatusEffects.RESISTANCE)
				infiniteEffectForEveryone(StatusEffects.SATURATION)
				infiniteEffectForEveryone(StatusEffects.NIGHT_VISION)
			}

			GameState.PLAYING -> {
				game.time.inc()
				forEachPlayer {
					if (game.time.getTicks() >= 0) {
						it.sendMessageToClient(standardText(game.time.formatHourMinSec()), true)
					}
				}
			}

			else -> {}
		}

		game.tick()
	}
}

fun startGame() {
	if (!game.canStart()) {
		return
	}

	game.start()

	game.state = GameState.PLAYING
}