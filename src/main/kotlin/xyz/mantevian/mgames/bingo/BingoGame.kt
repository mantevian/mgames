package xyz.mantevian.mgames.bingo

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.UnbreakableComponent
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameMode
import xyz.mantevian.mgames.*

class BingoGame(val mg: MG, var taskSourceSet: BingoTaskSourceSet, var splashes: List<String>) {
	fun init() {
		mg.worldgen.bedrockBoxAtWorldBottom()
		mg.util.tpPlayersToWorldBottom()
		mg.util.resetPlayersMinecraftStats()

		mg.storage.bingo.reinit()

		mg.util.deleteScoreboard("bingo.score")
		mg.util.createScoreboardSidebar("bingo.score", "★ Points ★")

		mg.executeCommand("worldborder set 50")

		mg.executeCommand("gamerule keepInventory true")
	}

	private fun setupData(): Boolean {
		mg.storage.bingo.tasks.clear()

		val setName = mg.storage.bingo.useSet ?: return BingoGenerator(taskSourceSet).generateTasks(mg)

		val data = Main.resourceManager.get<BingoStorage>("bingo/set/$setName.json", json)
			?: return BingoGenerator(taskSourceSet).generateTasks(mg)

		mg.storage.bingo = data
		return true
	}

	fun start(): Boolean {
		if (!setupData()) {
			mg.util.announce(
				standardText("Couldn't create a bingo with the specified picker or set. Please change the setup or try again").formatted(
					Formatting.RED
				)
			)
			return false
		}

		mg.storage.time.set(-20 * 10)

		mg.util.resetPlayersMinecraftStats()

		mg.util.forEachPlayer {
			val playerData = BingoPlayer()

			mg.storage.bingo.tasks.forEach { (i, _) ->
				playerData.tasks[i] = null
			}

			mg.storage.bingo.players[it.uuidAsString] = playerData

			it.giveItemStack(ItemStackBuilder(MGItems.BINGO_MENU_ITEM).build())

			it.changeGameMode(GameMode.SURVIVAL)
		}

		mg.executeCommand("time set 0")
		mg.executeCommand("weather clear 9999999")

		mg.executeCommand("execute in overworld run worldborder set ${mg.storage.bingo.worldSize * 2}")
		mg.executeCommand("execute in the_nether run worldborder set ${mg.storage.bingo.worldSize * 2}")
		mg.executeCommand("execute in the_end run worldborder set ${mg.storage.bingo.worldSize * 2}")

		mg.util.effectForEveryone(StatusEffects.SLOWNESS, 20 * 10, 5)
		mg.util.effectForEveryone(StatusEffects.MINING_FATIGUE, 20 * 10, 5)
		mg.util.effectForEveryone(StatusEffects.WEAKNESS, 20 * 10, 5)
		mg.util.effectForEveryone(StatusEffects.RESISTANCE, 20 * 10, 2)

		mg.util.effectForEveryone(StatusEffects.FIRE_RESISTANCE, 20 * 60 * 5, 0)
		mg.util.effectForEveryone(StatusEffects.RESISTANCE, 20 * 60 * 5, 0)

		mg.executeCommand("gamerule fallDamage false")

		mg.util.teleportInCircle(mg.util.getAllPlayers(), 500, 10)

		return true
	}

	fun finish() {
		mg.storage.state = GameState.NOT_INIT

		val sortedPlayers = mg.util.getAllPlayers().sortedByDescending { getScore(it) }

		mg.util.title("Bingo has ended!")

		mg.util.announce(standardText("Leaderboard for this game:").formatted(Formatting.AQUA))

		sortedPlayers.forEachIndexed { i, player ->
			mg.util.announce(standardText("").apply {
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

		player.giveItemStack(ItemStack(Items.ENDER_EYE))
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

	fun tick() {
		when (mg.storage.state) {
			GameState.PLAYING -> {
				mg.util.infiniteEffectForEveryone(StatusEffects.SATURATION)

				mg.util.forEachPlayer { player ->
					val uuid = player.uuidAsString

					mg.storage.bingo.tasks.forEach {
						val completed = checkTask(player, it.value.data)
						val alreadyMarkedCompleted = mg.storage.bingo.players[uuid]?.tasks?.get(it.key) != null

						if (completed && !alreadyMarkedCompleted) {
							setCompletedTask(player, it.key)
						}

						player.itemCooldownManager.set(
							Identifier.of(
								"mantevian",
								"${Main.MOD_ID}/bingo/item_${it.key}"
							), if (alreadyMarkedCompleted) 2000 else 0
						)
					}

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
						val mainhand = player.getStackInHand(Hand.MAIN_HAND)
						if (mainhand.get(DataComponentTypes.MAX_DAMAGE) != null) {
							mainhand.set(DataComponentTypes.UNBREAKABLE, UnbreakableComponent(true))
						}
					}
				}

				val gameTime = mg.storage.bingo.gameTime.getFullSeconds()
				if (mg.storage.time.getTicks() % 20 == 0) {
					when (mg.storage.time.getFullSeconds()) {
						-10 -> {
							mg.util.announce(
								standardText("Bingo starts in 10 seconds!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
							mg.server.isPvpEnabled = false
						}

						0 -> {
							mg.util.announce(
								standardText("Bingo has started!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
							mg.util.title("Bingo has started!")

							mg.util.forEachPlayer {
								it.setSpawnPointFrom(it)
							}

							mg.executeCommand("gamerule fallDamage true")
						}

						gameTime - 900 -> {
							mg.util.announce(
								standardText("Bingo ends in 15 minutes!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime - 300 -> {
							mg.util.announce(
								standardText("Bingo ends in 5 minutes!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime - 30 -> {
							mg.util.announce(
								standardText("Bingo ends in 30 seconds!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime - 5 -> {
							mg.util.announce(
								standardText("Bingo ends in 5 seconds!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime - 4 -> {
							mg.util.announce(
								standardText("Bingo ends in 4 seconds!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime - 3 -> {
							mg.util.announce(
								standardText("Bingo ends in 3 seconds!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime - 2 -> {
							mg.util.announce(
								standardText("Bingo ends in 2 seconds!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime - 1 -> {
							mg.util.announce(
								standardText("Bingo ends in 1 seconds!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
						}

						gameTime -> {
							mg.util.announce(
								standardText("Bingo has ended!"),
								SoundEvents.UI_BUTTON_CLICK.value(),
								2.0f,
								1.0f
							)
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

	fun getLastTime(player: ServerPlayerEntity): MGDuration {
		val playerData = mg.storage.bingo.players[player.uuidAsString] ?: return MGDuration.zero()
		return playerData.tasks.map { task -> task.value }.sortedByDescending { dur -> dur?.getTicks() }[0]
			?: MGDuration.zero()
	}

	fun getScore(player: ServerPlayerEntity): Int {
		return (countPoints(player) + 1) * mg.storage.bingo.gameTime.getTicks() - getLastTime(player).getTicks()
	}

	fun maxPoints(): Int {
		if (mg.storage.bingo.tasks.isEmpty()) {
			return 0
		}
		return mg.storage.bingo.tasks.map { it.value.reward }.reduce { a, b -> a + b }
	}
}