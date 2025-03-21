package xyz.mantevian.mgames.bingo

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.UnbreakableComponent
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
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

		mg.util.forEachPlayer {
			val playerData = BingoPlayer()

			mg.storage.bingo.tasks.forEach { (i, _) ->
				playerData.tasks[i] = null
			}

			mg.storage.bingo.players[it.uuidAsString] = playerData

			mg.util.resetPlayers()

			it.giveItemStack(ItemStackBuilder(MGItems.BINGO_MENU_ITEM).build())
		}

		mg.util.effectForEveryone(StatusEffects.BLINDNESS, 200, 0)
		mg.util.effectForEveryone(StatusEffects.SLOWNESS, 200, 0)
		mg.util.effectForEveryone(StatusEffects.MINING_FATIGUE, 200, 0)
		mg.util.effectForEveryone(StatusEffects.WEAKNESS, 200, 0)

		mg.util.effectForEveryone(StatusEffects.FIRE_RESISTANCE, 200 * 50 * 5, 0)
		mg.util.effectForEveryone(StatusEffects.RESISTANCE, 200 * 60 * 5, 0)

		mg.util.teleportInCircle(mg.util.getAllPlayers(), 500, 10)
	}

	fun finish() {
		val sortedPlayers = mg.util.getAllPlayers()
			.map {
				val playerData = mg.storage.bingo.players[it.uuidAsString] ?: return
				val lastTime = playerData.tasks.map { task -> task.value }.sortedByDescending { dur -> dur?.getTicks() }[0]
				Triple(it, countPoints(it), lastTime ?: MGDuration.zero())
			}
			.sortedWith(compareBy({ it.second }, { it.third.getTicks() }))

		sortedPlayers.forEachIndexed { i, (player, points, time) ->
			mg.util.announce(standardText("${i + 1}. ${player.nameForScoreboard} $points ★ [${time.formatHourMinSec()}]"))
		}

		mg.util.forEachPlayer {
			mg.util.teleport(it, mg.server.overworld, BlockPos(0, -62, 0))
		}
	}

	private fun setCompletedTask(player: ServerPlayerEntity, n: Int) {
		val task = mg.storage.bingo.tasks[n] ?: return
		val playerData = mg.storage.bingo.players[player.uuidAsString] ?: return

		playerData.tasks[n] = mg.storage.time.clone()

		val taskTypedText = when (task.data) {
			is BingoTypedTaskData.Item -> standardText("").run {
				append(Text.translatable(mg.util.itemById(task.data.id).translationKey))
				append(" (${task.data.count})")
			}

			is BingoTypedTaskData.Enchantment -> standardText("").run {
				append("Enchantment ")
				append(mg.util.enchantmentById(task.data.id)?.description)
			}

			is BingoTypedTaskData.Potion -> standardText("").run {
				append("Potion of ")
				append(Text.translatable(mg.util.statusEffectById(task.data.id)?.translationKey))
			}

			is BingoTypedTaskData.ColoredItem -> standardText("").run {
				append(Text.translatable(mg.util.itemById(task.data.id).translationKey))
			}

			else -> standardText("")
		}

		mg.util.announce(
			standardText("").apply {
				append(standardText("[").formatted(Formatting.GRAY))
				append(standardText("+${task.reward} ★").formatted(Formatting.YELLOW))
				append(standardText("] ").formatted(Formatting.GRAY))
				append(standardText(player.nameForScoreboard).formatted(Formatting.GREEN))
				append(standardText(" has collected ").formatted(Formatting.GRAY))
				append(taskTypedText.formatted(Formatting.GREEN))
				append(standardText(" [" + mg.storage.time.formatHourMinSec() + "]").formatted(Formatting.GRAY))
			}
		)

		mg.util.playSound(player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP)
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
				mg.util.forEachPlayer { player ->
					val uuid = player.uuidAsString
					val playerTasks = mg.storage.bingo.players[uuid]?.tasks ?: mutableMapOf()

					playerTasks
						.filter { it.value == null }
						.map { (i, _) -> (i to mg.storage.bingo.tasks[i]) }
						.filter { (_, task) -> task != null && checkTask(player, task.data) }
						.forEach { (i, _) -> setCompletedTask(player, i) }

					mg.util.setScore(player.nameForScoreboard, "bingo.score", countPoints(player))

					val mainHandStack = player.getStackInHand(Hand.MAIN_HAND)
					mg.storage.bingo.handEnchantments.forEach { (id, level) ->
						mg.util.enchantmentById(id)?.let { enchantment ->
							val entry = mg.util.getEnchantmentEntry(enchantment)
							if (mainHandStack.canBeEnchantedWith(entry, EnchantingContext.ACCEPTABLE)) {
								mainHandStack.addEnchantment(entry, level)
							}
						}
					}

					if (mg.storage.bingo.unbreakableItems) {
						player.getStackInHand(Hand.MAIN_HAND).set(DataComponentTypes.UNBREAKABLE, UnbreakableComponent(true))
					}
				}

				val gameTime = mg.storage.bingo.gameTime.getFullSeconds()
				if (mg.storage.time.getTicks() % 20 == 0) {
					when (mg.storage.time.getFullSeconds()) {
						-10 -> {
							mg.util.announce(standardText("Bingo starts in 10 seconds!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
							mg.server.isPvpEnabled = false
						}

						0 -> {
							mg.util.announce(standardText("Bingo has started!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 900 -> {
							mg.util.announce(standardText("Bingo ends in 15 minutes!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 300 -> {
							mg.util.announce(standardText("Bingo ends in 5 minutes!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 30 -> {
							mg.util.announce(standardText("Bingo ends in 30 seconds!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 5 -> {
							mg.util.announce(standardText("Bingo ends in 5 seconds!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 4 -> {
							mg.util.announce(standardText("Bingo ends in 4 seconds!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 3 -> {
							mg.util.announce(standardText("Bingo ends in 3 seconds!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 2 -> {
							mg.util.announce(standardText("Bingo ends in 2 seconds!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime - 1 -> {
							mg.util.announce(standardText("Bingo ends in 1 seconds!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
						}

						gameTime -> {
							mg.util.announce(standardText("Bingo has ended!"), SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
							finish()
						}
					}
				}
			}

			GameState.WAITING -> {
				mg.util.forEachPlayer { player ->
					mg.util.setScore(player.nameForScoreboard, "bingo.score", 0)
				}
			}

			else -> {}
		}
	}

	fun countPoints(player: ServerPlayerEntity): Int {
		var sum = 0
		mg.storage.bingo.players[player.uuidAsString]?.tasks?.forEach { (i, d) ->
			if (d != null) {
				sum += mg.storage.bingo.tasks[i]?.reward ?: 0
			}
		}
		return sum
	}
}