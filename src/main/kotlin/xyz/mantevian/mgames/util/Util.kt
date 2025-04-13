package xyz.mantevian.mgames.util

import net.minecraft.block.Blocks
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.PositionFlag
import net.minecraft.potion.Potion
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.World
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
	return Registries.ITEM.getId(this.item).toString() == id
}

infix fun ItemStack.isId(ids: List<String>): Boolean {
	return ids.contains(Registries.ITEM.getId(this.item).toString())
}

fun standardText(text: String): MutableText {
	return Text.literal(text).styled { it.withItalic(false).withColor(Formatting.WHITE) }
}

fun MutableText.resetStyle(): MutableText {
	return this.styled { it.withItalic(false).withColor(Formatting.WHITE) }
}

fun executeCommand(command: String) {
	server.commandManager.executeWithPrefix(server.commandSource, command)
}

fun resetPlayersMinecraftStats() {
	forEachPlayer {
		it.clearStatusEffects()
		it.setExperienceLevel(0)
		it.setExperiencePoints(0)
		it.inventory.clear()
		it.recipeBook.lockRecipes(server.recipeManager.values(), it)
	}

	executeCommand("advancement revoke @a everything")
}

fun tpPlayers(pos: Vec3i) {
	forEachPlayer { player ->
		player.teleport(
			server.overworld,
			pos.x.toDouble(),
			pos.y.toDouble(),
			pos.z.toDouble(),
			setOf(),
			0.0f,
			0.0f,
			false
		)

		server.overworld.setSpawnPos(BlockPos(0, -62, 0), 0.0f)
	}
}

fun infiniteEffectForEveryone(effect: RegistryEntry<StatusEffect>, level: Int = 0) {
	forEachPlayer { player ->
		player.addStatusEffect(StatusEffectInstance(effect, -1, level, false, false))
	}
}

fun effectForEveryone(effect: RegistryEntry<StatusEffect>, duration: Int = 0, level: Int = 0) {
	forEachPlayer { player ->
		player.addStatusEffect(StatusEffectInstance(effect, duration, level, false, false))
	}
}

fun announce(text: Text) {
	server.playerManager.broadcast(text, false)
}

fun announce(text: Text, soundEvent: SoundEvent, pitch: Float = 1.0f, volume: Float = 1.0f) {
	server.playerManager.broadcast(text, false)
	playSoundToEveryone(soundEvent, volume, pitch)
}

fun announce(lines: List<Text>) {
	lines.forEach { announce(it) }
}

fun announceClick(text: Text) {
	announce(text, SoundEvents.UI_BUTTON_CLICK.value(), 2.0f, 1.0f)
}

fun title(text: String) {
	executeCommand("title @a title {\"text\":\"$text\"}")
}

fun itemById(id: String): Item {
	return Registries.ITEM.get(Identifier.of(id))
}

fun enchantmentById(id: String): Enchantment? {
	return server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).get(Identifier.of(id))
}

fun getEnchantmentEntry(enchantment: Enchantment): RegistryEntry<Enchantment> {
	return server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(enchantment)
}

fun getAttributeEntry(attribute: EntityAttribute): RegistryEntry<EntityAttribute> {
	return server.registryManager.getOrThrow(RegistryKeys.ATTRIBUTE).getEntry(attribute)
}

fun potionById(id: String): Potion? {
	return Registries.POTION.get(Identifier.of(id))
}

fun statusEffectById(id: String): StatusEffect? {
	return Registries.STATUS_EFFECT.get(Identifier.of(id))
}

fun worldById(id: String): World? {
	val key = server.worldRegistryKeys.find { it.value.path == id } ?: return server.overworld
	return server.getWorld(key)
}

fun calculateColorValue(colors: List<String>): Int {
	val stack =
		ItemStackBuilder(Items.WOLF_ARMOR).ofColorMix(colors).build()
	return DyedColorComponent.getColor(stack, 0)
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

fun highestY(world: World, pos: BlockPos): Int {
	return world.getChunk(pos).getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(pos.x % 16, pos.z % 16)
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

fun teleportInCircle(players: List<ServerPlayerEntity>, radius: Int) {
	val points = pointsInCircle(Vec3i(0, 0, 0), radius, players.size)

	for (i in players.indices) {
		val player = players[i]

		player.teleport(
			server.overworld,
			points[i].x.toDouble(),
			server.overworld.logicalHeight.toDouble(),
			points[i].z.toDouble(),
			setOf(),
			player.yaw,
			player.pitch,
			false
		)
	}
}

fun teleportToWorldSpawn(player: ServerPlayerEntity) {
	val component = game.getComponent<SpawnBoxComponent>() ?: return
	val world = worldById(component.worldId) ?: return
	val y = highestY(world, component.pos.blockPos())

	val pos = BlockPos(component.pos.x, y, component.pos.z)

	world.setBlockState(pos.down(), Blocks.BEDROCK.defaultState)
	world.setBlockState(pos, Blocks.AIR.defaultState)
	world.setBlockState(pos.up(), Blocks.AIR.defaultState)

	teleport(player, world, pos)
}

fun teleportToOwnSpawn(player: ServerPlayerEntity) {
	val pos = player.spawnPointPosition ?: server.overworld.spawnPos
	val dimension = player.spawnPointDimension
	player.teleport(
		server.getWorld(dimension),
		pos.x.toDouble(),
		pos.y.toDouble(),
		pos.z.toDouble(),
		setOf(),
		player.yaw,
		player.pitch,
		false
	)
}

fun randomTeleport(player: ServerPlayerEntity, radius: Int, precision: Int) {
	val angle = random.nextFloat() * PI * 2
	val x = cos(angle) * radius
	val z = sin(angle) * radius
	executeCommand("spreadplayers ${x.roundToInt()} ${z.roundToInt()} 0 $precision true ${player.nameForScoreboard}")
}

fun getAllPlayers(): MutableList<ServerPlayerEntity> {
	return server.playerManager.playerList
}

fun forEachPlayer(fn: ((player: ServerPlayerEntity) -> Unit)) {
	getAllPlayers().forEach(fn)
}

fun playSoundToEveryone(soundEvent: SoundEvent, volume: Float = 1.0f, pitch: Float = 1.0f) {
	playSound(getAllPlayers(), soundEvent, volume, pitch)
}

fun playSound(
	players: List<ServerPlayerEntity>,
	soundEvent: SoundEvent,
	volume: Float = 1.0f,
	pitch: Float = 1.0f
) {
	players.forEach {
		it.playSoundToPlayer(soundEvent, SoundCategory.MASTER, volume, pitch)
	}
}

fun playSound(player: ServerPlayerEntity, soundEvent: SoundEvent, volume: Float = 1.0f, pitch: Float = 1.0f) {
	player.playSoundToPlayer(soundEvent, SoundCategory.MASTER, volume, pitch)
}

fun teleport(player: ServerPlayerEntity, world: World, pos: BlockPos) {
	player.teleport(
		world as ServerWorld? ?: server.overworld,
		pos.x.toDouble() + 0.5,
		pos.y.toDouble(),
		pos.z.toDouble() + 0.5,
		PositionFlag.getFlags(0),
		player.yaw,
		player.pitch,
		false
	)
}

fun setSpawnPoint(player: ServerPlayerEntity) {
	player.setSpawnPoint(
		player.world.registryKey,
		player.blockPos,
		0f,
		true,
		true
	)
}