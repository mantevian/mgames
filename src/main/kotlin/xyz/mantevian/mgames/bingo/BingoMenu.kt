package xyz.mantevian.mgames.bingo

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import eu.pb4.sgui.api.gui.SimpleGui
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items
import xyz.mantevian.mgames.BingoTypedTaskData
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.game
import xyz.mantevian.mgames.game.BingoComponent
import xyz.mantevian.mgames.game.WorldSizeComponent
import xyz.mantevian.mgames.resourceManager
import xyz.mantevian.mgames.util.*

class BingoMenu(player: ServerPlayer) : SimpleGui(MenuType.GENERIC_9x5, player, false) {
    private val bingo = game.getComponent<BingoComponent>()!!

    override fun getTitle(): Component {
        return Component.literal(resourceManager.get<List<String>>("bingo/splashes.json")!!.shuffled()[0])
    }

    override fun onOpen() {
        super.onOpen()

        val playerData = bingo.players[player.stringUUID] ?: return

        for (i in 0..44) {
            setSlot(i, ItemStackBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip().build())
        }

        for (i in 0..24) {
            val task = bingo.tasks[i]?.data
            val reward = bingo.tasks[i]?.reward

            val x = i % 5
            val y = i / 5

            val builder = when (task) {
                is BingoTypedTaskData.Item -> {
                    val name = Component.translatable(itemById(task.id)!!.descriptionId).resetStyle()

                    val builder = ItemStackBuilder(task.id)
                        .withCount(task.count)
                        .setCustomName(name)

                    if (task.count > 1) {
                        builder.setCustomName(name.append(standardText(" (${task.count})")))
                    }

                    builder
                }

                is BingoTypedTaskData.Enchantment -> {
                    ItemStackBuilder(Items.ENCHANTED_BOOK)
                        .setCustomName(Component.translatable("enchantment.${task.id.replace(":", ".")}").resetStyle())
                }

                is BingoTypedTaskData.Potion -> {
                    val effect = statusEffectById(task.id) ?: MobEffects.WITHER.value()
                    ItemStackBuilder(Items.POTION)
                        .setPotionColor(effect)
                        .setCustomName(Component.translatable("effect.${task.id.replace(":", ".")}").resetStyle())
                }

                is BingoTypedTaskData.ColoredItem -> {
                    val builder = ItemStackBuilder(task.id)
                        .setCustomName(Component.translatable(itemById(task.id)!!.descriptionId).resetStyle())
                        .ofColorMix(task.colorNames)

                    task.colorNames.forEach { name ->
                        val item = getRegistry(Registries.ITEM)
                            .getValue(Identifier.parse("minecraft:${name}_dye")) ?: return@forEach

                        builder.addLore(
                            Component.translatable(item.descriptionId).withStyle(
                                Style.EMPTY
                                    .withItalic(false)
                                    .withColor(ChatFormatting.GRAY)
                            )
                        )
                    }

                    builder
                }

                else -> ItemStackBuilder(Items.BEDROCK)
            }

            if (reward != null) {
                builder.addLore(standardText(""))
                builder.addLore(standardText("Reward: $reward ★").withStyle(ChatFormatting.YELLOW))
            }

            if (playerData.tasks[i] != null) {
                builder.setCooldown(
                    Identifier.parse(
                        "mantevian:$MOD_ID/bingo/item_${i}"
                    ), 2000
                )

                builder.addLore(standardText(""))
                builder.addLore(standardText("COMPLETED").withStyle(ChatFormatting.GREEN))
            }

            builder.hideAdditionalTooltip()

            setSlot(y * 9 + x + 2, builder.build())
        }

        // 9, 27

        val requiredPoints = (playerData.usedRTP + 1) * 15
        val canUseRTP = bingo.countPoints(player) >= requiredPoints

        val rtpBuilder = ItemStackBuilder(Items.ENDER_PEARL)
            .setCustomName(standardText("Random teleport"))

        if (canUseRTP) {
            rtpBuilder.addLore(standardText("Click to use").withStyle(ChatFormatting.AQUA))
            rtpBuilder.addLore(standardText(""))
            rtpBuilder.addLore(standardText("Next use activates at ${requiredPoints + 15} ★").withStyle(ChatFormatting.YELLOW))
        } else {
            rtpBuilder.addLore(standardText("Requires $requiredPoints ★ to use").withStyle(ChatFormatting.RED))
        }

        setSlot(9, rtpBuilder.build())

        setSlot(
            18, ItemStackBuilder(Items.RED_BED)
                .setCustomName(standardText("Teleport to your spawn"))
                .addLore(standardText("Click to use").withStyle(ChatFormatting.AQUA))
                .build()
        )

        setSlot(
            27, ItemStackBuilder(Items.NETHER_STAR)
                .setCustomName(standardText("Teleport to world spawn"))
                .addLore(standardText("Click to use").withStyle(ChatFormatting.AQUA))
                .build()
        )
    }

    override fun onClick(index: Int, type: ClickType, action: ContainerInput, element: GuiElement): Boolean {
        val playerData = bingo.players[player.stringUUID] ?: return false

        when (index) {
            9 -> {
                val requiredPoints = (playerData.usedRTP + 1) * 15
                val canUseRTP = bingo.countPoints(player) >= requiredPoints

                val worldSize = game.getComponentOrDefault<WorldSizeComponent>().value

                if (canUseRTP) {
                    close()
                    randomTeleport(player, worldSize * 2 / 3, worldSize / 8)
                    playerData.usedRTP++
                }
            }

            18 -> {
                close()
                teleportToOwnSpawn(player)
            }

            27 -> {
                close()
                teleportToWorldSpawn(player)
            }
        }

        return super.onClick(index, type, action, element)
    }
}