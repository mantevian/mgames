package xyz.mantevian.mgames.game

import kotlinx.serialization.Serializable
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity

@Serializable
sealed interface GameComponent {
    fun init() {}
    fun tick() {}
    fun canStart(): Boolean = true
    fun start() {}
    fun finish() {}
    fun reset() {}
    fun onDeath(entity: LivingEntity, source: DamageSource) {}
}