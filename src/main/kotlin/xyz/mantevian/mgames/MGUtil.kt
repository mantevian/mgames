package xyz.mantevian.mgames

import net.minecraft.component.type.DyedColorComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.Potion
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.scoreboard.ScoreHolder
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier

infix fun ItemStack.isId(id: String): Boolean {
	return Registries.ITEM.getId(this.item).toString() == id
}

infix fun ItemStack.isId(ids: List<String>): Boolean {
	return ids.contains(Registries.ITEM.getId(this.item).toString())
}

class MGUtil(private val mg: MG) {
	fun resetPlayersMinecraftStats() {
		mg.server.playerManager.playerList.forEach {
			it.clearStatusEffects()
			it.advancementTracker.clearCriteria()
			it.setExperienceLevel(0)
			it.setExperiencePoints(0)
			it.inventory.clear()
		}
	}

	fun tpPlayersToWorldBottom() {
		mg.server.playerManager.playerList.forEach { player ->
			player.teleport(
				mg.server.overworld,
				0.0,
				-62.0,
				0.0,
				setOf(),
				0.0f,
				0.0f,
				false
			)

			player.setSpawnPointFrom(player)
		}
	}

	fun infiniteEffect(effect: RegistryEntry<StatusEffect>, level: Int = 0) {
		mg.server.playerManager.playerList.forEach { player ->
			player.addStatusEffect(StatusEffectInstance(effect, -1, level, true, false))
		}
	}

	fun announce(text: Text) {
		mg.server.playerManager.broadcast(text, false)
	}

	fun itemById(id: String): Item {
		return Registries.ITEM.get(Identifier.of(id))
	}

	fun enchantmentById(id: String): Enchantment? {
		return mg.server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).get(Identifier.of(id))
	}

	fun potionById(id: String): Potion? {
		return Registries.POTION.get(Identifier.of(id))
	}

	fun statusEffectById(id: String): StatusEffect? {
		return Registries.STATUS_EFFECT.get(Identifier.of(id))
	}

	fun createScoreboardSidebar(scoreboardName: String, displayName: String) {
		if (mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") != null) {
			return
		}
		val objective = mg.server.scoreboard.addObjective("${Main.MOD_ID}.$scoreboardName", ScoreboardCriterion.DUMMY, Text.literal(displayName), ScoreboardCriterion.RenderType.INTEGER, true, null)
		mg.server.scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective)
	}

	fun deleteScoreboard(scoreboardName: String) {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return
		mg.server.scoreboard.removeObjective(objective)
	}

	fun getScore(playerName: String, scoreboardName: String): Int {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return 0
		val scoreAccess = mg.server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
		return scoreAccess.score
	}

	fun setScore(playerName: String, scoreboardName: String, value: Int) {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return
		val scoreAccess = mg.server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
		scoreAccess.score = value
	}

	fun addScore(playerName: String, scoreboardName: String, value: Int) {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return
		val scoreAccess = mg.server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
		scoreAccess.score += value
	}

	fun calculateColorValue(colors: List<String>): Int {
		val stack = ItemStackBuilder(Items.WOLF_ARMOR).ofColorMix(colors.map { DyeColor.byName(it, DyeColor.BLACK)!! }).build()
		return DyedColorComponent.getColor(stack, 0)
	}

	fun nextInt(range: IntRange): Int {
		return mg.server.overworld.random.nextBetween(range.first, range.last)
	}
}