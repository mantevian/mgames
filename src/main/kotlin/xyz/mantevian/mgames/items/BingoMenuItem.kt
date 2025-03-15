package xyz.mantevian.mgames.items

import eu.pb4.polymer.core.api.item.SimplePolymerItem
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.world.World
import xyz.mantevian.mgames.Main
import xyz.mantevian.mgames.bingo.BingoMenu
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
}