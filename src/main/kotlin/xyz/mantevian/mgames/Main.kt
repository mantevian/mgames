package xyz.mantevian.mgames

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.item.Item
import net.minecraft.world.item.enchantment.Enchantment
import xyz.mantevian.mgames.game.Game
import xyz.mantevian.mgames.util.*

const val MOD_ID = "mgames"
lateinit var server: MinecraftServer
lateinit var resourceManager: MGResourceManager
var initialized = false
var game = Game()

val allItems: MutableList<Item> = mutableListOf()
val allEnchantments: MutableList<Enchantment> = mutableListOf()
val allEffects: MutableList<MobEffect> = mutableListOf()
val allAttributes: MutableList<Attribute> = mutableListOf()

class Main : ModInitializer {
    override fun onInitialize() {
        resourceManager = MGResourceManager()

        ServerLifecycleEvents.SERVER_STARTING.register { s ->
            server = s
            initialized = true
            allItems.addAll(getRegistry(Registries.ITEM).stream().toList())
            allEnchantments.addAll(getRegistry(Registries.ENCHANTMENT).stream().toList())
            allEffects.addAll(getRegistry(Registries.MOB_EFFECT).stream().toList())
            allAttributes.addAll(getRegistry(Registries.ATTRIBUTE).stream().toList())

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

        CommandRegistrationCallback.EVENT.register { dispatcher, buildContext, commandSelection ->
            registerCommands(dispatcher, buildContext, commandSelection)
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
                infiniteEffectForEveryone(MobEffects.RESISTANCE)
                infiniteEffectForEveryone(MobEffects.SATURATION)
                infiniteEffectForEveryone(MobEffects.NIGHT_VISION)

                hideSidebar()
            }

            GameState.PLAYING -> {
                game.time.inc()
                forEachPlayer {
                    if (game.time.getTicks() >= 0) {
                        it.sendSystemMessage(standardText(game.time.formatHourMinSec()), true)
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