package xyz.mantevian.mgames.bingo

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElementInterface
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKeys
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import xyz.mantevian.mgames.*
import xyz.mantevian.mgames.game.BingoComponent
import xyz.mantevian.mgames.game.WorldSizeComponent
import xyz.mantevian.mgames.util.*

class BingoMenu(player: ServerPlayerEntity) : SimpleGui(ScreenHandlerType.GENERIC_9X5, player, false) {
	private val bingo = game.getComponent<BingoComponent>()!!

	override fun getTitle(): Text {
		return Text.literal(resourceManager.get<List<String>>("bingo/splashes.json")!!.shuffled()[0])
	}

	override fun onOpen() {
		super.onOpen()

		val playerData = bingo.players[player.uuidAsString] ?: return

		for (i in 0..44) {
			setSlot(i, ItemStackBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip().build())
		}

		for (i in 0..24) {
			val task = bingo.tasks[i]?.data
			val reward = bingo.tasks[i]?.reward

			val x = i % 5
			val y = i / 5

			val builder = when (task) {
				is BingoTypedTaskData.Item -> {
					val name = Text.translatable(itemById(task.id).translationKey).resetStyle()

					val builder = ItemStackBuilder(task.id)
						.withCount(task.count)
						.setCustomName(name)

					if (task.count > 1) {
						builder.setCustomName(name.append(standardText(" (${task.count})")))
					}

					builder
				}

				is BingoTypedTaskData.Enchantment -> {
					ItemStackBuilder(Items.ENCHANTED_BOOK)
						.setCustomName(Text.translatable("enchantment.${task.id.replace(":", ".")}").resetStyle())
				}

				is BingoTypedTaskData.Potion -> {
					val effect = statusEffectById(task.id) ?: StatusEffects.WITHER.value()
					ItemStackBuilder(Items.POTION)
						.setPotionColor(effect)
						.setCustomName(Text.translatable("effect.${task.id.replace(":", ".")}").resetStyle())
				}

				is BingoTypedTaskData.ColoredItem -> {
					val builder = ItemStackBuilder(task.id)
						.setCustomName(Text.translatable(itemById(task.id).translationKey).resetStyle())
						.ofColorMix(task.colorNames)

					task.colorNames.forEach { name ->
						val item = server.registryManager.getOrThrow(RegistryKeys.ITEM)
							.get(Identifier.of("minecraft", "${name}_dye")) ?: return@forEach

						builder.addLore(
							Text.translatable(item.translationKey)
								.styled { it.withItalic(false).withColor(Formatting.GRAY) }
						)
					}

					builder
				}

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
						"$MOD_ID/bingo/item_${i}"
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
		val canUseRTP = bingo.countPoints(player) >= requiredPoints

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
		val playerData = bingo.players[player.uuidAsString] ?: return false

		when (index) {
			9 -> {
				val requiredPoints = (playerData.usedRTP + 1) * 15
				val canUseRTP = bingo.countPoints(player) >= requiredPoints

				val worldSize = game.getComponentOrDefault<WorldSizeComponent>().value

				if (canUseRTP) {
					close()
					randomTeleport(player, worldSize / 2, worldSize / 8)
					playerData.usedRTP++
				}
			}

			18 -> {
				close()
				teleportToOwnSpawn(player)
			}

			27 -> {
				close()
				teleportToWorldSpawn(player)
			}
		}

		return super.onClick(index, type, action, element)
	}
}