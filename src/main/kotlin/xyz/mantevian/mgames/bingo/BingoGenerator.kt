package xyz.mantevian.mgames.bingo

import xyz.mantevian.mgames.BingoTaskData
import xyz.mantevian.mgames.BingoTypedTaskData
import xyz.mantevian.mgames.MG

class BingoGenerator(sources: BingoTaskSourceSet) {
	private val items: MutableList<BingoTaskSourceItemEntry> = sources.items.toMutableList()
	private val enchantments: MutableList<BingoTaskSourceEnchantmentEntry> = sources.enchantments.toMutableList()
	private val potions: MutableList<BingoTaskSourcePotionEntry> = sources.potions.toMutableList()
	private val picker: BingoPicker = sources.picker

	fun generateTasks(mg: MG): Boolean {
		var attempts = 0

		while (true) {
			val (ok, tasks) = tryGenerateTasks(mg)

			if (ok) {
				tasks.keys.shuffled().forEachIndexed { index, key ->
					mg.storage.bingo.tasks[index] = tasks[key]!!
				}

				break
			}

			attempts++

			if (attempts > 1000) {
				return false
			}
		}

		val target = picker.rules.targetPoints

		attempts = 0

		while (mg.bingo.maxPoints() != target) {
			mg.storage.bingo.tasks.forEach {
				if (mg.bingo.maxPoints() < target) {
					if (it.value.reward in 1..3 && mg.util.nextBoolean(0.1)) {
						it.value.reward++
					}
				}

				if (mg.bingo.maxPoints() > target) {
					if (it.value.reward in 2..4 && mg.util.nextBoolean(0.1)) {
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

	private fun takeItem(tag: String, exceptTags: List<String>): BingoTaskSourceItemEntry? {
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

		return result
	}

	private fun toTask(mg: MG, entry: BingoTaskSourceItemEntry): BingoTaskData {
		var cost = entry.rarity

		val count = if (mg.util.nextBoolean(0.5)) {
			mg.util.nextInt(1..entry.maxCount)
		} else {
			1
		}

		if (count > 1 && count > entry.maxCount / 2) {
			cost++
		}

		return BingoTaskData(cost, BingoTypedTaskData.Item(entry.id, count))
	}

	private fun tryGenerateTasks(mg: MG): Pair<Boolean, Map<Int, BingoTaskData>> {
		val result = mutableMapOf<Int, BingoTaskData>()

		var curr = 0

		if (mg.storage.bingo.taskEnchantment) {
			val source = peekEnchantment()
			result[curr] = BingoTaskData(source.rarity, BingoTypedTaskData.Enchantment(source.id))
			curr++
		}

		if (mg.storage.bingo.taskPotion) {
			val source = peekPotion()
			result[curr] = BingoTaskData(source.rarity, BingoTypedTaskData.Potion(source.id))
			curr++
		}

		picker.list.forEach {
			for (item in takeItems(it.tag, it.exceptTags, it.count, it.excludeTags)) {
				if (curr == 25) {
					return@forEach
				}
				result[curr] = toTask(mg, item)
				curr++
			}
		}

		while (curr < 25) {
			result[curr] = toTask(mg, takeItem("*", listOf()) ?: return false to result)
			curr++
		}

		return true to result
	}
}