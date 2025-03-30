package xyz.mantevian.mgames

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import xyz.mantevian.mgames.bingo.*

class Main : ModInitializer {
	companion object {
		const val MOD_ID = "mgames"

		var mg: MG? = null

		val resourceManager = ResourceManager()
	}

	override fun onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register { server ->
			val json = xyz.mantevian.mgames.bingo.json

			mg = MG(
				server,
				load(),
				BingoTaskSourceSet(
					resourceManager.get<List<BingoTaskSourceItemEntry>>("bingo/items.json", json)!!,
					resourceManager.get<List<BingoTaskSourceEnchantmentEntry>>("bingo/enchantments.json", json)!!,
					resourceManager.get<List<BingoTaskSourcePotionEntry>>("bingo/potions.json", json)!!,
					resourceManager.get<BingoPicker>("bingo/picker.json", json)!!
				),
				resourceManager.get<List<String>>("bingo/splashes.json", json)!!
			)
		}

		ServerLifecycleEvents.BEFORE_SAVE.register { _, _, _ ->
			mg?.storage?.let { save(it) }
		}

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ ->
			mg?.bingo?.taskSourceSet = BingoTaskSourceSet(
				resourceManager.get<List<BingoTaskSourceItemEntry>>("bingo/items.json", json)!!,
				resourceManager.get<List<BingoTaskSourceEnchantmentEntry>>("bingo/enchantments.json", json)!!,
				resourceManager.get<List<BingoTaskSourcePotionEntry>>("bingo/potions.json", json)!!,
				resourceManager.get<BingoPicker>("bingo/picker.json", json)!!
			)
			mg?.bingo?.splashes = resourceManager.get<List<String>>("bingo/splashes.json", json)!!
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, env ->
			registerCommands(dispatcher, registryAccess, env)
		}

		resourceManager.apply {
			registerFile("bingo/items.json")
			registerFile("bingo/enchantments.json")
			registerFile("bingo/potions.json")
			registerFile("bingo/picker.json")
			registerFile("bingo/splashes.json")

			registerDir("bingo/set")
		}

		MGItems.init()
	}
}
