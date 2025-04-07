package xyz.mantevian.mgames.bingo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BingoTaskSourceSet(
	val items: List<BingoTaskSourceItemEntry>,
	val enchantments: List<BingoTaskSourceEnchantmentEntry>,
	val potions: List<BingoTaskSourcePotionEntry>,
	val picker: BingoPicker
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
data class BingoPicker(
	val rules: BingoPickerRuleSet,
	val list: List<BingoPickerEntry>
)

@Serializable
data class BingoPickerRuleSet(
	@SerialName("never_repeat") val neverRepeat: List<String> = listOf(),
	@SerialName("target_points") val targetPoints: Int = 60,
	@SerialName("task_count") val taskCount: Int = 25
)

@Serializable
data class BingoPickerEntry(
	val tag: String = "*",
	@SerialName("except_tags") val exceptTags: List<String> = listOf(),
	val count: Int = 1,
	@SerialName("exclude_tags") val excludeTags: List<String> = listOf()
)