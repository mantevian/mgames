package xyz.mantevian.mgames

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.ServerCommandSource

fun registerCommands(
	dispatcher: CommandDispatcher<ServerCommandSource>,
	registryAccess: CommandRegistryAccess,
	env: RegistrationEnvironment
) {
	val root = CommandManager.literal("mg").requires { it.hasPermissionLevel(1) }

	val initBingo = CommandManager.literal("bingo").executes(::initBingoCommand)

	val init = CommandManager.literal("init")
		.then(initBingo)

	val start = CommandManager.literal("start").executes(::startCommand)

	val configBingoGameTime = CommandManager.literal("game_time")
		.then(
			CommandManager.argument("value", StringArgumentType.greedyString())
				.executes(::configBingoGameTimeCommand)
		)

	val configBingoWorldSize = CommandManager.literal("world_size")
		.then(
			CommandManager.argument("value", IntegerArgumentType.integer(0, 100000))
				.executes(::configBingoWorldSizeCommand)
		)

	val configBingoEnchantment = CommandManager.literal("enchantment")
		.then(
			CommandManager.argument(
				"id",
				RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENCHANTMENT)
			)
				.then(
					CommandManager.argument("level", IntegerArgumentType.integer(1, 127))
						.executes(::configBingoEnchantmentCommand)
				)
		)

	val configBingoTaskUseSet = CommandManager.literal("use_set")
		.executes(::configBingoTaskUseNoSetCommand)
		.then(
			CommandManager.argument("value", StringArgumentType.greedyString())
				.executes(::configBingoTaskUseSetCommand)
		)

	val configBingoUnbreakable = CommandManager.literal("unbreakable")
		.then(
			CommandManager.argument("value", BoolArgumentType.bool())
				.executes(::configBingoUnbreakableCommand)
		)

	val configBingoTaskEnchantment = CommandManager.literal("task_enchantment")
		.then(
			CommandManager.argument("value", BoolArgumentType.bool())
				.executes(::configBingoTaskEnchantmentCommand)
		)

	val configBingoTaskPotion = CommandManager.literal("task_potion")
		.then(
			CommandManager.argument("value", BoolArgumentType.bool())
				.executes(::configBingoTaskPotionCommand)
		)

	val configBingo = CommandManager.literal("bingo")
		.then(configBingoGameTime)
		.then(configBingoWorldSize)
		.then(configBingoEnchantment)
		.then(configBingoUnbreakable)
		.then(configBingoTaskEnchantment)
		.then(configBingoTaskPotion)
		.then(configBingoTaskUseSet)

	val config = CommandManager.literal("config")
		.then(configBingo)

	dispatcher.register(
		root
			.then(init)
			.then(config)
			.then(start)
	)
}

private fun initBingoCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	mg.initGame(GameType.BINGO)

	context.source.sendFeedback({ standardText("Initializing the game Bingo") }, true)

	return 1
}

private fun startCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	context.source.sendFeedback({ standardText("Starting the game") }, true)

	mg.startGame()

	return 1
}

private fun configBingoGameTimeCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	val value = StringArgumentType.getString(context, "value")

	if (mg.storage.bingo.gameTime.set(value)) {
		context.source.sendFeedback({ standardText("Set game time to $value") }, true)
		return 1
	}

	context.source.sendFeedback({ standardText("Couldn't parse the time") }, false)
	return 0
}

private fun configBingoWorldSizeCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	val value = IntegerArgumentType.getInteger(context, "value")

	mg.storage.bingo.worldSize = value

	context.source.sendFeedback({ standardText("Set world size to $value") }, true)

	return 1
}

private fun configBingoEnchantmentCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	val id = RegistryEntryReferenceArgumentType.getEnchantment(context, "id")
	val level = IntegerArgumentType.getInteger(context, "level")

	mg.storage.bingo.handEnchantments[id.idAsString] = level

	context.source.sendFeedback({ standardText("Set enchantment $id to level $level") }, true)

	return 1
}

private fun configBingoUnbreakableCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	val value = BoolArgumentType.getBool(context, "value")

	mg.storage.bingo.unbreakableItems = value

	context.source.sendFeedback({ standardText("Set unbreakable items to $value") }, true)

	return 1
}

private fun configBingoTaskEnchantmentCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	val value = BoolArgumentType.getBool(context, "value")

	mg.storage.bingo.taskEnchantment = value

	context.source.sendFeedback({ standardText("Set task of enchantment to $value") }, true)

	return 1
}

private fun configBingoTaskPotionCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	val value = BoolArgumentType.getBool(context, "value")

	mg.storage.bingo.taskPotion = value

	context.source.sendFeedback({ standardText("Set task of potion to $value") }, true)

	return 1
}

private fun configBingoTaskUseSetCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	val value = StringArgumentType.getString(context, "value")

	mg.storage.bingo.useSet = value

	context.source.sendFeedback({ standardText("Using the bingo set $value") }, true)

	return 1
}

private fun configBingoTaskUseNoSetCommand(context: CommandContext<ServerCommandSource>): Int {
	val mg = Main.mg ?: return 0

	mg.storage.bingo.useSet = null

	context.source.sendFeedback({ standardText("No longer using a bingo set") }, true)

	return 1
}