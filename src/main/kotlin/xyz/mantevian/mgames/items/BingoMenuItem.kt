package xyz.mantevian.mgames.items

import eu.pb4.polymer.core.api.item.SimplePolymerItem
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.world.World
import xyz.mantevian.mgames.bingo.BingoMenu
import xyz.mantevian.mgames.game
import xyz.mantevian.mgames.game.BingoComponent
import xyz.mantevian.mgames.util.standardText
import xyz.nucleoid.packettweaker.PacketContext

class BingoMenuItem(settings: Settings) : SimplePolymerItem(settings) {
	override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
		if (game.hasComponent<BingoComponent>()) {
			BingoMenu(user as ServerPlayerEntity).open()
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

	override fun inventoryTick(stack: ItemStack?, world: ServerWorld?, entity: Entity?, slot: EquipmentSlot?) {
		super.inventoryTick(stack, world, entity, slot)

		if (stack == null || world == null || entity == null || slot == null) {
			return
		}

		if (entity !is ServerPlayerEntity) {
			return
		}

		val maxPoints = game.getComponent<BingoComponent>()?.maxPoints() ?: 0
		val points = game.getComponent<BingoComponent>()?.countPoints(entity) ?: 0

		stack.set(DataComponentTypes.MAX_STACK_SIZE, 1)
		stack.set(DataComponentTypes.MAX_DAMAGE, maxPoints)
		stack.set(DataComponentTypes.DAMAGE, maxPoints - points)
		stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)

		stack.set(DataComponentTypes.CUSTOM_NAME, standardText("Bingo ($points / $maxPoints ★)"))
	}
}