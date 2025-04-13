package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode
import xyz.mantevian.mgames.SirPlayer
import xyz.mantevian.mgames.game
import xyz.mantevian.mgames.server
import xyz.mantevian.mgames.sir.giveRandomItem
import xyz.mantevian.mgames.util.*

@Serializable
@SerialName("skyblock_item_randomizer")
class SirComponent : GameComponent {
	@SerialName("life_count")
	var lifeCount: Int = 3

	@SerialName("item_timer")
	var itemTimer: MGDuration = MGDuration.fromTicks(100)

	@SerialName("players")
	val players: MutableMap<String, SirPlayer> = mutableMapOf()

	@SerialName("radius")
	var radius: Int = 75

	@SerialName("enchantment_chance")
	var enchantmentChance: Double = 0.2

	@SerialName("attribute_modifier_chance")
	var attributeModifierChance: Double = 0.2

	@SerialName("banned_items")
	val bannedItems: MutableList<String> =
		mutableListOf("minecraft:ender_dragon_spawn_egg", "minecraft:wither_spawn_egg")

	override fun start() {
		forEachPlayer {
			players[it.uuidAsString] = SirPlayer()
		}

		val points = pointsInCircle(Vec3i(0, 64, 0), radius, players.size)

		for (i in points.indices) {
			island(setOf("oak", "birch", "spruce", "acacia", "jungle", "cherry").shuffled()[0], points[i])
		}

		teleportInCircle(server.playerManager.playerList, radius)
	}

	override fun tick() {
		when (game.state) {
			GameState.PLAYING -> {
				if (game.time.getTicks() % itemTimer.getTicks() == 0) {
					forEachPlayer {
						if (it.interactionManager.gameMode != GameMode.SPECTATOR) {
							giveRandomItem(it, bannedItems, enchantmentChance, attributeModifierChance)
						}
					}
				}
			}

			GameState.NOT_INIT -> {}
			GameState.WAITING -> {}
		}
	}

	override fun onDeath(entity: LivingEntity, source: DamageSource) {
		if (entity !is ServerPlayerEntity) {
			return
		}

		val playerEntry = players[entity.uuidAsString] ?: return
		playerEntry.deaths++

		if (playerEntry.deaths >= lifeCount) {
			announce(standardText("${entity.nameForScoreboard} has been eliminated!").formatted(Formatting.RED))
			entity.changeGameMode(GameMode.SPECTATOR)
		}
	}
}