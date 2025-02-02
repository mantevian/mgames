package xyz.mantevian.mgames

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

class Main : ModInitializer {
	companion object {
		const val MOD_ID = "mgames"

		var mg: MG? = null

		private lateinit var bingoTasksResource: Resource
	}

	override fun onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register {
			mg = MG(it, load(), bingoTasksResource.get(xyz.mantevian.mgames.bingo.json))
		}

		ServerLifecycleEvents.BEFORE_SAVE.register { _, _, _ ->
			mg?.storage?.let { save(it) }
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			registerCommands(dispatcher)
		}

		bingoTasksResource = Resource("mgames/bingo/tasks.json")
	}
}
