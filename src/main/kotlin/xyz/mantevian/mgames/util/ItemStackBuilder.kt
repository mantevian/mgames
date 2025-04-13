package xyz.mantevian.mgames.util

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.*
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.DyeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.item.consume.ApplyEffectsConsumeEffect
import net.minecraft.potion.Potion
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.Unit
import java.util.*

class ItemStackBuilder() {
	private var stack: ItemStack = ItemStack.EMPTY

	constructor(item: ItemConvertible) : this() {
		stack = ItemStack(item)
	}

	constructor(itemId: String) : this() {
		stack = ItemStack(Registries.ITEM.get(Identifier.of(itemId)))
	}

	fun ofItem(item: ItemConvertible): ItemStackBuilder {
		stack = stack.withItem { item as Item }
		return this
	}

	fun ofItem(itemId: String): ItemStackBuilder {
		stack = stack.withItem { (Registries.ITEM.get(Identifier.of(itemId))) }
		return this
	}

	fun withCount(count: Int): ItemStackBuilder {
		stack.count = count
		return this
	}

	fun enchant(enchantment: Enchantment, level: Int): ItemStackBuilder {
		stack.addEnchantment(getEnchantmentEntry(enchantment), level)
		return this
	}

	fun addStatusEffect(effect: StatusEffectInstance): ItemStackBuilder {
		val component = stack.getOrDefault(DataComponentTypes.CONSUMABLE, ConsumableComponent.builder().build())
		val componentBuilder = ConsumableComponent.builder()
		component.onConsumeEffects.forEach { componentBuilder.consumeEffect(it) }
		componentBuilder.consumeEffect(ApplyEffectsConsumeEffect(effect))
		stack.set(DataComponentTypes.CONSUMABLE, componentBuilder.build())
		return this
	}

	fun setPotion(potion: Potion): ItemStackBuilder {
		stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(Registries.POTION.getEntry(potion)))
		return this
	}

	fun setPotionColor(statusEffect: StatusEffect): ItemStackBuilder {
		stack.set(
			DataComponentTypes.POTION_CONTENTS,
			PotionContentsComponent(Optional.empty(), Optional.of(statusEffect.color), listOf(), Optional.empty())
		)
		return this
	}

	fun setItemName(name: Text): ItemStackBuilder {
		stack.set(DataComponentTypes.ITEM_NAME, name)
		return this
	}

	fun setCustomName(name: Text): ItemStackBuilder {
		stack.set(DataComponentTypes.CUSTOM_NAME, name)
		return this
	}

	fun appendCustomName(text: Text): ItemStackBuilder {
		val name = (stack.customName ?: stack.name).copy()
		stack.set(DataComponentTypes.CUSTOM_NAME, name.append(text))
		return this
	}

	fun addLore(lore: Text): ItemStackBuilder {
		val component = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)
		stack.set(DataComponentTypes.LORE, component.with(lore))
		return this
	}

	fun ofColorMix(colors: List<String>): ItemStackBuilder {
		stack = DyedColorComponent.setColor(stack, colors.map { DyeItem.byColor(DyeColor.byName(it, DyeColor.BLACK)) })
		return this
	}

	fun hideAdditionalTooltip(): ItemStackBuilder {
		stack.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
		return this
	}

	fun hideTooltip(): ItemStackBuilder {
		stack.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE)
		return this
	}

	fun setCooldown(cooldownGroup: Identifier, ticks: Int): ItemStackBuilder {
		stack.set(
			DataComponentTypes.USE_COOLDOWN,
			UseCooldownComponent(ticks.toFloat().div(20.0f), Optional.of(cooldownGroup))
		)
		return this
	}

	fun addAttributeModifier(
		attribute: EntityAttribute,
		modifier: EntityAttributeModifier,
		slot: AttributeModifierSlot
	): ItemStackBuilder {
		val component = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
		val newComponent = component.with(getAttributeEntry(attribute), modifier, slot)
		stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, newComponent)

		return this
	}

	fun build(): ItemStack {
		return stack.copy()
	}
}