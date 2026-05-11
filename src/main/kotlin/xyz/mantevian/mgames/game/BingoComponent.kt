package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.level.GameType
import net.minecraft.world.level.gamerules.GameRules
import xyz.mantevian.mgames.*
import xyz.mantevian.mgames.bingo.BingoGenerator
import xyz.mantevian.mgames.util.*

@Serializable
@SerialName("bingo")
class BingoComponent : GameComponent {
    @SerialName("use_set")
    var useSet: String? = null

    @SerialName("task_enchantment")
    var taskEnchantment: Boolean = true

    @SerialName("task_potion")
    var taskPotion: Boolean = true

    @SerialName("task_colored")
    var taskColored: Boolean = true

    @SerialName("tasks")
    val tasks: MutableMap<Int, BingoTaskData> = mutableMapOf()

    @SerialName("players")
    val players: MutableMap<String, BingoPlayer> = mutableMapOf()

    override fun init() {
        tasks.clear()
        players.clear()

        deleteObjective("bingo.score")
        createObjective("bingo.score", standardText("★ Points ★"))
    }

    override fun canStart(): Boolean {
        return setupData()
    }

    override fun start() {
        forEachPlayer {
            val playerData = BingoPlayer()
            playerData.tasks.clear()
            players[it.stringUUID] = playerData
            it.inventory.addAndPickItem(ItemStackBuilder(MGItems.BINGO_MENU_ITEM).build())
            it.setGameMode(GameType.SURVIVAL)
        }

        effectForEveryone(MobEffects.SLOWNESS, 20 * 10, 5)
        effectForEveryone(MobEffects.MINING_FATIGUE, 20 * 10, 5)
        effectForEveryone(MobEffects.WEAKNESS, 20 * 10, 5)
        effectForEveryone(MobEffects.RESISTANCE, 20 * 10, 2)

        effectForEveryone(MobEffects.FIRE_RESISTANCE, 20 * 60 * 5, 0)
        effectForEveryone(MobEffects.RESISTANCE, 20 * 60 * 5, 0)

        server.gameRules.set(GameRules.FALL_DAMAGE, false, server)

        teleportInCircle(getAllPlayers(), 500)
    }

    override fun tick() {
        when (game.state) {
            GameState.PLAYING -> {
                infiniteEffectForEveryone(MobEffects.SATURATION)

                forEachPlayer { player ->
                    val uuid = player.stringUUID

                    tasks.forEach {
                        val completed = checkTask(player, it.value.data)
                        val alreadyMarkedCompleted = players[uuid]?.tasks?.get(it.key) != null

                        if (completed && !alreadyMarkedCompleted) {
                            setCompletedTask(player, it.key)
                        }

                        player.cooldowns.addCooldown(
                            Identifier.parse(
                                "mantevian:$MOD_ID/bingo/item_${it.key}"
                            ), if (alreadyMarkedCompleted) 1000000 else 0
                        )
                    }

                    setScore(player.scoreboardName, "bingo.score", countPoints(player))
                }

                setSidebar(standardText("Bingo"), leaderboard(false))

                val gameTime = game.getComponentOrDefault<GameTimeComponent>().value.getFullSeconds()
                if (game.time.getTicks() % 20 == 0) {
                    when (game.time.getFullSeconds()) {
                        -10 -> {
                            announceClick(standardText("Bingo starts in 10 seconds!"))
                            server.gameRules.set(GameRules.PVP, false, server)
                        }

                        0 -> {
                            announce(standardText("Bingo has started!"))
                            title("Bingo has started!")

                            forEachPlayer {
                                setSpawnPoint(it)
                            }
                        }

                        30 -> {
                            executeCommand("gamerule fallDamage true")
                        }

                        gameTime - 900 -> {
                            announceClick(standardText("Bingo ends in 15 minutes!"))
                        }

                        gameTime - 300 -> {
                            announceClick(standardText("Bingo ends in 5 minutes!"))
                        }

                        gameTime - 30 -> {
                            announceClick(standardText("Bingo ends in 30 seconds!"))
                        }

                        gameTime - 5 -> {
                            announceClick(standardText("Bingo ends in 5 seconds!"))
                        }

                        gameTime - 4 -> {
                            announceClick(standardText("Bingo ends in 4 seconds!"))
                        }

                        gameTime - 3 -> {
                            announceClick(standardText("Bingo ends in 3 seconds!"))
                        }

                        gameTime - 2 -> {
                            announceClick(standardText("Bingo ends in 2 seconds!"))
                        }

                        gameTime - 1 -> {
                            announceClick(standardText("Bingo ends in 1 seconds!"))
                        }

                        gameTime -> {
                            announceClick(standardText("Bingo has ended!"))
                            game.finish()
                        }
                    }
                }
            }

            GameState.WAITING -> {
                forEachPlayer { player ->
                    setScore(player.scoreboardName, "bingo.score", 0)
                }
            }

            else -> {}
        }
    }

    override fun finish() {
        title("Bingo has ended!")

        announce(standardText("Leaderboard for this game:").withStyle(ChatFormatting.AQUA))
        announce(leaderboard(true).values)
        playSoundToEveryone(SoundEvents.PLAYER_LEVELUP)
    }

    private fun leaderboard(showTime: Boolean, asPlayer: ServerPlayer? = null): Map<Int, Component> {
        val sortedPlayers = getAllPlayers().sortedByDescending { getScore(it) }

        val result: MutableMap<Int, Component> = mutableMapOf()

        sortedPlayers.forEachIndexed { i, player ->
            val text = standardText("").apply {
                append(standardText("${i + 1}. ").withStyle(ChatFormatting.GRAY))
                append(standardText(player.scoreboardName).withStyle(ChatFormatting.WHITE))
                append(standardText(" ${countPoints(player)} ★").withStyle(ChatFormatting.YELLOW))

                if (showTime) {
                    append(standardText(" [${getLastTime(player).formatHourMinSec()}]").withStyle(ChatFormatting.GRAY))
                }
            }

            if (asPlayer != null && player.stringUUID == asPlayer.stringUUID) {
                text.withStyle(ChatFormatting.BOLD)
            }

            result[i] = text
        }

        return result
    }

    private fun checkTask(player: ServerPlayer, task: BingoTypedTaskData): Boolean {
        return when (task) {
            is BingoTypedTaskData.Item -> {
                player.inventory.contains { stack ->
                    stack isId task.id && stack.count >= task.count
                }
            }

            is BingoTypedTaskData.Enchantment -> {
                player.inventory.contains { stack ->
                    stack.enchantments.entrySet().any { entry ->
                        val enchantment = entry.key.unwrapKey().get().identifier().toString()
                        enchantment == task.id
                    } || stack.get(DataComponents.STORED_ENCHANTMENTS)?.entrySet()?.any { entry2 ->
                        val enchantment = entry2.key.unwrapKey().get().identifier().toString()
                        enchantment == task.id
                    } ?: false
                }
            }

            is BingoTypedTaskData.Potion -> {
                player.inventory.contains { stack ->
                    val isItem = stack isId listOf(
                        "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion"
                    )

                    if (!isItem) return@contains false

                    val isPotion = stack.get(DataComponents.POTION_CONTENTS)?.potion?.get()?.value()?.effects?.any {
                        it.effect.unwrapKey().get().identifier().toString() == task.id
                    }

                    isPotion ?: false
                }
            }

            is BingoTypedTaskData.ColoredItem -> {
                player.inventory.contains { stack ->
                    stack isId task.id && DyedItemColor.getOrDefault(
                        stack,
                        0
                    ) == task.colorValue
                }
            }

            is BingoTypedTaskData.OneOfItems -> {
                false
            }

            is BingoTypedTaskData.None -> false
        }
    }

    private fun setCompletedTask(player: ServerPlayer, n: Int) {
        val task = tasks[n] ?: return
        val playerData = players[player.stringUUID] ?: return

        playerData.tasks[n] = game.time.clone()

        val taskTypedText = when (task.data) {
            is BingoTypedTaskData.Item -> standardText("").run {
                append(Component.translatable(itemById(task.data.id)!!.descriptionId))
                append(" (${task.data.count})")
            }

            is BingoTypedTaskData.Enchantment -> standardText("").run {
                append("Enchantment ")
                append(enchantmentById(task.data.id)!!.description)
            }

            is BingoTypedTaskData.Potion -> standardText("").run {
                append("Potion of ")
                statusEffectById(task.data.id)?.descriptionId?.let { append(Component.translatable(it)) }
            }

            is BingoTypedTaskData.ColoredItem -> standardText("").run {
                append(Component.translatable(itemById(task.data.id)?.descriptionId ?: ""))
            }

            else -> standardText("")
        } ?: standardText("")

        announce(
            standardText("").apply {
                append(standardText("[").withStyle(ChatFormatting.GRAY))
                append(standardText("+${task.reward} ★").withStyle(ChatFormatting.YELLOW))
                append(standardText("] ").withStyle(ChatFormatting.GRAY))
                append(standardText(player.scoreboardName).withStyle(ChatFormatting.GREEN))
                append(standardText(" has collected ").withStyle(ChatFormatting.GRAY))
                append(taskTypedText.withStyle(ChatFormatting.GREEN))
                append(standardText(" [" + game.time.formatHourMinSec() + "]").withStyle(ChatFormatting.GRAY))
            }
        )

        playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP)

        player.addItem(ItemStack(Items.ENDER_EYE))
    }

    fun countPoints(player: ServerPlayer): Int {
        var sum = 0
        players[player.stringUUID]?.tasks?.forEach { (i, d) ->
            sum += tasks[i]?.reward ?: 0
        }
        return sum
    }

    fun getLastTime(player: ServerPlayer): MGDuration {
        val playerData = players[player.stringUUID] ?: return MGDuration.zero()
        if (playerData.tasks.isEmpty()) {
            return MGDuration.zero()
        }

        return playerData.tasks.values.sortedByDescending { dur -> dur.getTicks() }[0]
    }

    fun getScore(player: ServerPlayer): Int {
        val gameTime = game.getComponentOrDefault<GameTimeComponent>().value.getTicks()
        return (countPoints(player) + 1) * gameTime - getLastTime(player).getTicks()
    }

    fun maxPoints(): Int {
        if (tasks.isEmpty()) {
            return 0
        }
        return tasks.map { it.value.reward }.reduce { a, b -> a + b }
    }

    private fun setupData(): Boolean {
        tasks.clear()

        val data = resourceManager.get<Map<Int, BingoTaskData>>("bingo/set/${useSet}.json")
            ?: return BingoGenerator().generateTasks()

        tasks.putAll(data)

        return true
    }
}