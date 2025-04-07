package xyz.mantevian.mgames

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.block.Blocks
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.ServerCommandSource
import xyz.mantevian.mgames.bingo.createBingoGame
import xyz.mantevian.mgames.game.*
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

	val init = CommandManager.literal("init")
		.then(initBingo)

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
		.then(configBingoEnchantment)
		.then(configBingoUnbreakable)
		.then(configBingoTaskEnchantment)
		.then(configBingoTaskPotion)
		.then(configBingoTaskUseSet)

	val config = CommandManager.literal("config")
		.then(configBingo)
		.then(configGameTime)
		.then(configWorldSize)

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

	context.source.sendFeedback({ standardText("Initializing the game Bingo") }, true)

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

private fun configBingoEnchantmentCommand(context: CommandContext<ServerCommandSource>): Int {
	val id = RegistryEntryReferenceArgumentType.getEnchantment(context, "id")
	val level = IntegerArgumentType.getInteger(context, "level")

	val component = game.getComponentOrSetDefault<HandEnchantmentsComponent>()

	component.enchantments[id.idAsString] = level

	context.source.sendFeedback({ standardText("Set enchantment $id to level $level") }, true)

	return 1
}

private fun configBingoUnbreakableCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = BoolArgumentType.getBool(context, "value")

	game.toggleComponent<HandUnbreakableComponent>(value)

	context.source.sendFeedback({ standardText("Set unbreakable items to $value") }, true)

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

private fun testCommand(context: CommandContext<ServerCommandSource>): Int {
	val value = StringArgumentType.getString(context, "value")

	when (value) {
		"oak" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_default",
			treeFeature = "oak"
		)

		"birch" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_default",
			treeFeature = "birch_tall"
		)

		"spruce" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "patch_brown_mushroom",
			treeFeature = "trees_taiga"
		)

		"acacia" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_plain",
			treeFeature = "acacia"
		)

		"jungle" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass_jungle",
			flowerFeature = "flower_default",
			treeFeature = "jungle_tree"
		)

		"cherry" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_cherry",
			treeFeature = "cherry"
		)

		"pale" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.PALE_MOSS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "pale_moss_patch",
			flowerFeature = "flower_pale_garden",
			treeFeature = "pale_oak"
		)

		"lush" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_flower_forest",
			treeFeature = "azalea_tree"
		)

		"crimson" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.CRIMSON_NYLIUM,
			mainBlock = Blocks.NETHERRACK,
			stoneBlock = Blocks.BLACKSTONE,
			grassFeature = "crimson_forest_vegetation",
			flowerFeature = "crimson_forest_vegetation",
			treeFeature = "crimson_fungus"
		)

		"warped" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.WARPED_NYLIUM,
			mainBlock = Blocks.NETHERRACK,
			stoneBlock = Blocks.BLACKSTONE,
			grassFeature = "warped_forest_vegetation",
			flowerFeature = "warped_forest_vegetation",
			treeFeature = "warped_fungus"
		)

		"soul_sand" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.SOUL_SOIL,
			mainBlock = Blocks.SOUL_SAND,
			stoneBlock = Blocks.NETHERRACK,
			grassFeature = "patch_soul_fire",
			flowerFeature = "",
			treeFeature = "fossil_diamonds"
		)

		"desert" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.SAND,
			mainBlock = Blocks.SANDSTONE,
			stoneBlock = Blocks.STONE,
			addSupportingBlocks = true,
			grassFeature = "patch_dead_bush",
			flowerFeature = "patch_cactus",
			treeFeature = ""
		)

		"badlands" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.RED_SAND,
			mainBlock = Blocks.TERRACOTTA,
			stoneBlock = Blocks.STONE,
			addSupportingBlocks = true,
			grassFeature = "patch_dead_bush",
			flowerFeature = "patch_cactus",
			treeFeature = ""
		)

		"snow" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.SNOW_BLOCK,
			mainBlock = Blocks.PACKED_ICE,
			stoneBlock = Blocks.STONE,
			grassFeature = "pile_snow",
			flowerFeature = "patch_pumpkin",
			treeFeature = "trees_taiga"
		)

		"ice" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 30,
			surfaceBlock = Blocks.BLUE_ICE,
			mainBlock = Blocks.PACKED_ICE,
			stoneBlock = Blocks.STONE,
			grassFeature = "",
			flowerFeature = "blue_ice",
			treeFeature = "ice_spike"
		)

		"mushroom" -> island(
			center = Vec3i(context.source.position),
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.MYCELIUM,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_brown_mushroom",
			flowerFeature = "patch_red_mushroom",
			treeFeature = "mushroom_island_vegetation"
		)
	}

	return 1
}