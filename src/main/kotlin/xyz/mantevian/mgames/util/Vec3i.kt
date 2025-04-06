package xyz.mantevian.mgames.util

import kotlinx.serialization.Serializable
import net.minecraft.util.math.BlockPos

@Serializable
data class Vec3i(var x: Int, var y: Int, var z: Int) {
	fun blockPos(): BlockPos {
		return BlockPos(x, y, z)
	}
}