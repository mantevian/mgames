package xyz.mantevian.mgames.bingo

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import xyz.mantevian.mgames.BingoTypedTaskData
import xyz.mantevian.mgames.ItemStackBuilder
import xyz.mantevian.mgames.MG


class BingoMenuFactory(val mg: MG) : NamedScreenHandlerFactory {
	override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
		return BingoMenu(syncId, playerInventory, mg)
	}

	override fun getDisplayName(): Text {
		return Text.literal("Bingo")
	}
}

class BingoMenu : ScreenHandler {
	constructor(syncId: Int, playerInventory: PlayerInventory, mg: MG): this(syncId, playerInventory, SimpleInventory(45), mg)

	constructor(syncId: Int, playerInventory: PlayerInventory, inventory: Inventory, mg: MG) : super(ScreenHandlerType.GENERIC_9X5, syncId) {
		for (i in 0..44) {
			addSlot(Slot(inventory, i, 0, 0))

			getSlot(i).stack = ItemStackBuilder(Items.LIGHT_GRAY_STAINED_GLASS_PANE).build()
		}

		for (i in 0..24) {
			val task = mg.storage.bingo.tasks[i + 1]?.data

			val x = i % 5
			val y = i / 5

			getSlot(y * 5 + x + 2).stack = when (task) {
				is BingoTypedTaskData.Item -> ItemStackBuilder(task.id).build()
				is BingoTypedTaskData.Enchantment -> ItemStackBuilder(Items.ENCHANTED_BOOK).storeEnchant().build()
				is BingoTypedTaskData.Potion -> ItemStackBuilder(Items.POTION).setPotion().build()
				is BingoTypedTaskData.ColoredItem -> ItemStackBuilder(task.id).build()

				else -> ItemStackBuilder(Items.LIGHT_GRAY_STAINED_GLASS_PANE).build()
			}
		}
	}

	override fun quickMove(player: PlayerEntity, slot: Int): ItemStack {
		return ItemStack.EMPTY
	}

	override fun canUse(player: PlayerEntity?): Boolean {
		return true
	}

	override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {

	}


}