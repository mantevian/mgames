package xyz.mantevian.mgames

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.ServerCommandSource
import xyz.mantevian.mgames.bingo.createBingoGame
import xyz.mantevian.mgames.game.*
import xyz.mantevian.mgames.sir.createSirGame
import xyz.mantevian.mgames.util.Vec3i
import xyz.mantevian.mgames.util.island
import xyz.mantevian.mgames.util.standardText

fun registerCommands(
	dispatcher: CommandDispatcher<ServerCommandSource>,
	registryAccess: CommandRegistryAccess,
	env: RegistrationEnvironment
) {
	val root = CommandManager.literal("mg").requires { it.hasPermissionLevel(1) }

	val test = CommandManager.literal("test").then(
		CommandManager.argument("value", StringArgumentType.greedyString())
			.executes(::testCommand)
	)

	val initBingo = CommandManager.literal("bingo").executes(::initBingoCommand)
	val initSir = CommandManager.literal("sir").executes(::initSirCommand)

	val init = CommandManager.literal("init")
		.then(initBingo)
		.then(initSir)

	val start = CommandManager.literal("start").executes(::startCommand)

	val configGameTime = CommandManager.literal("game_time")
		.then(
			CommandManager.argument("value", StringArgumentType.greedyString())
				.executes(::configGameTimeCommand)
		)

	val configWorldSize = CommandManager.literal("world_size")
		.then(
			CommandManager.argument("value", IntegerArgumentType.integer(0, 100000))
				.executes(::configWorldSizeCommand)
		)

	val configEnchantment = CommandManager.literal("enchantment")
		.then(
			CommandManager.argument(
				"id",
				RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENCHANTMENT)
			)
				.then(
					CommandManager.argument("level", IntegerArgumentType.integer(1, 127))
						.executes(::configEnchantmentCommand)
				)
		)

	val configMiningEfficiency = CommandManager.literal("mining_efficiency")
		.then(
			CommandManager.argument("value", DoubleArgumentType.doubleArg(-1024.0, 1024.0))
				.executes(::configMiningEfficiencyCommand)
		)

	val configUnbreakable = CommandManager.literal("unbreakable")
		.then(
			CommandManager.argument("value", BoolArgumentType.bool())
				.executes(::configUnbreakableCommand)
		)

	val configKeepInventory = CommandManager.literal("keep_inventory")
		.then(
			CommandManager.argument("value", BoolArgumentType.bool())
				.executes(::configKeepInventoryCommand)
		)

	val configBingoTaskUseSet = CommandManager.literal("use_set")
		.executes(::configBingoTaskUseNoSetCommand)
		.then(
			CommandManager.argument("value", StringArgumentType.greedyString())
				.executes(::configBingoTaskUseSetCommand)
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

	val configBingoTaskColored = CommandManager.literal("task_colored")
		.then(
			CommandManager.argument("value", BoolArgumentType.bool())
				.executes(::configBingoTaskColoredCommand)
		)

	val configSirLifeCount = CommandManager.literal("life_count")
		.then(
			CommandManager.argument("value", IntegerArgumentType.integer(1, 100))
				.executes(::configSirLifeCountCommand)
		)

	val configSirItemTimer = CommandManager.literal("item_timer")
		.then(
			CommandManager.argument("value", StringArgumentType.greedyString())
				.executes(::configSirItemTimerCommand)
		)

	val configSirRadius = CommandManager.literal("radius")
		.then(
			CommandManager.argument("value", IntegerArgumentType.integer(10, 200))
				.executes(::configSirRadiusCommand)
		)

	val configSirEnchantmentChance = CommandManager.literal("enchantment_chance")
		.then(
			CommandManager.argument("value", DoubleArgumentType.doubleArg(0.0, 0.9))
				.executes(::configSirEnchantmentChanceCommand)
		)

	val configSirAttributeModifierChance = CommandManager.literal("attribute_modifier_chance")
		.then(
			CommandManager.argument("value", DoubleArgumentType.doubleArg(0.0, 0.9))
				.executes(::configSirAttributeModifierChanceCommand)
		)

	val configBingo = CommandManager.literal("bingo")
		.then(configBingoTaskEnchantment)
		.then(configBingoTaskPotion)
		.then(configBingoTaskColored)
		.then(configBingoTaskUseSet)

	val configSir = CommandManager.literal("sir")
		.then(configSirLifeCount)
		.then(configSirItemTimer)
		.then(configSirRadius)
		.then(configSirEnchantmentChance)
		.then(configSirAttributeModifierChance)

	val config = CommandManager.literal("config")
		.then(configBingo)
		.then(configSir)
		.then(configGameTime)
		.then(configWorldSize)
		.then(configEnchantment)
		.then(configUnbreakable)
		.then(configKeepInventory)
		.then(configMiningEfficiency)

	dispatcher.register(
		root
			.then(init)
			.then(config)
			.then(start)
	)

	dispatcher.register(test)
}

private fun initBingoCommand(context: CommandContext<ServerCommandSource>): Int {
	game = createBingoGame()
	game.init()

	context.source.sendFeedback({ standardText("Initializing Bingo") }, true)

	return 1
}

private fun initSirCommand(context: CommandContext<ServerCommandSource>): Int {
	game = createSirGame()
	game.init()

	context.source.sendFeedback({ standardText("Initializing Skyblock Item Randomizer") }, true)

	return 1
}

private fun startCommand(context: CommandContext<ServerCommandSource>): Int {
	context.source.sendFeedback({ standardText("Starting the game") }, true)

	startGame()

	return 1
}

private fun configGameTimeCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = StringArgumentType.getString(context, "value")

	val component = game.getComponentOrSetDefault<GameTimeComponent>()

	if (component.value.set(value)) {
		context.source.sendFeedback({ standardText("Set game time to $value") }, true)
		return 1
	}

	context.source.sendFeedback({ standardText("Couldn't parse the time") }, false)
	return 0
}

private fun configWorldSizeCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = IntegerArgumentType.getInteger(context, "value")

	val component = game.getComponentOrSetDefault<WorldSizeComponent>()

	component.update(value)

	context.source.sendFeedback({ standardText("Set world size to $value") }, true)

	return 1
}

private fun configMiningEfficiencyCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = DoubleArgumentType.getDouble(context, "value")

	val component = game.getComponentOrSetDefault<PlayersMiningEfficiencyComponent>()

	component.value = value

	context.source.sendFeedback({ standardText("Set players' mining efficiency to $value") }, true)

	return 1
}

private fun configEnchantmentCommand(context: CommandContext<ServerCommandSource>): Int {
	val id = RegistryEntryReferenceArgumentType.getEnchantment(context, "id")
	val level = IntegerArgumentType.getInteger(context, "level")

	val component = game.getComponentOrSetDefault<HandEnchantmentsComponent>()

	component.enchantments[id.idAsString] = level

	context.source.sendFeedback({ standardText("Set enchantment $id to level $level") }, true)

	return 1
}

private fun configUnbreakableCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = BoolArgumentType.getBool(context, "value")

	game.toggleComponent<HandUnbreakableComponent>(value)

	context.source.sendFeedback({ standardText("Set unbreakable items to $value") }, true)

	return 1
}

private fun configKeepInventoryCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = BoolArgumentType.getBool(context, "value")

	game.toggleComponent<KeepInventoryComponent>(value)

	context.source.sendFeedback({ standardText("Set keepInventory to $value") }, true)

	return 1
}

private fun configBingoTaskEnchantmentCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = BoolArgumentType.getBool(context, "value")

	val component = game.getComponent<BingoComponent>() ?: return 0

	component.taskEnchantment = value

	context.source.sendFeedback({ standardText("Set task of enchantment to $value") }, true)

	return 1
}

private fun configBingoTaskPotionCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = BoolArgumentType.getBool(context, "value")

	val component = game.getComponent<BingoComponent>() ?: return 0

	component.taskPotion = value

	context.source.sendFeedback({ standardText("Set task of potion to $value") }, true)

	return 1
}

private fun configBingoTaskColoredCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = BoolArgumentType.getBool(context, "value")

	val component = game.getComponent<BingoComponent>() ?: return 0

	component.taskColored = value

	context.source.sendFeedback({ standardText("Set task of colored to $value") }, true)

	return 1
}

private fun configBingoTaskUseSetCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = StringArgumentType.getString(context, "value")

	val component = game.getComponent<BingoComponent>() ?: return 0

	component.useSet = value

	context.source.sendFeedback({ standardText("Using the bingo set $value") }, true)

	return 1
}

private fun configBingoTaskUseNoSetCommand(context: CommandContext<ServerCommandSource>): Int {
	val component = game.getComponent<BingoComponent>() ?: return 0

	component.useSet = null

	context.source.sendFeedback({ standardText("No longer using a bingo set") }, true)

	return 1
}

private fun configSirLifeCountCommand(context: CommandContext<ServerCommandSource>): Int {
	val component = game.getComponent<SirComponent>() ?: return 0

	val value = IntegerArgumentType.getInteger(context, "value")
	component.lifeCount = value

	context.source.sendFeedback({ standardText("Set life count to $value") }, true)

	return 1
}

private fun configSirItemTimerCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = StringArgumentType.getString(context, "value")

	val component = game.getComponent<SirComponent>() ?: return 0

	if (component.itemTimer.set(value)) {
		context.source.sendFeedback({ standardText("Set item timer to $value") }, true)
		return 1
	}

	context.source.sendFeedback({ standardText("Couldn't parse the time") }, false)
	return 0
}

private fun configSirRadiusCommand(context: CommandContext<ServerCommandSource>): Int {
	val component = game.getComponent<SirComponent>() ?: return 0

	val value = IntegerArgumentType.getInteger(context, "value")
	component.radius = value

	context.source.sendFeedback({ standardText("Set radius to $value") }, true)

	return 1
}

private fun configSirEnchantmentChanceCommand(context: CommandContext<ServerCommandSource>): Int {
	val component = game.getComponent<SirComponent>() ?: return 0

	val value = DoubleArgumentType.getDouble(context, "value")
	component.enchantmentChance = value

	context.source.sendFeedback({ standardText("Set enchantment chance to $value") }, true)

	return 1
}

private fun configSirAttributeModifierChanceCommand(context: CommandContext<ServerCommandSource>): Int {
	val component = game.getComponent<SirComponent>() ?: return 0

	val value = DoubleArgumentType.getDouble(context, "value")
	component.attributeModifierChance = value

	context.source.sendFeedback({ standardText("Set attribute modifier chance to $value") }, true)

	return 1
}

private fun testCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = StringArgumentType.getString(context, "value")

	island(value, Vec3i(context.source.position))

	return 1
}