package xyz.mantevian.mgames.bingo

import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.item.Items
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import xyz.mantevian.mgames.BingoTypedTaskData
import xyz.mantevian.mgames.ItemStackBuilder
import xyz.mantevian.mgames.MG

class BingoMenu(player: ServerPlayerEntity, val mg: MG) : SimpleGui(ScreenHandlerType.GENERIC_9X5, player, false) {
	override fun getTitle(): Text? {
		return Text.literal("Bingo")
	}

	override fun onOpen() {
		super.onOpen()

		for (i in 0..44) {
			setSlot(i, ItemStackBuilder(Items.LIGHT_GRAY_STAINED_GLASS_PANE).build())
		}

		for (i in 0..24) {
			val task = mg.storage.bingo.tasks[i]?.data

			val x = i % 5
			val y = i / 5

			setSlot(y * 9 + x + 2, when (task) {
				is BingoTypedTaskData.Item -> ItemStackBuilder(task.id).withCount(task.count).build()
				is BingoTypedTaskData.Enchantment -> ItemStackBuilder(Items.ENCHANTED_BOOK).storeEnchant().build()
				is BingoTypedTaskData.Potion -> ItemStackBuilder(Items.POTION).setPotion().build()
				is BingoTypedTaskData.ColoredItem -> ItemStackBuilder(task.id).build()

				else -> ItemStackBuilder(Items.BEDROCK).build()
			})
		}
	}
}