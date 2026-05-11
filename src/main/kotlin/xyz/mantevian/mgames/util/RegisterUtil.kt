package xyz.mantevian.mgames.util

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.items.BingoMenuItem

fun <T : Item> registerItem(name: String, settings: Item.Properties, itemFactory: (Item.Properties) -> T): T {
    val itemKey: ResourceKey<Item> =
        ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name))

    settings.setId(itemKey)
    val item: T = itemFactory(settings)

    Registry.register(BuiltInRegistries.ITEM, itemKey, item)

    return item
}

object MGItems {
    val BINGO_MENU_ITEM = registerItem(
        "bingo_menu",
        Item.Properties()
    ) {
        BingoMenuItem(it)
    }

    fun init() {

    }
}
