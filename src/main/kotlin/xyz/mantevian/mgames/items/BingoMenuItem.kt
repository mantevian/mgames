package xyz.mantevian.mgames.items

import eu.pb4.polymer.core.api.item.SimplePolymerItem
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.world.World
import xyz.mantevian.mgames.Main
import xyz.mantevian.mgames.bingo.BingoMenu
import xyz.mantevian.mgames.standardText
import xyz.nucleoid.packettweaker.PacketContext

class BingoMenuItem(settings: Settings) : SimplePolymerItem(settings) {
	override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
		Main.mg?.let {
			BingoMenu(user as ServerPlayerEntity, it).open()
		}

		return ActionResult.SUCCESS
	}

	override fun getPolymerItem(itemStack: ItemStack, context: PacketContext): Item {
		return Items.NETHER_STAR
	}

	override fun getPolymerItemModel(stack: ItemStack, context: PacketContext): Identifier {
		return Identifier.of("minecraft", "nether_star")
	}

	override fun getName(stack: ItemStack?): Text {
		return standardText("Bingo")
	}

	override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
		super.inventoryTick(stack, world, entity, slot, selected)

		val mg = Main.mg ?: return

		if (entity !is ServerPlayerEntity) {
			return
		}

		val maxPoints = mg.bingo.maxPoints()
		val points = mg.bingo.countPoints(entity)

		stack.set(DataComponentTypes.MAX_STACK_SIZE, 1)
		stack.set(DataComponentTypes.MAX_DAMAGE, maxPoints)
		stack.set(DataComponentTypes.DAMAGE, maxPoints - points)
		stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
	}
}