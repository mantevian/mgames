package xyz.mantevian.mgames.bingo

import xyz.mantevian.mgames.BingoTaskData
import xyz.mantevian.mgames.BingoTypedTaskData
import xyz.mantevian.mgames.game
import xyz.mantevian.mgames.game.BingoComponent
import xyz.mantevian.mgames.resourceManager
import xyz.mantevian.mgames.util.calculateColorValue
import xyz.mantevian.mgames.util.nextBoolean
import xyz.mantevian.mgames.util.nextInt
import kotlin.math.floor

class BingoGenerator {
	private val sources = BingoTaskSourceSet(
		resourceManager.get<List<BingoTaskSourceItemEntry>>("bingo/items.json")!!,
		resourceManager.get<List<BingoTaskSourceEnchantmentEntry>>("bingo/enchantments.json")!!,
		resourceManager.get<List<BingoTaskSourcePotionEntry>>("bingo/potions.json")!!,
		resourceManager.get<BingoPicker>("bingo/picker.json")!!
	)

	private val items: MutableList<BingoTaskSourceItemEntry> = sources.items.toMutableList()
	private val enchantments: MutableList<BingoTaskSourceEnchantmentEntry> = sources.enchantments.toMutableList()
	private val potions: MutableList<BingoTaskSourcePotionEntry> = sources.potions.toMutableList()
	private val picker: BingoPicker = sources.picker

	private val bingo = game.getComponent<BingoComponent>()!!

	fun generateTasks(): Boolean {
		var attempts = 0

		while (true) {
			val (ok, tasks) = tryGenerateTasks()

			if (ok) {
				tasks.keys.shuffled().forEachIndexed { index, key ->
					bingo.tasks[index] = tasks[key]!!
				}

				break
			}

			attempts++

			if (attempts > 1000) {
				return false
			}
		}

		val target = picker.rules.targetPoints

		if (target < 0) {
			return true
		}

		attempts = 0

		while (bingo.maxPoints() != target) {
			bingo.tasks.forEach {
				if (bingo.maxPoints() < target) {
					if (it.value.reward in 1..3 && nextBoolean(0.1)) {
						it.value.reward++
					}
				}

				if (bingo.maxPoints() > target) {
					if (it.value.reward in 2..4 && nextBoolean(0.1)) {
						it.value.reward--
					}
				}
			}

			attempts++

			if (attempts > 1000) {
				return false
			}
		}

		return true
	}

	private fun peekItem(): BingoTaskSourceItemEntry = items.shuffled()[0]
	private fun peekEnchantment(): BingoTaskSourceEnchantmentEntry = enchantments.shuffled()[0]
	private fun peekPotion(): BingoTaskSourcePotionEntry = potions.shuffled()[0]

	private fun takeItems(
		tag: String,
		exceptTags: List<String>,
		count: Int,
		excludeTags: List<String>
	): List<BingoTaskSourceItemEntry> {
		if (items.size == 0) {
			return listOf()
		}

		val result = (1..count).map {
			takeItem(tag, exceptTags)!!
		}

		excludeTags.forEach { excludeTag ->
			items.removeAll { item -> item.tags.contains(excludeTag) }
		}

		return result
	}

	private fun takeItem(
		tag: String,
		exceptTags: List<String> = listOf(),
		excludeTags: List<String> = listOf()
	): BingoTaskSourceItemEntry? {
		if (items.size == 0) {
			return null
		}

		val result = if (tag == "*") {
			items.shuffled()[0]
		} else {
			items.filter { item ->
				item.tags.contains(tag) && item.tags.none { tag ->
					exceptTags.contains(tag)
				}
			}.shuffled().getOrNull(0) ?: items.shuffled()[0]
		}

		items.removeAll { item ->
			item.id == result.id
		}

		items.removeAll { item ->
			picker.rules.neverRepeat.any { rule ->
				result.id.contains(rule) && item.id.contains(rule)
			}
		}

		excludeTags.forEach { excludeTag ->
			items.removeAll { item -> item.tags.contains(excludeTag) }
		}

		return result
	}

	private fun toTask(entry: BingoTaskSourceItemEntry): BingoTaskData {
		var cost = entry.rarity

		val count = if (nextBoolean(0.5)) {
			nextInt(1..entry.maxCount)
		} else {
			1
		}

		if (entry.tags.contains("increase_cost_with_count") && count > 1 && count >= entry.maxCount / 2) {
			cost++
		}

		return BingoTaskData(cost, BingoTypedTaskData.Item(entry.id, count))
	}

	private fun tryGenerateTasks(): Pair<Boolean, Map<Int, BingoTaskData>> {
		val result = mutableMapOf<Int, BingoTaskData>()

		var curr = 0

		if (bingo.taskEnchantment) {
			val source = peekEnchantment()
			result[curr] = BingoTaskData(source.rarity, BingoTypedTaskData.Enchantment(source.id))
			curr++
		}

		if (bingo.taskPotion) {
			val source = peekPotion()
			result[curr] = BingoTaskData(source.rarity, BingoTypedTaskData.Potion(source.id))
			curr++
		}

		if (bingo.taskColored) {
			val source = takeItem("any_color", excludeTags = listOf("any_color"))

			if (source != null) {
				val colors = mutableMapOf(
					"warm" to (setOf("red", "orange", "yellow", "pink") to 0.5),
					"cold" to (setOf("light_blue", "blue", "purple", "magenta") to 0.7),
					"bw" to (setOf("white", "light_gray", "gray", "black") to 0.9),
					"rare" to (setOf("lime", "green", "cyan", "brown") to 1.2)
				)

				var cost = 2.0
				val list = mutableListOf<String>()

				for (i in 1..2) {
					val key = colors.keys.shuffled()[0]
					val colorSet = colors[key] ?: (setOf("red") to 0.3)
					val color = colorSet.first.shuffled()[0]
					list.add(color)
					cost += colorSet.second
					colors.remove(key)
				}

				result[curr] = BingoTaskData(
					floor(cost).toInt(),
					BingoTypedTaskData.ColoredItem(source.id, list, calculateColorValue(list))
				)

				list.forEach { color ->
					items.removeAll { item ->
						item.id.startsWith("minecraft:${color}_")
					}
				}

				curr++
			}
		}

		picker.list.forEach {
			for (item in takeItems(it.tag, it.exceptTags, it.count, it.excludeTags)) {
				if (curr == sources.picker.rules.taskCount) {
					return@forEach
				}
				result[curr] = toTask(item)
				curr++
			}
		}

		while (curr < sources.picker.rules.taskCount) {
			result[curr] = toTask(takeItem("*", listOf()) ?: return false to result)
			curr++
		}

		return true to result
	}
}