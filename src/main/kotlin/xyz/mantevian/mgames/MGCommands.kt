package xyz.mantevian.mgames

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import xyz.mantevian.mgames.bingo.BingoMenu

fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
	val root = CommandManager.literal("mg").requires { it.hasPermissionLevel(1) }
	val init = CommandManager.literal("init")
	val bingo = CommandManager.literal("bingo").executes(::initBingoCommand)
	val start = CommandManager.literal("start").executes(::startCommand)

	dispatcher.register(
		root
			.then(init.then(bingo))
			.then(start)
	)
}

private fun initBingoCommand(context: CommandContext<ServerCommandSource>): Int {
	Main.mg?.initGame(GameType.BINGO)

	context.source.sendFeedback({ Text.literal("Initializing the game Bingo") }, true)

	return 1
}

private fun startCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	mg.startGame()

	mg.server.playerManager.playerList.forEach {
		BingoMenu(it, mg).open()
	}

	context.source.sendFeedback({ Text.literal("Starting the game") }, true)

	return 1
}
