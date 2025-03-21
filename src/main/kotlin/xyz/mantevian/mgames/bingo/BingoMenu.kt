package xyz.mantevian.mgames.bingo

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElementInterface
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.item.Items
import net.minecraft.potion.Potions
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import xyz.mantevian.mgames.*

class BingoMenu(player: ServerPlayerEntity, val mg: MG) : SimpleGui(ScreenHandlerType.GENERIC_9X5, player, false) {
	override fun getTitle(): Text {
		return standardText("Bingo")
	}

	override fun onOpen() {
		super.onOpen()

		val playerData = mg.storage.bingo.players[player.uuidAsString] ?: return

		for (i in 0..44) {
			setSlot(i, ItemStackBuilder(Items.LIGHT_GRAY_STAINED_GLASS_PANE).build())
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
						.addLore(standardText(Text.translatable("enchantment.${task.id.replace(":", ".")}").string).formatted(Formatting.AQUA))
						.addLore(standardText("Obtain any item with this enchantment").formatted(Formatting.GRAY))
				}

				is BingoTypedTaskData.Potion -> {
					val potion = mg.util.potionById(task.id) ?: Potions.AWKWARD.value()

					ItemStackBuilder(Items.POTION)
						.setPotion(potion)
						.addLore(standardText(Text.translatable("effect.${task.id.replace(":", ".")}").string).formatted(Formatting.AQUA))
						.addLore(standardText("Obtain a potion with this effect").formatted(Formatting.GRAY))
				}

				is BingoTypedTaskData.ColoredItem -> ItemStackBuilder(task.id)

				else -> ItemStackBuilder(Items.BEDROCK)
			}

			if (playerData.tasks[i] != null) {
				builder.ofItem(Items.NETHER_STAR).withCount(1)
			}

			if (reward != null) {
				builder.addLore(standardText(""))
				builder.addLore(standardText("Reward: $reward ★").formatted(Formatting.YELLOW))
			}

			builder.hideTooltip()

			setSlot(y * 9 + x + 2, builder.build())
		}

		// 9, 27

		val requiredPoints = (playerData.usedRTP + 1) * 15
		val canUseRTP = mg.bingo.countPoints(player) >= requiredPoints

		val builder = ItemStackBuilder(Items.ENDER_PEARL)
			.setCustomName(standardText("Random teleport"))

		if (canUseRTP) {
			builder.addLore(standardText("Click to use").formatted(Formatting.AQUA))
			builder.addLore(standardText(""))
			builder.addLore(standardText("Next use activates at ${requiredPoints + 15} ★").formatted(Formatting.YELLOW))
		} else {
			builder.addLore(standardText("Requires $requiredPoints ★ to use").formatted(Formatting.RED))
		}

		setSlot(9, builder.build())
	}

	override fun onClick(index: Int, type: ClickType, action: SlotActionType, element: GuiElementInterface): Boolean {
		val playerData = mg.storage.bingo.players[player.uuidAsString] ?: return false

		when (index) {
			9 -> {
				val requiredPoints = (playerData.usedRTP + 1) * 15
				val canUseRTP = mg.bingo.countPoints(player) >= requiredPoints

				if (canUseRTP) {
					mg.util.randomTeleport(player, mg.storage.bingo.worldSize / 2, mg.storage.bingo.worldSize / 8)
					playerData.usedRTP++
				}
			}
		}

		return super.onClick(index, type, action, element)
	}
}