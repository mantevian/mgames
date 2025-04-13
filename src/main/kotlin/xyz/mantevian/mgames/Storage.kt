package xyz.mantevian.mgames

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import xyz.mantevian.mgames.game.*
import xyz.mantevian.mgames.util.MGDuration
import java.nio.file.Files
import java.nio.file.Paths

val json = Json {
	serializersModule = SerializersModule {
		polymorphic(GameComponent::class) {
			subclass(BingoComponent::class)
			subclass(GameTimeComponent::class)
			subclass(HandEnchantmentsComponent::class)
			subclass(HandUnbreakableComponent::class)
			subclass(SpawnBoxComponent::class)
			subclass(WorldSizeComponent::class)
			subclass(KeepInventoryComponent::class)
			subclass(SirComponent::class)
		}

		polymorphic(BingoTypedTaskData::class) {
			subclass(BingoTypedTaskData.None::class)
			subclass(BingoTypedTaskData.Item::class)
			subclass(BingoTypedTaskData.Enchantment::class)
			subclass(BingoTypedTaskData.Potion::class)
			subclass(BingoTypedTaskData.ColoredItem::class)
			subclass(BingoTypedTaskData.OneOfItems::class)
		}
	}

	prettyPrint = true
	encodeDefaults = true
	explicitNulls = true
}

fun load(): Game {
	val path = Paths.get("data", "mgames", "storage.json")
	if (Files.exists(path)) {
		return try {
			json.decodeFromString(Files.readString(path))
		} catch (e: Exception) {
			Game()
		}
	}

	return Game()
}

fun save() {
	val path = Paths.get("data", "mgames", "storage.json")
	if (Files.notExists(path)) {
		Files.createDirectories(path.parent)
		Files.createFile(path)
	}

	try {
		val string = json.encodeToString(game)
		Files.writeString(path, string)
	} catch (e: Exception) {
		println(e)
	}
}

@Serializable
data class BingoPlayer(
	@SerialName("tasks")
	val tasks: MutableMap<Int, MGDuration> = mutableMapOf(),

	@SerialName("used_rtp")
	var usedRTP: Int = 0
)

@Serializable
data class BingoTaskData(
	var reward: Int = 0,
	val data: BingoTypedTaskData = BingoTypedTaskData.None
)

@Serializable
sealed interface BingoTypedTaskData {
	@Serializable
	@SerialName("none")
	data object None : BingoTypedTaskData

	@Serializable
	@SerialName("item")
	data class Item(
		val id: String = "",
		val count: Int = 0
	) : BingoTypedTaskData

	@Serializable
	@SerialName("enchantment")
	data class Enchantment(
		val id: String = ""
	) : BingoTypedTaskData

	@Serializable
	@SerialName("potion")
	data class Potion(
		val id: String = ""
	) : BingoTypedTaskData

	@Serializable
	@SerialName("colored_item")
	data class ColoredItem(
		val id: String = "",
		@SerialName("color_names") val colorNames: List<String> = listOf(),
		@SerialName("color_value") val colorValue: Int = 0
	) : BingoTypedTaskData

	@Serializable
	@SerialName("one_of_items")
	data class OneOfItems(
		val ids: List<String> = listOf(),
		val count: Int = 0
	) : BingoTypedTaskData
}

@Serializable
class SirPlayer {
	@SerialName("deaths")
	var deaths: Int = 0
}