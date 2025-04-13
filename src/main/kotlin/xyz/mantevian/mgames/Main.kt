package xyz.mantevian.mgames

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.MinecraftServer
import xyz.mantevian.mgames.game.Game
import xyz.mantevian.mgames.util.*

const val MOD_ID = "mgames"
lateinit var server: MinecraftServer
lateinit var resourceManager: ResourceManager
var initialized = false
var game = Game()

val allItems: MutableList<RegistryEntry<Item>> = mutableListOf()
val allEnchantments: MutableList<RegistryEntry<Enchantment>> = mutableListOf()
val allEffects: MutableList<RegistryEntry<StatusEffect>> = mutableListOf()
val allAttributes: MutableList<RegistryEntry<EntityAttribute>> = mutableListOf()

class Main : ModInitializer {
	override fun onInitialize() {
		resourceManager = ResourceManager()

		ServerLifecycleEvents.SERVER_STARTING.register { s ->
			server = s
			initialized = true

			allItems.addAll(server.registryManager.getOrThrow(RegistryKeys.ITEM).indexedEntries)
			allEnchantments.addAll(server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).indexedEntries)
			allEffects.addAll(server.registryManager.getOrThrow(RegistryKeys.STATUS_EFFECT).indexedEntries)
			allAttributes.addAll(server.registryManager.getOrThrow(RegistryKeys.ATTRIBUTE).indexedEntries)

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

		ServerLivingEntityEvents.AFTER_DEATH.register { entity, source ->
			if (initialized) {
				game.onDeath(entity, source)
			}
		}

		MGItems.init()
	}

	private fun tick() {
		when (game.state) {
			GameState.WAITING -> {
				infiniteEffectForEveryone(StatusEffects.RESISTANCE)
				infiniteEffectForEveryone(StatusEffects.SATURATION)
				infiniteEffectForEveryone(StatusEffects.NIGHT_VISION)

				hideSidebar()
			}

			GameState.PLAYING -> {
				game.time.inc()
				forEachPlayer {
					if (game.time.getTicks() >= 0) {
						it.sendMessageToClient(standardText(game.time.formatHourMinSec()), true)
					}
				}
			}

			else -> {
				hideSidebar()
			}
		}

		game.tick()
	}
}

fun startGame() {
	if (!game.canStart()) {
		return
	}

	game.state = GameState.PLAYING

	game.start()
}