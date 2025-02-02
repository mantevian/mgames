package xyz.mantevian.mgames.bingo

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
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
		mg.server.playerManager.playerList.forEach {
			val playerData = BingoPlayer()

			mg.storage.bingo.tasks.forEach { (i, _) ->
				playerData.tasks[i] = false
			}

			mg.storage.bingo.players[it.uuidAsString] = playerData
		}
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