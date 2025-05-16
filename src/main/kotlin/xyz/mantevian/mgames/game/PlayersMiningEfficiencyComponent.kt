package xyz.mantevian.mgames.game

import com.google.common.collect.HashMultimap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.util.forEachPlayer

@Serializable
@SerialName("mining_efficiency")
class PlayersMiningEfficiencyComponent(var value: Double = 0.0) : GameComponent {
	override fun tick() {
		forEachPlayer { player ->
			val map = HashMultimap.create<RegistryEntry<EntityAttribute>?, EntityAttributeModifier?>()
			map.put(
				EntityAttributes.MINING_EFFICIENCY,
				EntityAttributeModifier(
					Identifier.of("mantevian", "$MOD_ID/mining_efficiency"),
					value,
					EntityAttributeModifier.Operation.ADD_VALUE
				)
			)

			player.attributes.addTemporaryModifiers(map)
		}
	}
}