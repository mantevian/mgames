package xyz.mantevian.mgames.util

import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.items.BingoMenuItem

fun registerItem(name: String, settings: Settings, factory: (settings: Settings) -> Item): Item {
	val itemKey: RegistryKey<Item> = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name))
	val item = factory.invoke(settings.registryKey(itemKey))
	Registry.register(Registries.ITEM, itemKey, item)
	return item
}

object MGItems {
	val BINGO_MENU_ITEM = registerItem("bingo_menu", Settings()) {
		BingoMenuItem(it)
	}

	fun init() {

	}
}
