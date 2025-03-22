package xyz.mantevian.mgames

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.item.DyeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.Unit
import java.util.*

class ItemStackBuilder() {
	private var stack: ItemStack = ItemStack.EMPTY
	private var mg: MG? = null

	constructor(mg: MG) : this() {
		this.mg = mg
	}

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
		mg?.let {
			stack.addEnchantment(it.util.getEnchantmentEntry(enchantment), level)
		}

		return this
	}

	fun addStatusEffect(): ItemStackBuilder {
		return this
	}

	fun setPotion(potion: Potion): ItemStackBuilder {
		stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(Registries.POTION.getEntry(potion)))
		return this
	}

	fun setPotionColor(statusEffect: StatusEffect): ItemStackBuilder {
		stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(Optional.empty(), Optional.of(statusEffect.color), listOf(), Optional.empty()))
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

	fun addLore(lore: Text): ItemStackBuilder {
		val component = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)
		stack.set(DataComponentTypes.LORE, component.with(lore))
		return this
	}

	fun ofColorMix(colors: List<DyeColor>): ItemStackBuilder {
		stack = DyedColorComponent.setColor(stack, colors.map { DyeItem.byColor(it) })
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

	fun build(): ItemStack {
		return stack.copy()
	}
}