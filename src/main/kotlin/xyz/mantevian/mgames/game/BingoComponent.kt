package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.world.GameMode
import xyz.mantevian.mgames.*
import xyz.mantevian.mgames.bingo.BingoGenerator
import xyz.mantevian.mgames.util.*

@Serializable
@SerialName("bingo")
class BingoComponent : GameComponent {
	@SerialName("use_set")
	var useSet: String? = null

	@SerialName("task_enchantment")
	var taskEnchantment: Boolean = true

	@SerialName("task_potion")
	var taskPotion: Boolean = true

	@SerialName("task_colored")
	var taskColored: Boolean = true

	@SerialName("tasks")
	val tasks: MutableMap<Int, BingoTaskData> = mutableMapOf()

	@SerialName("players")
	val players: MutableMap<String, BingoPlayer> = mutableMapOf()

	override fun init() {
		tasks.clear()
		players.clear()

		executeCommand("gamerule keepInventory true")

		deleteScoreboard("bingo.score")
		createScoreboardSidebar("bingo.score", "★ Points ★")
	}

	override fun canStart(): Boolean {
		return setupData()
	}

	override fun start() {
		game.time.set(-20 * 10)

		resetPlayersMinecraftStats()

		forEachPlayer {
			val playerData = BingoPlayer()
			playerData.tasks.clear()
			players[it.uuidAsString] = playerData
			it.giveItemStack(ItemStackBuilder(MGItems.BINGO_MENU_ITEM).build())
			it.changeGameMode(GameMode.SURVIVAL)
		}

		effectForEveryone(StatusEffects.SLOWNESS, 20 * 10, 5)
		effectForEveryone(StatusEffects.MINING_FATIGUE, 20 * 10, 5)
		effectForEveryone(StatusEffects.WEAKNESS, 20 * 10, 5)
		effectForEveryone(StatusEffects.RESISTANCE, 20 * 10, 2)

		effectForEveryone(StatusEffects.FIRE_RESISTANCE, 20 * 60 * 5, 0)
		effectForEveryone(StatusEffects.RESISTANCE, 20 * 60 * 5, 0)

		executeCommand("gamerule fallDamage false")

		teleportInCircle(getAllPlayers(), 500)
	}

	override fun tick() {
		when (game.state) {
			GameState.PLAYING -> {
				infiniteEffectForEveryone(StatusEffects.SATURATION)

				forEachPlayer { player ->
					val uuid = player.uuidAsString

					tasks.forEach {
						val completed = checkTask(player, it.value.data)
						val alreadyMarkedCompleted = players[uuid]?.tasks?.get(it.key) != null

						if (completed && !alreadyMarkedCompleted) {
							setCompletedTask(player, it.key)
						}

						player.itemCooldownManager.set(
							Identifier.of(
								"mantevian",
								"$MOD_ID/bingo/item_${it.key}"
							), if (alreadyMarkedCompleted) 1000000 else 0
						)
					}

					setScore(player.nameForScoreboard, "bingo.score", countPoints(player))
				}

				val gameTime = game.getComponentOrDefault<GameTimeComponent>().value.getFullSeconds()
				if (game.time.getTicks() % 20 == 0) {
					when (game.time.getFullSeconds()) {
						-10 -> {
							announceClick(standardText("Bingo starts in 10 seconds!"))
							server.isPvpEnabled = false
						}

						0 -> {
							announce(standardText("Bingo has started!"))
							title("Bingo has started!")

							forEachPlayer {
								setSpawnPoint(it)
							}
						}

						30 -> {
							executeCommand("gamerule fallDamage true")
						}

						gameTime - 900 -> {
							announceClick(standardText("Bingo ends in 15 minutes!"))
						}

						gameTime - 300 -> {
							announceClick(standardText("Bingo ends in 5 minutes!"))
						}

						gameTime - 30 -> {
							announceClick(standardText("Bingo ends in 30 seconds!"))
						}

						gameTime - 5 -> {
							announceClick(standardText("Bingo ends in 5 seconds!"))
						}

						gameTime - 4 -> {
							announceClick(standardText("Bingo ends in 4 seconds!"))
						}

						gameTime - 3 -> {
							announceClick(standardText("Bingo ends in 3 seconds!"))
						}

						gameTime - 2 -> {
							announceClick(standardText("Bingo ends in 2 seconds!"))
						}

						gameTime - 1 -> {
							announceClick(standardText("Bingo ends in 1 seconds!"))
						}

						gameTime -> {
							announceClick(standardText("Bingo has ended!"))
							game.finish()
						}
					}
				}
			}

			GameState.WAITING -> {
				forEachPlayer { player ->
					setScore(player.nameForScoreboard, "bingo.score", 0)
				}
			}

			else -> {}
		}
	}

	override fun finish() {
		val sortedPlayers = getAllPlayers().sortedByDescending { getScore(it) }

		title("Bingo has ended!")

		announce(standardText("Leaderboard for this game:").formatted(Formatting.AQUA))

		sortedPlayers.forEachIndexed { i, player ->
			announce(standardText("").apply {
				append(standardText("${i + 1}. "))
				append(
					standardText(player.nameForScoreboard).formatted(
						when (i) {
							0 -> Formatting.YELLOW
							1 -> Formatting.GRAY
							2 -> Formatting.GOLD
							else -> Formatting.WHITE
						}
					)
				)
				append(standardText(" ${countPoints(player)} ★").formatted(Formatting.WHITE))
				append(standardText(" [${getLastTime(player).formatHourMinSec()}]").formatted(Formatting.GRAY))
			})
		}
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
					} || stack.get(DataComponentTypes.STORED_ENCHANTMENTS)?.enchantments?.any {
						it.idAsString == task.id
					} ?: false
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

	private fun setCompletedTask(player: ServerPlayerEntity, n: Int) {
		val task = tasks[n] ?: return
		val playerData = players[player.uuidAsString] ?: return

		playerData.tasks[n] = game.time.clone()

		val taskTypedText = when (task.data) {
			is BingoTypedTaskData.Item -> standardText("").run {
				append(Text.translatable(itemById(task.data.id).translationKey))
				append(" (${task.data.count})")
			}

			is BingoTypedTaskData.Enchantment -> standardText("").run {
				append("Enchantment ")
				append(enchantmentById(task.data.id)?.description)
			}

			is BingoTypedTaskData.Potion -> standardText("").run {
				append("Potion of ")
				append(Text.translatable(statusEffectById(task.data.id)?.translationKey))
			}

			is BingoTypedTaskData.ColoredItem -> standardText("").run {
				append(Text.translatable(itemById(task.data.id).translationKey))
			}

			else -> standardText("")
		}

		announce(
			standardText("").apply {
				append(standardText("[").formatted(Formatting.GRAY))
				append(standardText("+${task.reward} ★").formatted(Formatting.YELLOW))
				append(standardText("] ").formatted(Formatting.GRAY))
				append(standardText(player.nameForScoreboard).formatted(Formatting.GREEN))
				append(standardText(" has collected ").formatted(Formatting.GRAY))
				append(taskTypedText.formatted(Formatting.GREEN))
				append(standardText(" [" + game.time.formatHourMinSec() + "]").formatted(Formatting.GRAY))
			}
		)

		playSound(player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP)

		player.giveItemStack(ItemStack(Items.ENDER_EYE))
	}

	fun countPoints(player: ServerPlayerEntity): Int {
		var sum = 0
		players[player.uuidAsString]?.tasks?.forEach { (i, d) ->
			sum += tasks[i]?.reward ?: 0
		}
		return sum
	}

	fun getLastTime(player: ServerPlayerEntity): MGDuration {
		val playerData = players[player.uuidAsString] ?: return MGDuration.zero()
		return playerData.tasks.map { task -> task.value }.sortedByDescending { dur -> dur.getTicks() }[0]
	}

	fun getScore(player: ServerPlayerEntity): Int {
		val gameTime = game.getComponentOrDefault<GameTimeComponent>().value.getTicks()
		return (countPoints(player) + 1) * gameTime - getLastTime(player).getTicks()
	}

	fun maxPoints(): Int {
		if (tasks.isEmpty()) {
			return 0
		}
		return tasks.map { it.value.reward }.reduce { a, b -> a + b }
	}

	private fun setupData(): Boolean {
		tasks.clear()

		val data = resourceManager.get<Map<Int, BingoTaskData>>("bingo/set/${useSet}.json")
			?: return BingoGenerator().generateTasks()

		tasks.putAll(data)

		return true
	}
}