package xyz.mantevian.mgames.bingo

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.UnbreakableComponent
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import xyz.mantevian.mgames.*

class BingoGame(val mg: MG, val taskSourceSet: BingoTaskSourceSet) {
	fun init() {
		mg.worldgen.bedrockBoxAtWorldBottom()
		mg.util.tpPlayersToWorldBottom()
		mg.util.resetPlayersMinecraftStats()

		generateTasks()

		mg.util.deleteScoreboard("bingo.score")
		mg.util.createScoreboardSidebar("bingo.score", "★ Points ★")
	}

	fun generateTasks() {
		for (i in 0..24) {
			val s = taskSourceSet.tasks[mg.util.nextInt(0..<taskSourceSet.tasks.size)]

			mg.storage.bingo.tasks[i] = when (s) {
				is BingoTaskSource.Item -> {
					BingoTaskData(s.rarity, BingoTypedTaskData.Item(s.id, mg.util.nextInt(s.minCount..s.maxCount)))
				}

				is BingoTaskSource.Enchantment -> {
					BingoTaskData(s.options[0].rarity, BingoTypedTaskData.Enchantment(s.options[0].id))
				}

				is BingoTaskSource.Potion -> {
					BingoTaskData(s.options[0].rarity, BingoTypedTaskData.Potion(s.options[0].id))
				}

				else -> {
					BingoTaskData(0, BingoTypedTaskData.None)
				}
			}
		}

		println(mg.storage.bingo.tasks)
	}

	fun start() {
		mg.storage.time.set(-20 * 10)

		mg.server.playerManager.playerList.forEach {
			val playerData = BingoPlayer()

			mg.storage.bingo.tasks.forEach { (i, _) ->
				playerData.tasks[i] = false
			}

			mg.storage.bingo.players[it.uuidAsString] = playerData

			it.giveItemStack(ItemStackBuilder(MGItems.BINGO_MENU_ITEM).build())

			it.addStatusEffect(StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0, false, false, false))
			it.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 200, 10, false, false, false))
			it.addStatusEffect(StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 10, false, false, false))
			it.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 200, 10, false, false, false))

			it.addStatusEffect(StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 60 * 5, 0, false, false, false))
			it.addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 200 * 60 * 5, 1, false, false, false))
		}

		mg.util.teleportInCircle(mg.server.playerManager.playerList, 500, 10)
	}

	private fun setCompletedTask(player: ServerPlayerEntity, n: Int) {
		val task = mg.storage.bingo.tasks[n] ?: return
		val playerData = mg.storage.bingo.players[player.uuidAsString] ?: return

		playerData.tasks[n] = true

		val taskTypedText = when (task.data) {
			is BingoTypedTaskData.Item -> Text.literal("").run {
				append(Text.translatable(mg.util.itemById(task.data.id).translationKey))
				append(" (${task.data.count})")
			}

			is BingoTypedTaskData.Enchantment -> Text.literal("").run {
				append("Enchantment ")
				append(mg.util.enchantmentById(task.data.id)?.description)
			}

			is BingoTypedTaskData.Potion -> Text.literal("").run {
				append("Potion of ")
				append(Text.translatable(mg.util.statusEffectById(task.data.id)?.translationKey))
			}

			is BingoTypedTaskData.ColoredItem -> Text.literal("").run {
				append(Text.translatable(mg.util.itemById(task.data.id).translationKey))
			}

			else -> Text.literal("")
		}

		mg.util.announce(
			Text.literal("").apply {
				append(Text.literal("[").formatted(Formatting.GRAY))
				append(Text.literal("+${task.reward} ★").formatted(Formatting.YELLOW))
				append(Text.literal("] ").formatted(Formatting.GRAY))
				append(Text.literal(player.nameForScoreboard).formatted(Formatting.GREEN))
				append(Text.literal(" has collected ").formatted(Formatting.GRAY))
				append(taskTypedText.formatted(Formatting.GREEN))
				append(Text.literal(" [" + mg.storage.time.formatHourMinSec() + "]").formatted(Formatting.GRAY))
			}
		)
	}

	private fun checkTask(player: ServerPlayerEntity, task: BingoTypedTaskData): Boolean {
		return when (task) {
			is BingoTypedTaskData.Item -> {
				player.inventory.contains { stack ->
					stack isId task.id && stack.count >= task.count
				}
			}

			is BingoTypedTaskData.Enchantment -> {
				player.inventory.contains { stack ->
					stack.enchantments.enchantments.any { enchantment ->
						enchantment.idAsString == task.id
					}
				}
			}

			is BingoTypedTaskData.Potion -> {
				player.inventory.contains { stack ->
					val isItem = stack isId listOf(
						"minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion"
					)

					val isPotion = stack.get(DataComponentTypes.POTION_CONTENTS)?.potion?.get()?.value()?.effects?.any {
						it.effectType.idAsString == task.id
					}

					isItem && isPotion ?: false
				}
			}

			is BingoTypedTaskData.ColoredItem -> {
				player.inventory.contains { stack ->
					stack isId task.id && DyedColorComponent.getColor(
						stack,
						0
					) == task.colorValue
				}
			}

			is BingoTypedTaskData.OneOfItems -> {
				false
			}

			is BingoTypedTaskData.None -> false
		}
	}

	fun tick() {
		when (mg.storage.state) {
			GameState.PLAYING -> {
				mg.server.playerManager.playerList.forEach { player ->
					val uuid = player.uuidAsString
					val playerTasks = mg.storage.bingo.players[uuid]?.tasks ?: mutableMapOf()

					playerTasks
						.filter { !it.value }
						.map { (i, _) -> (i to mg.storage.bingo.tasks[i]) }
						.filter { (_, task) -> task != null && checkTask(player, task.data) }
						.forEach { (i, _) -> setCompletedTask(player, i) }

					mg.util.setScore(player.nameForScoreboard, "bingo.score", countPoints(player))

					val mainHandStack = player.getStackInHand(Hand.MAIN_HAND)
					mg.storage.bingo.handEnchantments.forEach { (id, level) ->
						val enchantment = RegistryEntry.of(mg.util.enchantmentById(id))
						if (mainHandStack.canBeEnchantedWith(enchantment, EnchantingContext.ACCEPTABLE)) {
							mainHandStack.addEnchantment(enchantment, level)
						}
					}

					if (mg.storage.bingo.unbreakableItems) {
						player.getStackInHand(Hand.MAIN_HAND).set(DataComponentTypes.UNBREAKABLE, UnbreakableComponent(true))
					}
				}

				val pvpTime = mg.storage.bingo.pvpTime.getFullSeconds()
				val gameTime = mg.storage.bingo.gameTime.getFullSeconds()
				if (mg.storage.time.getTicks() % 20 == 0) {
					when (mg.storage.time.getFullSeconds()) {
						-10 -> {
							mg.util.announce(Text.literal("Bingo starts in 10 seconds!"))
						}

						0 -> {
							mg.util.announce(Text.literal("Bingo has started!"))
						}

						pvpTime - 300 -> {
							mg.util.announce(Text.literal("PVP enables in 5 minutes!"))
						}

						pvpTime - 30 -> {
							mg.util.announce(Text.literal("PVP enables in 30 seconds!"))
						}

						pvpTime - 5 -> {
							mg.util.announce(Text.literal("PVP enables in 5 seconds!"))
						}

						pvpTime - 4 -> {
							mg.util.announce(Text.literal("PVP enables in 4 seconds!"))
						}

						pvpTime - 3 -> {
							mg.util.announce(Text.literal("PVP enables in 3 seconds!"))
						}

						pvpTime - 2 -> {
							mg.util.announce(Text.literal("PVP enables in 2 seconds!"))
						}

						pvpTime - 1 -> {
							mg.util.announce(Text.literal("PVP enables in 1 seconds!"))
						}

						pvpTime -> {
							mg.util.announce(Text.literal("PVP is enabled!"))
						}

						gameTime - 900 -> {
							mg.util.announce(Text.literal("Bingo ends in 15 minutes!"))
						}

						gameTime - 300 -> {
							mg.util.announce(Text.literal("Bingo ends in 5 minutes!"))
						}

						gameTime - 30 -> {
							mg.util.announce(Text.literal("Bingo ends in 30 seconds!"))
						}

						gameTime - 5 -> {
							mg.util.announce(Text.literal("Bingo ends in 5 seconds!"))
						}

						gameTime - 4 -> {
							mg.util.announce(Text.literal("Bingo ends in 4 seconds!"))
						}

						gameTime - 3 -> {
							mg.util.announce(Text.literal("Bingo ends in 3 seconds!"))
						}

						gameTime - 2 -> {
							mg.util.announce(Text.literal("Bingo ends in 2 seconds!"))
						}

						gameTime - 1 -> {
							mg.util.announce(Text.literal("Bingo ends in 1 seconds!"))
						}

						gameTime -> {
							mg.util.announce(Text.literal("Bingo has ended!"))
						}
					}
				}
			}

			GameState.WAITING -> {
				mg.server.playerManager.playerList.forEach { player ->
					mg.util.setScore(player.nameForScoreboard, "bingo.score", 0)
				}
			}

			else -> {}
		}
	}

	fun countPoints(player: ServerPlayerEntity): Int {
		var sum = 0
		mg.storage.bingo.players[player.uuidAsString]?.tasks?.forEach { (i, b) ->
			if (b) {
				sum += mg.storage.bingo.tasks[i]?.reward ?: 0
			}
		}
		return sum
	}
}