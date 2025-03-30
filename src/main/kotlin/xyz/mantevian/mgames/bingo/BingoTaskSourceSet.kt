package xyz.mantevian.mgames.bingo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val json = Json

@Serializable
data class BingoTaskSourceSet(
	val items: List<BingoTaskSourceItemEntry>,
	val enchantments: List<BingoTaskSourceEnchantmentEntry>,
	val potions: List<BingoTaskSourcePotionEntry>,
	val picker: List<BingoTaskSourcePickerEntry>
)

@Serializable
data class BingoTaskSourceItemEntry(
	val id: String,
	@SerialName("max_count") val maxCount: Int,
	val tags: List<String>,
	val rarity: Int
)

@Serializable
data class BingoTaskSourceEnchantmentEntry(
	val id: String,
	val rarity: Int
)

@Serializable
data class BingoTaskSourcePotionEntry(
	val id: String,
	val rarity: Int
)

@Serializable
data class BingoTaskSourcePickerEntry(
	val tag: String = "*",
	val count: Int = 1,
	val exclude: Boolean = false
)