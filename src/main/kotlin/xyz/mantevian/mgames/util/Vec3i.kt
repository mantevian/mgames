package xyz.mantevian.mgames.util

import kotlinx.serialization.Serializable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.roundToInt

@Serializable
data class Vec3i(var x: Int, var y: Int, var z: Int) {
	constructor(vec3d: Vec3d) : this(vec3d.x.roundToInt(), vec3d.y.roundToInt(), vec3d.z.roundToInt())

	constructor(blockPos: BlockPos) : this(blockPos.x, blockPos.y, blockPos.z)

	fun blockPos(): BlockPos {
		return BlockPos(x, y, z)
	}

	fun up(): Vec3i {
		return Vec3i(x, y + 1, z)
	}
}