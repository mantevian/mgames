package xyz.mantevian.mgames

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.LoreComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.DyeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier

class ItemStackBuilder() {
	private lateinit var stack: ItemStack
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
		stack.withItem(item)
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

	fun storeEnchant(): ItemStackBuilder {
		return this
	}

	fun addStatusEffect(): ItemStackBuilder {
		return this
	}

	fun setPotion(): ItemStackBuilder {
		return this
	}

	fun setName(name: Text): ItemStackBuilder {
		stack.set(DataComponentTypes.ITEM_NAME, name)
		return this
	}

	fun setCustomName(name: Text): ItemStackBuilder {
		stack.set(DataComponentTypes.CUSTOM_NAME, name)
		return this
	}

	fun addLore(lore: Text): ItemStackBuilder {
		val component = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)
		component.with(lore)
		stack.set(DataComponentTypes.LORE, component)
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