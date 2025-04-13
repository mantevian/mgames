package xyz.mantevian.mgames.sir

import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import xyz.mantevian.mgames.allAttributes
import xyz.mantevian.mgames.allEffects
import xyz.mantevian.mgames.allEnchantments
import xyz.mantevian.mgames.allItems
import xyz.mantevian.mgames.game.Game
import xyz.mantevian.mgames.game.SirComponent
import xyz.mantevian.mgames.game.SpawnBoxComponent
import xyz.mantevian.mgames.game.WorldSizeComponent
import xyz.mantevian.mgames.util.*

fun createSirGame(): Game {
	return Game().apply {
		setComponents(
			SirComponent(),
			SpawnBoxComponent(block = "minecraft:barrier", pos = Vec3i(0, 64, 0)),
			WorldSizeComponent(value = 1000)
		)
	}
}

fun giveRandomItem(
	player: ServerPlayerEntity,
	bannedItems: List<String>,
	enchantmentChance: Double,
	attributeModifierChange: Double
) {
	val items = allItems.filterNot { bannedItems.contains(it.idAsString) }
	val item = when (nextInt(1..50)) {
		1 -> Items.POTION
		2 -> Items.SPLASH_POTION
		3 -> Items.LINGERING_POTION
		4 -> Items.TIPPED_ARROW

		else -> items.shuffled()[0].value()
	}

	val builder = ItemStackBuilder(item)

	while (nextBoolean(enchantmentChance)) {
		val enchantment = allEnchantments.shuffled()[0].value()
		builder.enchant(enchantment, nextInt(1..10))
	}

	if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW) {
		do {
			val effect = allEffects.shuffled()[0]
			val instance = StatusEffectInstance(effect, 20 * nextInt(10..600), nextInt(1..5))
			builder.addStatusEffect(instance)
		} while (nextBoolean(0.33))
	}

	while (nextBoolean(attributeModifierChange)) {
		val attribute = allAttributes.shuffled()[0].value()
		val operation = setOf(
			EntityAttributeModifier.Operation.ADD_VALUE,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		).shuffled()[0]

		val valueRange = when (attribute) {
			EntityAttributes.ARMOR -> -10.0 to 10.0
			EntityAttributes.ARMOR_TOUGHNESS -> -5.0 to 5.0
			EntityAttributes.ATTACK_KNOCKBACK -> -1.0 to 1.0
			EntityAttributes.MOVEMENT_SPEED -> -1.0 to 1.0
			EntityAttributes.SCALE -> -5.0 to 5.0
			EntityAttributes.GRAVITY -> -0.05 to 0.05
			EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE -> -0.5 to 0.5
			EntityAttributes.JUMP_STRENGTH -> -0.5 to 0.5
			EntityAttributes.KNOCKBACK_RESISTANCE -> -0.2 to 0.2

			else -> -2.0 to 2.0
		}

		val modifier = EntityAttributeModifier(
			Identifier.of("mantevian", "mgames/modifier_${nextInt(0..99999999)}"),
			nextDouble(valueRange.first, valueRange.second),
			operation
		)

		val slot = AttributeModifierSlot.ANY
		builder.addAttributeModifier(attribute, modifier, slot)
	}

	player.giveOrDropStack(builder.build())
}