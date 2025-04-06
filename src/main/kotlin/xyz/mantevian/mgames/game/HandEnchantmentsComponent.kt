package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.util.Hand
import xyz.mantevian.mgames.util.enchantmentById
import xyz.mantevian.mgames.util.forEachPlayer
import xyz.mantevian.mgames.util.getEnchantmentEntry

@Serializable
@SerialName("hand_enchantments")
class HandEnchantmentsComponent(
	val enchantments: MutableMap<String, Int> = mutableMapOf()
) : GameComponent {
	override fun tick() {
		forEachPlayer { player ->
			val mainHandStack = player.getStackInHand(Hand.MAIN_HAND)
			enchantments.forEach { (id, level) ->
				enchantmentById(id)?.let { enchantment ->
					val entry = getEnchantmentEntry(enchantment)
					if (mainHandStack.canBeEnchantedWith(entry, EnchantingContext.ACCEPTABLE)) {
						mainHandStack.addEnchantment(entry, level)
					}
				}
			}
		}
	}
}