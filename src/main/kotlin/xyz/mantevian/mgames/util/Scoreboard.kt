package xyz.mantevian.mgames.util

import net.minecraft.scoreboard.ScoreHolder
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.number.BlankNumberFormat
import net.minecraft.text.Text
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.server

fun createObjective(objectiveName: String, displayName: Text): ScoreboardObjective {
	getObjective(objectiveName)?.let {
		return it
	}

	return server.scoreboard.addObjective(
		"$MOD_ID.$objectiveName",
		ScoreboardCriterion.DUMMY,
		displayName,
		ScoreboardCriterion.RenderType.INTEGER,
		true,
		null
	)
}

fun deleteObjective(objectiveName: String) {
	val objective = server.scoreboard.getNullableObjective("$MOD_ID.$objectiveName") ?: return
	server.scoreboard.removeObjective(objective)
}

fun getObjective(objectiveName: String): ScoreboardObjective? {
	return server.scoreboard.getNullableObjective("$MOD_ID.$objectiveName")
}

fun getScore(playerName: String, scoreboardName: String): Int {
	val objective = getObjective(scoreboardName) ?: return 0
	val scoreAccess = server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
	return scoreAccess.score
}

fun setScore(playerName: String, objectiveName: String, value: Int, displayName: Text? = null) {
	val objective = getObjective(objectiveName) ?: return
	val scoreAccess = server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
	scoreAccess.score = value
	scoreAccess.displayText = displayName
}

fun removeScore(playerName: String, objectiveName: String) {
	val objective = getObjective(objectiveName) ?: return
	server.scoreboard.removeScore(ScoreHolder.fromName(playerName), objective)
}

fun addScore(playerName: String, objectiveName: String, value: Int) {
	val objective = getObjective(objectiveName) ?: return
	val scoreAccess = server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
	scoreAccess.score += value
}

fun setSidebar(title: Text, lines: Map<Int, Text>) {
	val objective = getObjective("sidebar")

	if (objective == null) {
		server.scoreboard.addObjective(
			"$MOD_ID.sidebar",
			ScoreboardCriterion.DUMMY,
			title,
			ScoreboardCriterion.RenderType.INTEGER,
			true,
			BlankNumberFormat.INSTANCE
		)
	} else {
		objective.displayName = title
	}

	server.scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective)

	for (i in 0..14) {
		lines[i]?.let {
			setScore("line_$i", "sidebar", 15 - i, it)
		} ?: removeScore("line_$i", "sidebar")
	}
}

fun hideSidebar() {
	server.scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null)
}