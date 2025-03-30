package xyz.mantevian.mgames.bingo

import xyz.mantevian.mgames.BingoTaskData
import xyz.mantevian.mgames.BingoTypedTaskData
import xyz.mantevian.mgames.MG

class BingoGenerator(sources: BingoTaskSourceSet) {
	private val items: MutableList<BingoTaskSourceItemEntry> = sources.items.toMutableList()
	private val enchantments: MutableList<BingoTaskSourceEnchantmentEntry> = sources.enchantments.toMutableList()
	private val potions: MutableList<BingoTaskSourcePotionEntry> = sources.potions.toMutableList()
	private val picker: MutableList<BingoTaskSourcePickerEntry> = sources.picker.toMutableList()

	fun generateTasks(mg: MG) {
		while (true) {
			val (ok, tasks) = tryGenerateTasks(mg)

			if (ok) {
				tasks.keys.shuffled().forEachIndexed { index, key ->
					mg.storage.bingo.tasks[index] = tasks[key]!!
				}

				break
			}
		}
	}

	private fun peekItem(): BingoTaskSourceItemEntry = items.shuffled()[0]
	private fun peekEnchantment(): BingoTaskSourceEnchantmentEntry = enchantments.shuffled()[0]
	private fun peekPotion(): BingoTaskSourcePotionEntry = potions.shuffled()[0]

	private fun takeItems(tag: String, count: Int, exclude: Boolean): List<BingoTaskSourceItemEntry> {
		if (items.size == 0) {
			return listOf()
		}

		val result = (1..count).map {
			takeItem(tag)!!
		}

		if (exclude) {
			items.removeAll { it.tags.contains(tag) }
		}

		return result
	}

	private fun takeItem(tag: String): BingoTaskSourceItemEntry? {
		if (items.size == 0) {
			return null
		}

		val result = if (tag == "*") {
			items.shuffled()[0]
		} else {
			items.filter { it.tags.contains(tag) }.shuffled().getOrNull(0) ?: items.shuffled()[0]
		}

		items.removeAll { it.id == result.id }

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

		picker.forEach {
			for (item in takeItems(it.tag, it.count, it.exclude)) {
				result[curr] = toTask(mg, item)
				curr++
			}
		}

		while (curr < 25) {
			result[curr] = toTask(mg, takeItem("*") ?: return false to result)
			curr++
		}

		return true to result
	}
}