package xyz.mantevian.mgames

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.Registries
import net.minecraft.server.permissions.Permissions
import xyz.mantevian.mgames.bingo.createBingoGame
import xyz.mantevian.mgames.game.*
import xyz.mantevian.mgames.sir.createSirGame
import xyz.mantevian.mgames.util.island
import xyz.mantevian.mgames.util.standardText

fun registerCommands(
    dispatcher: CommandDispatcher<CommandSourceStack>,
    buildContext: CommandBuildContext,
    commandSelection: Commands.CommandSelection
) {
    val root = Commands.literal("mg").requires { req -> req.permissions().hasPermission(Permissions.COMMANDS_ADMIN) }

    val test = Commands.literal("test").then(
        Commands.argument("value", StringArgumentType.greedyString())
            .executes(::testCommand)
    )

    val initBingo = Commands.literal("bingo").executes(::initBingoCommand)
    val initSir = Commands.literal("sir").executes(::initSirCommand)

    val init = Commands.literal("init")
        .then(initBingo)
        .then(initSir)

    val start = Commands.literal("start").executes(::startCommand)

    val configGameTime = Commands.literal("game_time")
        .then(
            Commands.argument("value", StringArgumentType.greedyString())
                .executes(::configGameTimeCommand)
        )

    val configWorldSize = Commands.literal("world_size")
        .then(
            Commands.argument("value", IntegerArgumentType.integer(0, 100000))
                .executes(::configWorldSizeCommand)
        )

    val configEnchantment = Commands.literal("enchantment")
        .then(
            Commands.argument(
                "id",
                ResourceArgument.resource(buildContext, Registries.ENCHANTMENT)
            )
                .then(
                    Commands.argument("level", IntegerArgumentType.integer(1, 127))
                        .executes(::configEnchantmentCommand)
                )
        )

    val configMiningEfficiency = Commands.literal("mining_efficiency")
        .then(
            Commands.argument("value", DoubleArgumentType.doubleArg(-1024.0, 1024.0))
                .executes(::configMiningEfficiencyCommand)
        )

    val configUnbreakable = Commands.literal("unbreakable")
        .then(
            Commands.argument("value", BoolArgumentType.bool())
                .executes(::configUnbreakableCommand)
        )

    val configKeepInventory = Commands.literal("keep_inventory")
        .then(
            Commands.argument("value", BoolArgumentType.bool())
                .executes(::configKeepInventoryCommand)
        )

    val configBingoTaskUseSet = Commands.literal("use_set")
        .executes(::configBingoTaskUseNoSetCommand)
        .then(
            Commands.argument("value", StringArgumentType.greedyString())
                .executes(::configBingoTaskUseSetCommand)
        )

    val configBingoTaskEnchantment = Commands.literal("task_enchantment")
        .then(
            Commands.argument("value", BoolArgumentType.bool())
                .executes(::configBingoTaskEnchantmentCommand)
        )

    val configBingoTaskPotion = Commands.literal("task_potion")
        .then(
            Commands.argument("value", BoolArgumentType.bool())
                .executes(::configBingoTaskPotionCommand)
        )

    val configBingoTaskColored = Commands.literal("task_colored")
        .then(
            Commands.argument("value", BoolArgumentType.bool())
                .executes(::configBingoTaskColoredCommand)
        )

    val configSirLifeCount = Commands.literal("life_count")
        .then(
            Commands.argument("value", IntegerArgumentType.integer(1, 100))
                .executes(::configSirLifeCountCommand)
        )

    val configSirItemTimer = Commands.literal("item_timer")
        .then(
            Commands.argument("value", StringArgumentType.greedyString())
                .executes(::configSirItemTimerCommand)
        )

    val configSirRadius = Commands.literal("radius")
        .then(
            Commands.argument("value", IntegerArgumentType.integer(10, 200))
                .executes(::configSirRadiusCommand)
        )

    val configSirEnchantmentChance = Commands.literal("enchantment_chance")
        .then(
            Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 0.9))
                .executes(::configSirEnchantmentChanceCommand)
        )

    val configSirAttributeModifierChance = Commands.literal("attribute_modifier_chance")
        .then(
            Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 0.9))
                .executes(::configSirAttributeModifierChanceCommand)
        )

    val configBingo = Commands.literal("bingo")
        .then(configBingoTaskEnchantment)
        .then(configBingoTaskPotion)
        .then(configBingoTaskColored)
        .then(configBingoTaskUseSet)

    val configSir = Commands.literal("sir")
        .then(configSirLifeCount)
        .then(configSirItemTimer)
        .then(configSirRadius)
        .then(configSirEnchantmentChance)
        .then(configSirAttributeModifierChance)

    val config = Commands.literal("config")
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

private fun initBingoCommand(context: CommandContext<CommandSourceStack>): Int {
    game = createBingoGame()
    game.init()

    context.source.sendSuccess({ standardText("Initializing Bingo") }, true)

    return 1
}

private fun initSirCommand(context: CommandContext<CommandSourceStack>): Int {
    game = createSirGame()
    game.init()

    context.source.sendSuccess({ standardText("Initializing Skyblock Item Randomizer") }, true)

    return 1
}

private fun startCommand(context: CommandContext<CommandSourceStack>): Int {
    context.source.sendSuccess({ standardText("Starting the game") }, true)

    startGame()

    return 1
}

private fun configGameTimeCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = StringArgumentType.getString(context, "value")

    val component = game.getComponentOrSetDefault<GameTimeComponent>()

    if (component.value.set(value)) {
        context.source.sendSuccess({ standardText("Set game time to $value") }, true)
        return 1
    }

    context.source.sendSuccess({ standardText("Couldn't parse the time") }, false)
    return 0
}

private fun configWorldSizeCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = IntegerArgumentType.getInteger(context, "value")

    val component = game.getComponentOrSetDefault<WorldSizeComponent>()

    component.update(value)

    context.source.sendSuccess({ standardText("Set world size to $value") }, true)

    return 1
}

private fun configMiningEfficiencyCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = DoubleArgumentType.getDouble(context, "value")

    val component = game.getComponentOrSetDefault<PlayersMiningEfficiencyComponent>()

    component.value = value

    context.source.sendSuccess({ standardText("Set players' mining efficiency to $value") }, true)

    return 1
}

private fun configEnchantmentCommand(context: CommandContext<CommandSourceStack>): Int {
    val id = ResourceArgument.getEnchantment(context, "id")
    val level = IntegerArgumentType.getInteger(context, "level")

    val component = game.getComponentOrSetDefault<HandEnchantmentsComponent>()

    component.enchantments[id.key().identifier().toString()] = level

    context.source.sendSuccess({ standardText("Set enchantment $id to level $level") }, true)

    return 1
}

private fun configUnbreakableCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = BoolArgumentType.getBool(context, "value")

    game.toggleComponent<HandUnbreakableComponent>(value)

    context.source.sendSuccess({ standardText("Set unbreakable items to $value") }, true)

    return 1
}

private fun configKeepInventoryCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = BoolArgumentType.getBool(context, "value")

    game.toggleComponent<KeepInventoryComponent>(value)

    context.source.sendSuccess({ standardText("Set keepInventory to $value") }, true)

    return 1
}

private fun configBingoTaskEnchantmentCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = BoolArgumentType.getBool(context, "value")

    val component = game.getComponent<BingoComponent>() ?: return 0

    component.taskEnchantment = value

    context.source.sendSuccess({ standardText("Set task of enchantment to $value") }, true)

    return 1
}

private fun configBingoTaskPotionCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = BoolArgumentType.getBool(context, "value")

    val component = game.getComponent<BingoComponent>() ?: return 0

    component.taskPotion = value

    context.source.sendSuccess({ standardText("Set task of potion to $value") }, true)

    return 1
}

private fun configBingoTaskColoredCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = BoolArgumentType.getBool(context, "value")

    val component = game.getComponent<BingoComponent>() ?: return 0

    component.taskColored = value

    context.source.sendSuccess({ standardText("Set task of colored to $value") }, true)

    return 1
}

private fun configBingoTaskUseSetCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = StringArgumentType.getString(context, "value")

    val component = game.getComponent<BingoComponent>() ?: return 0

    component.useSet = value

    context.source.sendSuccess({ standardText("Using the bingo set $value") }, true)

    return 1
}

private fun configBingoTaskUseNoSetCommand(context: CommandContext<CommandSourceStack>): Int {
    val component = game.getComponent<BingoComponent>() ?: return 0

    component.useSet = null

    context.source.sendSuccess({ standardText("No longer using a bingo set") }, true)

    return 1
}

private fun configSirLifeCountCommand(context: CommandContext<CommandSourceStack>): Int {
    val component = game.getComponent<SirComponent>() ?: return 0

    val value = IntegerArgumentType.getInteger(context, "value")
    component.lifeCount = value

    context.source.sendSuccess({ standardText("Set life count to $value") }, true)

    return 1
}

private fun configSirItemTimerCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = StringArgumentType.getString(context, "value")

    val component = game.getComponent<SirComponent>() ?: return 0

    if (component.itemTimer.set(value)) {
        context.source.sendSuccess({ standardText("Set item timer to $value") }, true)
        return 1
    }

    context.source.sendSuccess({ standardText("Couldn't parse the time") }, false)
    return 0
}

private fun configSirRadiusCommand(context: CommandContext<CommandSourceStack>): Int {
    val component = game.getComponent<SirComponent>() ?: return 0

    val value = IntegerArgumentType.getInteger(context, "value")
    component.radius = value

    context.source.sendSuccess({ standardText("Set radius to $value") }, true)

    return 1
}

private fun configSirEnchantmentChanceCommand(context: CommandContext<CommandSourceStack>): Int {
    val component = game.getComponent<SirComponent>() ?: return 0

    val value = DoubleArgumentType.getDouble(context, "value")
    component.enchantmentChance = value

    context.source.sendSuccess({ standardText("Set enchantment chance to $value") }, true)

    return 1
}

private fun configSirAttributeModifierChanceCommand(context: CommandContext<CommandSourceStack>): Int {
    val component = game.getComponent<SirComponent>() ?: return 0

    val value = DoubleArgumentType.getDouble(context, "value")
    component.attributeModifierChance = value

    context.source.sendSuccess({ standardText("Set attribute modifier chance to $value") }, true)

    return 1
}

private fun testCommand(context: CommandContext<CommandSourceStack>): Int {
    val value = StringArgumentType.getString(context, "value")

    island(
        value, Vec3i(
            context.source.position.x.toInt(),
            context.source.position.y.toInt(),
            context.source.position.z.toInt()
        )
    )

    return 1
}