package xyz.mantevian.mgames.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.server

fun createObjective(objectiveName: String, displayName: Component): Objective {
    getObjective(objectiveName)?.let {
        return it
    }

    return server.scoreboard.addObjective(
        "$MOD_ID.$objectiveName",
        ObjectiveCriteria.DUMMY,
        displayName,
        ObjectiveCriteria.RenderType.INTEGER,
        true,
        null
    )
}

fun deleteObjective(objectiveName: String) {
    val objective = server.scoreboard.getObjective("$MOD_ID.$objectiveName") ?: return
    server.scoreboard.removeObjective(objective)
}

fun getObjective(objectiveName: String): Objective? {
    return server.scoreboard.getObjective("$MOD_ID.$objectiveName")
}

fun getScore(playerName: String, scoreboardName: String): Int {
    val objective = getObjective(scoreboardName) ?: return 0
    val scoreAccess = server.scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(playerName), objective)
    return scoreAccess.get()
}

fun setScore(playerName: String, objectiveName: String, value: Int, displayName: Component? = null) {
    val objective = getObjective(objectiveName) ?: return
    val scoreAccess = server.scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(playerName), objective)
    scoreAccess.set(value)
    scoreAccess.display(displayName)
}

fun removeScore(playerName: String, objectiveName: String) {
    val objective = getObjective(objectiveName) ?: return
    server.scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(playerName), objective)
}

fun addScore(playerName: String, objectiveName: String, value: Int) {
    val objective = getObjective(objectiveName) ?: return
    val scoreAccess = server.scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(playerName), objective)
    scoreAccess.set(scoreAccess.get() + value)
}

fun setSidebar(title: Component, lines: Map<Int, Component>) {
    val objective = getObjective("sidebar")

    if (objective == null) {
        server.scoreboard.addObjective(
            "$MOD_ID.sidebar",
            ObjectiveCriteria.DUMMY,
            title,
            ObjectiveCriteria.RenderType.INTEGER,
            true,
            BlankFormat.INSTANCE
        )
    } else {
        objective.displayName = title
    }

    server.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective)

    for (i in 0..14) {
        lines[i]?.let {
            setScore("line_$i", "sidebar", 15 - i, it)
        } ?: removeScore("line_$i", "sidebar")
    }
}

fun hideSidebar() {
    server.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null)
}