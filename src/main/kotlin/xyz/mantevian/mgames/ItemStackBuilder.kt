package xyz.mantevian.mgames

import net.minecraft.component.type.DyedColorComponent
import net.minecraft.item.DyeItem
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier

class ItemStackBuilder() {
	private lateinit var stack: ItemStack

	constructor(item: ItemConvertible) : this() {
		stack = ItemStack(item)
	}

	constructor(itemId: String) : this() {
		stack = ItemStack(Registries.ITEM.get(Identifier.of(itemId)))
	}

	fun ofItem(item: ItemConvertible): ItemStackBuilder {
		stack.withItem(item)
		return this
	}

	fun withCount(count: Int): ItemStackBuilder {
		stack.count = count
		return this
	}

	fun enchant(): ItemStackBuilder {
		return this
	}

	fun storeEnchant(): ItemStackBuilder {
		return this
	}

	fun addStatusEffect(): ItemStackBuilder {
		return this
	}

	fun setPotion(): ItemStackBuilder {
		return this
	}

	fun setName(): ItemStackBuilder {
		return this
	}

	fun addLore(): ItemStackBuilder {
		return this
	}

	fun ofColorMix(colors: List<DyeColor>): ItemStackBuilder {
		stack = DyedColorComponent.setColor(stack, colors.map { DyeItem.byColor(it) })
		return this
	}

	fun build(): ItemStack {
		return stack.copy()
	}
}