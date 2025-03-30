package xyz.mantevian.mgames.bingo

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElementInterface
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import xyz.mantevian.mgames.*

class BingoMenu(player: ServerPlayerEntity, val mg: MG) : SimpleGui(ScreenHandlerType.GENERIC_9X5, player, false) {
	override fun getTitle(): Text {
		return Text.literal(mg.bingo.splashes.shuffled()[0])
	}

	override fun onOpen() {
		super.onOpen()

		val playerData = mg.storage.bingo.players[player.uuidAsString] ?: return

		for (i in 0..44) {
			setSlot(i, ItemStackBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip().build())
		}

		for (i in 0..24) {
			val task = mg.storage.bingo.tasks[i]?.data
			val reward = mg.storage.bingo.tasks[i]?.reward

			val x = i % 5
			val y = i / 5

			val builder = when (task) {
				is BingoTypedTaskData.Item -> ItemStackBuilder(task.id)
					.setCustomName(Text.translatable(mg.util.itemById(task.id).translationKey).resetStyle())
					.addLore(standardText("Obtain ${task.count} of this item").formatted(Formatting.GRAY))
					.withCount(task.count)

				is BingoTypedTaskData.Enchantment -> {
					ItemStackBuilder(Items.ENCHANTED_BOOK)
						.setCustomName(standardText("Enchantment"))
						.addLore(
							standardText(
								Text.translatable(
									"enchantment.${
										task.id.replace(
											":",
											"."
										)
									}"
								).string
							).formatted(Formatting.AQUA)
						)
						.addLore(standardText("Obtain any item with this enchantment").formatted(Formatting.GRAY))
				}

				is BingoTypedTaskData.Potion -> {
					val effect = mg.util.statusEffectById(task.id) ?: StatusEffects.WITHER.value()

					ItemStackBuilder(Items.POTION)
						.setPotionColor(effect)
						.setCustomName(standardText("Potion"))
						.addLore(
							standardText(
								Text.translatable(
									"effect.${
										task.id.replace(
											":",
											"."
										)
									}"
								).string
							).formatted(Formatting.AQUA)
						)
						.addLore(standardText("Obtain a potion with this effect").formatted(Formatting.GRAY))
				}

				is BingoTypedTaskData.ColoredItem -> ItemStackBuilder(task.id)

				else -> ItemStackBuilder(Items.BEDROCK)
			}

			if (reward != null) {
				builder.addLore(standardText(""))
				builder.addLore(standardText("Reward: $reward ★").formatted(Formatting.YELLOW))
			}

			if (playerData.tasks[i] != null) {
				builder.setCooldown(
					Identifier.of(
						"mantevian",
						"${Main.MOD_ID}/bingo/item_${i}"
					), 2000
				)

				builder.addLore(standardText(""))
				builder.addLore(standardText("COMPLETED").formatted(Formatting.GREEN))
			}

			builder.hideAdditionalTooltip()

			setSlot(y * 9 + x + 2, builder.build())
		}

		// 9, 27

		val requiredPoints = (playerData.usedRTP + 1) * 15
		val canUseRTP = mg.bingo.countPoints(player) >= requiredPoints

		val rtpBuilder = ItemStackBuilder(Items.ENDER_PEARL)
			.setCustomName(standardText("Random teleport"))

		if (canUseRTP) {
			rtpBuilder.addLore(standardText("Click to use").formatted(Formatting.AQUA))
			rtpBuilder.addLore(standardText(""))
			rtpBuilder.addLore(standardText("Next use activates at ${requiredPoints + 15} ★").formatted(Formatting.YELLOW))
		} else {
			rtpBuilder.addLore(standardText("Requires $requiredPoints ★ to use").formatted(Formatting.RED))
		}

		setSlot(9, rtpBuilder.build())

		setSlot(
			18, ItemStackBuilder(Items.RED_BED)
				.setCustomName(standardText("Teleport to your spawn"))
				.addLore(standardText("Click to use").formatted(Formatting.AQUA))
				.build()
		)

		setSlot(
			27, ItemStackBuilder(Items.NETHER_STAR)
				.setCustomName(standardText("Teleport to world spawn"))
				.addLore(standardText("Click to use").formatted(Formatting.AQUA))
				.build()
		)
	}

	override fun onClick(index: Int, type: ClickType, action: SlotActionType, element: GuiElementInterface): Boolean {
		val playerData = mg.storage.bingo.players[player.uuidAsString] ?: return false

		when (index) {
			9 -> {
				val requiredPoints = (playerData.usedRTP + 1) * 15
				val canUseRTP = mg.bingo.countPoints(player) >= requiredPoints

				if (canUseRTP) {
					close()
					mg.util.randomTeleport(player, mg.storage.bingo.worldSize / 2, mg.storage.bingo.worldSize / 8)
					playerData.usedRTP++
				}
			}

			18 -> {
				close()
				mg.util.teleportToOwnSpawn(player)
			}

			27 -> {
				close()
				mg.util.teleportToWorldSpawn(player)
			}
		}

		return super.onClick(index, type, action, element)
	}
}