package xyz.mantevian.mgames.items

import eu.pb4.polymer.core.api.item.SimplePolymerItem
import net.fabricmc.fabric.api.networking.v1.context.PacketContext
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import xyz.mantevian.mgames.bingo.BingoMenu
import xyz.mantevian.mgames.game
import xyz.mantevian.mgames.game.BingoComponent
import xyz.mantevian.mgames.util.standardText

class BingoMenuItem(settings: Properties) : SimplePolymerItem(settings) {
    override fun use(level: Level, user: Player, hand: InteractionHand): InteractionResult {
        if (game.hasComponent<BingoComponent>()) {
            BingoMenu(user as ServerPlayer).open()
        }

        return InteractionResult.SUCCESS
    }

    override fun getPolymerItem(itemStack: ItemStack, context: PacketContext): Item {
        return Items.NETHER_STAR
    }

    override fun getPolymerItemModel(
        stack: ItemStack,
        context: PacketContext,
        lookup: HolderLookup.Provider
    ): Identifier {
        return Identifier.parse("minecraft:nether_star")
    }

    override fun getName(itemStack: ItemStack): Component {
        return standardText("Bingo")
    }

    override fun inventoryTick(stack: ItemStack, level: ServerLevel, owner: Entity, slot: EquipmentSlot?) {
        super.inventoryTick(stack, level, owner, slot)

        if (slot == null) {
            return
        }

        if (owner !is ServerPlayer) {
            return
        }

        val maxPoints = game.getComponent<BingoComponent>()?.maxPoints() ?: 0
        val points = game.getComponent<BingoComponent>()?.countPoints(owner) ?: 0

        stack.set(DataComponents.MAX_STACK_SIZE, 1)
        stack.set(DataComponents.MAX_DAMAGE, maxPoints)
        stack.set(DataComponents.DAMAGE, maxPoints - points)
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)

        stack.set(DataComponents.CUSTOM_NAME, standardText("Bingo ($points / $maxPoints ★)"))
    }
}