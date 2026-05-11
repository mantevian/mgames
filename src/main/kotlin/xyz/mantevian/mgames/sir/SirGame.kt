package xyz.mantevian.mgames.sir

import net.minecraft.core.Holder
import net.minecraft.core.Vec3i
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Items
import xyz.mantevian.mgames.allAttributes
import xyz.mantevian.mgames.allEffects
import xyz.mantevian.mgames.allEnchantments
import xyz.mantevian.mgames.allItems
import xyz.mantevian.mgames.game.Game
import xyz.mantevian.mgames.game.SirComponent
import xyz.mantevian.mgames.game.SpawnBoxComponent
import xyz.mantevian.mgames.game.WorldSizeComponent
import xyz.mantevian.mgames.util.ItemStackBuilder
import xyz.mantevian.mgames.util.nextBoolean
import xyz.mantevian.mgames.util.nextDouble
import xyz.mantevian.mgames.util.nextInt

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
    player: ServerPlayer,
    bannedItems: List<String>,
    enchantmentChance: Double,
    attributeModifierChange: Double
) {
    val items = allItems.filterNot { bannedItems.contains(it.toString()) }
    val item = when (nextInt(1..50)) {
        1 -> Items.POTION
        2 -> Items.SPLASH_POTION
        3 -> Items.LINGERING_POTION
        4 -> Items.TIPPED_ARROW

        else -> items.shuffled()[0].asItem()
    }

    val builder = ItemStackBuilder(item)

    while (nextBoolean(enchantmentChance)) {
        val enchantment = allEnchantments.shuffled()[0]
        builder.enchant(enchantment, nextInt(1..10))
    }

    if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW) {
        do {
            val effect = allEffects.shuffled()[0]
            val instance = MobEffectInstance(Holder.direct(effect), 20 * nextInt(10..600), nextInt(1..5))
            builder.addStatusEffect(instance)
        } while (nextBoolean(0.33))
    }

    while (nextBoolean(attributeModifierChange)) {
        val attribute = allAttributes.shuffled()[0]
        val operation = setOf(
            AttributeModifier.Operation.ADD_VALUE,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ).shuffled()[0]

        val valueRange = when (attribute) {
            Attributes.ARMOR -> -10.0 to 10.0
            Attributes.ARMOR_TOUGHNESS -> -5.0 to 5.0
            Attributes.ATTACK_KNOCKBACK -> -1.0 to 1.0
            Attributes.MOVEMENT_SPEED -> -1.0 to 1.0
            Attributes.SCALE -> -5.0 to 5.0
            Attributes.GRAVITY -> -0.05 to 0.05
            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE -> -0.5 to 0.5
            Attributes.JUMP_STRENGTH -> -0.5 to 0.5
            Attributes.KNOCKBACK_RESISTANCE -> -0.2 to 0.2

            else -> -2.0 to 2.0
        }

        val modifier = AttributeModifier(
            Identifier.parse("mantevian:mgames/modifier_${nextInt(0..99999999)}"),
            nextDouble(valueRange.first, valueRange.second),
            operation
        )

        val slot = EquipmentSlotGroup.ANY
        builder.addAttributeModifier(attribute, modifier, slot)
    }

    player.inventory.addAndPickItem(builder.build())
}