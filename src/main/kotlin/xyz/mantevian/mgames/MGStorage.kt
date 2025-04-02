package xyz.mantevian.mgames

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.nio.file.Files
import java.nio.file.Paths

val json = Json {
	serializersModule = SerializersModule {
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

fun default(): MGStorage {
	return MGStorage().apply {
		state = GameState.NOT_INIT
	}
}

fun load(): MGStorage {
	val path = Paths.get("data", "mgames", "storage.json")
	if (Files.exists(path)) {
		return try {
			json.decodeFromString(Files.readString(path))
		} catch (e: Exception) {
			default()
		}
	}

	return default()
}

fun save(storage: MGStorage) {
	val path = Paths.get("data", "mgames", "storage.json")
	if (Files.notExists(path)) {
		Files.createDirectories(path.parent)
		Files.createFile(path)
	}
	Files.writeString(path, json.encodeToString(storage))
}

@Serializable
class MGStorage {
	@SerialName("game")
	var game: GameType? = null

	@SerialName("time")
	var time = MGDuration.zero()

	@SerialName("bingo")
	var bingo = BingoStorage()

	@SerialName("state")
	var state: GameState? = null
}

@Serializable
enum class GameType {
	@SerialName("bingo")
	BINGO
}

@Serializable
enum class GameState {
	@SerialName("not_init")
	NOT_INIT,

	@SerialName("waiting")
	WAITING,

	@SerialName("playing")
	PLAYING
}

@Serializable
class BingoStorage {
	@SerialName("game_time")
	var gameTime = MGDuration.fromHours(1, 30, 0)

	@SerialName("world_size")
	var worldSize: Int = 20000

	@SerialName("hand_enchantments")
	var handEnchantments: MutableMap<String, Int> = mutableMapOf()

	@SerialName("unbreakable_items")
	var unbreakableItems: Boolean = false

	@SerialName("use_set")
	var useSet: String? = null

	@SerialName("task_enchantment")
	var taskEnchantment: Boolean = true

	@SerialName("task_potion")
	var taskPotion: Boolean = true

	@SerialName("tasks")
	var tasks: MutableMap<Int, BingoTaskData> = mutableMapOf()

	@SerialName("players")
	var players: MutableMap<String, BingoPlayer> = mutableMapOf()

	fun reset() {
		gameTime = MGDuration.fromHours(1, 30, 0)
		worldSize = 20000
		handEnchantments = mutableMapOf()
		unbreakableItems = false
		useSet = null
		taskEnchantment = true
		taskPotion = true
		tasks = mutableMapOf()
		players = mutableMapOf()
	}

	fun reinit() {
		tasks = mutableMapOf()
		players = mutableMapOf()
	}
}

@Serializable
class BingoPlayer {
	val tasks: MutableMap<Int, MGDuration?> = mutableMapOf()
	var usedRTP: Int = 0
}

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