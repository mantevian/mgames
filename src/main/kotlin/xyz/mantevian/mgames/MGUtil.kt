package xyz.mantevian.mgames

import net.minecraft.component.type.DyedColorComponent
import net.minecraft.enchantment.Enchantment
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
import net.minecraft.scoreboard.ScoreHolder
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

class MGUtil(private val mg: MG) {
	fun resetPlayersMinecraftStats() {
		forEachPlayer {
			it.clearStatusEffects()
//			it.advancementTracker.clearCriteria()
			it.setExperienceLevel(0)
			it.setExperiencePoints(0)
			it.inventory.clear()
			it.recipeBook.lockRecipes(mg.server.recipeManager.values(), it)
		}
	}

	fun tpPlayersToWorldBottom() {
		forEachPlayer { player ->
			player.teleport(
				mg.server.overworld,
				0.0,
				-62.0,
				0.0,
				setOf(),
				0.0f,
				0.0f,
				false
			)

			mg.server.overworld.setSpawnPos(BlockPos(0, -62, 0), 0.0f)
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
		mg.server.playerManager.broadcast(text, false)
	}

	fun announce(text: Text, soundEvent: SoundEvent, pitch: Float = 1.0f, volume: Float = 1.0f) {
		mg.server.playerManager.broadcast(text, false)
		playSoundToEveryone(soundEvent, volume, pitch)
	}

	fun title(text: String) {
		mg.executeCommand("title @a title {\"text\":\"$text\"}")
	}

	fun itemById(id: String): Item {
		return Registries.ITEM.get(Identifier.of(id))
	}

	fun enchantmentById(id: String): Enchantment? {
		return mg.server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).get(Identifier.of(id))
	}

	fun getEnchantmentEntry(enchantment: Enchantment): RegistryEntry<Enchantment> {
		return mg.server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(enchantment)
	}

	fun potionById(id: String): Potion? {
		return Registries.POTION.get(Identifier.of(id))
	}

	fun statusEffectById(id: String): StatusEffect? {
		return Registries.STATUS_EFFECT.get(Identifier.of(id))
	}

	fun createScoreboardSidebar(scoreboardName: String, displayName: String) {
		if (mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") != null) {
			return
		}
		val objective = mg.server.scoreboard.addObjective(
			"${Main.MOD_ID}.$scoreboardName",
			ScoreboardCriterion.DUMMY,
			standardText(displayName),
			ScoreboardCriterion.RenderType.INTEGER,
			true,
			null
		)
		mg.server.scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective)
	}

	fun deleteScoreboard(scoreboardName: String) {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return
		mg.server.scoreboard.removeObjective(objective)
	}

	fun getScore(playerName: String, scoreboardName: String): Int {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return 0
		val scoreAccess = mg.server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
		return scoreAccess.score
	}

	fun setScore(playerName: String, scoreboardName: String, value: Int) {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return
		val scoreAccess = mg.server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
		scoreAccess.score = value
	}

	fun addScore(playerName: String, scoreboardName: String, value: Int) {
		val objective = mg.server.scoreboard.getNullableObjective("${Main.MOD_ID}.$scoreboardName") ?: return
		val scoreAccess = mg.server.scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective)
		scoreAccess.score += value
	}

	fun calculateColorValue(colors: List<String>): Int {
		val stack =
			ItemStackBuilder(Items.WOLF_ARMOR).ofColorMix(colors.map { DyeColor.byName(it, DyeColor.BLACK)!! }).build()
		return DyedColorComponent.getColor(stack, 0)
	}

	fun nextInt(range: IntRange): Int {
		return mg.server.overworld.random.nextBetween(range.first, range.last)
	}

	fun nextDouble(min: Double, max: Double): Double {
		return mg.server.overworld.random.nextDouble() * (max - min) + min
	}

	fun nextBoolean(chance: Double): Boolean {
		return nextDouble(0.0, 1.0) < chance
	}

	fun teleportInCircle(players: List<ServerPlayerEntity>, radius: Int, precision: Int) {
		val count = players.size
		for (i in players.indices) {
			val player = players[i]
			val angle = i.toDouble() / count.toDouble()
			val x = cos(angle * 2 * PI) * radius
			val z = sin(angle * 2 * PI) * radius

			player.teleport(
				mg.server.overworld,
				x,
				mg.server.overworld.logicalHeight.toDouble(),
				z,
				setOf(),
				player.yaw,
				player.pitch,
				false
			)
		}
	}

	fun teleportToWorldSpawn(player: ServerPlayerEntity) {
		mg.executeCommand("execute in overworld run spreadplayers 0 0 0 20 true ${player.nameForScoreboard}")
	}

	fun teleportToOwnSpawn(player: ServerPlayerEntity) {
		val pos = player.spawnPointPosition ?: mg.server.overworld.spawnPos
		val dimension = player.spawnPointDimension
		player.teleport(
			mg.server.getWorld(dimension),
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
		val angle = mg.server.overworld.random.nextFloat() * PI * 2
		val x = cos(angle) * radius
		val z = sin(angle) * radius
		mg.executeCommand("spreadplayers ${x.roundToInt()} ${z.roundToInt()} 0 $precision true ${player.nameForScoreboard}")
	}

	fun getAllPlayers(): MutableList<ServerPlayerEntity> {
		return mg.server.playerManager.playerList
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

	fun teleport(player: ServerPlayerEntity, world: ServerWorld, pos: BlockPos) {
		player.teleport(
			world,
			pos.x.toDouble(),
			pos.y.toDouble(),
			pos.z.toDouble(),
			PositionFlag.getFlags(0),
			player.yaw,
			player.pitch,
			false
		)
	}
}