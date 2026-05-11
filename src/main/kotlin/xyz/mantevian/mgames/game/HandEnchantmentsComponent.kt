package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.core.Holder
import xyz.mantevian.mgames.util.enchantmentById
import xyz.mantevian.mgames.util.forEachPlayer

@Serializable
@SerialName("hand_enchantments")
class HandEnchantmentsComponent(
    val enchantments: MutableMap<String, Int> = mutableMapOf()
) : GameComponent {
    override fun tick() {
        forEachPlayer { player ->
            val mainHandStack = player.mainHandItem
            enchantments.forEach { (id, level) ->
                enchantmentById(id)?.let { enchantment ->
                    val holder = Holder.direct(enchantment)
                    if (mainHandStack.canBeEnchantedWith(holder, EnchantingContext.ACCEPTABLE)) {
                        mainHandStack.enchant(holder, level)
                    }
                }
            }
        }
    }
}