package xyz.mantevian.mgames.bingo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val json = Json {
	serializersModule = SerializersModule {
		polymorphic(BingoTaskSource::class) {
			subclass(BingoTaskSource.None::class)
			subclass(BingoTaskSource.Item::class)
			subclass(BingoTaskSource.Enchantment::class)
		}
	}
}

@Serializable
data class BingoTaskSourceSet (
	val tasks: List<BingoTaskSource>
)

@Serializable
data class BingoTaskSourceEnchantmentEntry (
	val id: String,
	val rarity: Int
)

@Serializable
data class BingoTaskSourcePotionEntry (
	val id: String,
	val rarity: Int
)

@Serializable
sealed interface BingoTaskSource {
	@Serializable
	@SerialName("none")
	data object None : BingoTaskSource

	@Serializable
	@SerialName("item")
	data class Item (
		val id: String,
		@SerialName("min_count") val minCount: Int,
		@SerialName("max_count") val maxCount: Int,
		val tags: List<String>,
		val rarity: Int
	) : BingoTaskSource

	@Serializable
	@SerialName("enchantment")
	data class Enchantment (
		val options: List<BingoTaskSourceEnchantmentEntry>
	) : BingoTaskSource

	@Serializable
	@SerialName("potion")
	data class Potion (
		val options: List<BingoTaskSourcePotionEntry>
	) : BingoTaskSource
}