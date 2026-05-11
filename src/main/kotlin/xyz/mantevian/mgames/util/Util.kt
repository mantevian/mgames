package xyz.mantevian.mgames.util

import net.minecraft.ChatFormatting
import net.minecraft.core.*
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.portal.TeleportTransition
import net.minecraft.world.level.storage.LevelData
import net.minecraft.world.phys.Vec3
import xyz.mantevian.mgames.game
import xyz.mantevian.mgames.game.SpawnBoxComponent
import xyz.mantevian.mgames.server
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

val random = Random

infix fun ItemStack.isId(id: String): Boolean {
    return getRegistry(Registries.ITEM)
        .getKey(this.item).toString() == id
}

infix fun ItemStack.isId(ids: List<String>): Boolean {
    return ids.contains(
        getRegistry(Registries.ITEM)
            .getKey(this.item).toString()
    )
}

fun <T : Any> getRegistry(key: ResourceKey<Registry<T>>): Registry<T> {
    return server.registryAccess().lookup(key).get()
}

fun standardText(text: String): MutableComponent {
    return Component
        .literal(text)
        .setStyle(Style.EMPTY)
        .withStyle { it.withItalic(false) }
        .withStyle(ChatFormatting.WHITE)
}

fun MutableComponent.resetStyle(): MutableComponent {
    return this.setStyle(Style.EMPTY)
        .withStyle { it.withItalic(false) }
        .withStyle(ChatFormatting.WHITE)
}

fun executeCommand(command: String) {
    server.commands.performPrefixedCommand(server.createCommandSourceStack(), command)
}

fun resetPlayersMinecraftStats() {
    forEachPlayer {
        it.removeAllEffects()
        it.setExperienceLevels(0)
        it.setExperiencePoints(0)
        it.inventory.clearContent()
        it.recipeBook.removeRecipes(server.recipeManager.recipes, it)
    }

    executeCommand("advancement revoke @a everything")
}

fun tpPlayers(pos: Vec3i) {
    forEachPlayer { player ->
        player.teleport(
            TeleportTransition(
                server.overworld(),
                Vec3(
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                ),
                Vec3.ZERO,
                player.yRot,
                player.xRot
            ) {}
        )

        server.overworld().respawnData = LevelData.RespawnData(
            GlobalPos(
                server.overworld().dimension(),
                BlockPos(0, -62, 0)
            ), 0.0f, 0.0f
        )
    }
}

fun infiniteEffectForEveryone(effect: Holder<MobEffect>, level: Int = 0) {
    forEachPlayer { player ->
        player.addEffect(MobEffectInstance(effect, -1, level, false, false))
    }
}

fun effectForEveryone(effect: Holder<MobEffect>, duration: Int = 0, level: Int = 0) {
    forEachPlayer { player ->
        player.addEffect(MobEffectInstance(effect, duration, level, false, false))
    }
}

fun announce(text: Component) {
    forEachPlayer {
        it.sendSystemMessage(text)
    }
}

fun announce(text: Component, soundEvent: SoundEvent, pitch: Float = 1.0f, volume: Float = 1.0f) {
    announce(text)
    playSoundToEveryone(soundEvent, volume, pitch)
}

fun announce(lines: Collection<Component>) {
    lines.forEach { announce(it) }
}

fun announceClick(text: Component) {
    announce(text, SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
}

fun title(text: String) {
    executeCommand("title @a title {\"text\":\"$text\"}")
}

fun itemById(id: String): Item? {
    return getRegistry(Registries.ITEM).getValue(Identifier.parse(id))
}

fun enchantmentById(id: String): Enchantment? {
    return getRegistry(Registries.ENCHANTMENT).getValue(Identifier.parse(id))
}

fun statusEffectById(id: String): MobEffect? {
    return getRegistry(Registries.MOB_EFFECT).getValue(Identifier.parse(id))
}

fun levelById(id: String): ServerLevel? {
    val key = server.levelKeys().find { it.toString() == id } ?: return null
    return server.getLevel(key)
}

fun calculateColorValue(colors: List<String>): Int {
    val stack =
        ItemStackBuilder(Items.WOLF_ARMOR).ofColorMix(colors).build()
    return DyedItemColor.getOrDefault(stack, 0)
}

fun nextInt(range: IntRange): Int {
    return random.nextInt(range.first, range.last + 1)
}

fun nextDouble(min: Double, max: Double): Double {
    return random.nextDouble() * (max - min) + min
}

fun nextBoolean(chance: Double): Boolean {
    return nextDouble(0.0, 1.0) < chance
}

fun highestY(level: Level, pos: BlockPos): Int {
    return level.getChunk(pos)
        .heightmaps
        .find { (k, v) -> k == Heightmap.Types.MOTION_BLOCKING }
        ?.value
        ?.getFirstAvailable(
            pos.x % 16,
            pos.z % 16
        ) ?: 0
}

fun pointsInCircle(center: Vec3i, radius: Int, count: Int): List<Vec3i> {
    val result: MutableList<Vec3i> = mutableListOf()

    for (i in 0..<count) {
        val angle = i.toDouble() / count.toDouble()
        val x = cos(angle * 2 * PI) * radius
        val z = sin(angle * 2 * PI) * radius

        result.add(Vec3i(center.x + x.roundToInt(), center.y, center.z + z.roundToInt()))
    }

    return result
}

fun teleportInCircle(players: List<ServerPlayer>, radius: Int) {
    val points = pointsInCircle(Vec3i(0, 0, 0), radius, players.size)

    for (i in players.indices) {
        val player = players[i]

        player.teleport(
            TeleportTransition(
                server.overworld(),
                Vec3(
                    points[i].x.toDouble(),
                    server.overworld().logicalHeight.toDouble(),
                    points[i].z.toDouble()
                ),
                Vec3.ZERO,
                player.yRot,
                player.xRot
            ) {}
        )
    }
}

fun teleportToWorldSpawn(player: ServerPlayer) {
    val component = game.getComponent<SpawnBoxComponent>() ?: return
    val level = levelById(component.worldId) ?: return
    val y = highestY(level, BlockPos(component.pos))

    val pos = BlockPos(component.pos.x, y, component.pos.z)
    val down = BlockPos(component.pos.x, y - 1, component.pos.z)
    val up = BlockPos(component.pos.x, y + 1, component.pos.z)

    level.setBlock(down, Blocks.BEDROCK.defaultBlockState(), 0)
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 0)
    level.setBlock(up, Blocks.AIR.defaultBlockState(), 0)

    teleport(player, level, pos)
}

fun teleportToOwnSpawn(player: ServerPlayer) {
    val respawnData = player.respawnConfig?.respawnData ?: server.overworld().respawnData
    val pos = respawnData.pos()
    val dimension = respawnData.dimension()

    player.teleport(
        TeleportTransition(
            server.getLevel(dimension)!!,
            Vec3(pos.x.toDouble() + 0.5, pos.y.toDouble(), pos.z.toDouble() + 0.5),
            Vec3.ZERO,
            player.yRot,
            player.xRot
        ) {}
    )
}

fun randomTeleport(player: ServerPlayer, radius: Int, precision: Int) {
    val angle = random.nextFloat() * PI * 2
    val x = cos(angle) * radius
    val z = sin(angle) * radius
    executeCommand("spreadplayers ${x.roundToInt()} ${z.roundToInt()} 0 $precision true ${player.scoreboardName}")
}

fun getAllPlayers(): MutableList<ServerPlayer> {
    return server.playerList.players
}

fun forEachPlayer(fn: ((player: ServerPlayer) -> Unit)) {
    getAllPlayers().forEach(fn)
}

fun playSoundToEveryone(soundEvent: SoundEvent, volume: Float = 1.0f, pitch: Float = 1.0f) {
    playSound(getAllPlayers(), soundEvent, volume, pitch)
}

fun playSound(
    players: List<ServerPlayer>,
    soundEvent: SoundEvent,
    volume: Float = 1.0f,
    pitch: Float = 1.0f
) {
    players.forEach {
        playSound(it, soundEvent, volume, pitch)
    }
}

fun playSound(player: ServerPlayer, soundEvent: SoundEvent, volume: Float = 1.0f, pitch: Float = 1.0f) {
    player.level().playSound(null, player, soundEvent, player.soundSource, volume, pitch)
}

fun teleport(player: ServerPlayer, level: ServerLevel, pos: BlockPos) {
    player.teleport(
        TeleportTransition(
            level,
            Vec3(pos.x.toDouble() + 0.5, pos.y.toDouble(), pos.z.toDouble() + 0.5),
            Vec3.ZERO,
            player.yRot,
            player.xRot
        ) {}
    )
}

fun setSpawnPoint(player: ServerPlayer) {
    player.setRespawnPosition(
        ServerPlayer.RespawnConfig(
            LevelData.RespawnData(
                GlobalPos(
                    player.level().dimension(),
                    player.blockPosition()
                ),
                0f,
                0f
            ),
            true
        ),
        true
    )
}