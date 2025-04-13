package xyz.mantevian.mgames.game

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource

interface GameComponent {
	fun init() {}
	fun tick() {}
	fun canStart(): Boolean = true
	fun start() {}
	fun finish() {}
	fun reset() {}
	fun onDeath(entity: LivingEntity, source: DamageSource) {}
}