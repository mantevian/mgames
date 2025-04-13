package xyz.mantevian.mgames.util

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.gen.feature.ConfiguredFeature
import xyz.mantevian.mgames.server
import java.lang.Math.clamp
import kotlin.math.*

fun box(center: Vec3i, radius: Int, height: Int, blockId: String) {
	val a = Vec3i(center.x - radius, center.y, center.z - radius)
	val b = Vec3i(center.x + radius, center.y + height, center.z + radius)
	executeCommand(
		"fill ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} $blockId hollow"
	)
}

fun setBlock(world: ServerWorld, pos: Vec3i, block: Block) {
	world.setBlockState(pos.blockPos(), block.defaultState, 2)
}

fun getBlock(world: ServerWorld, pos: Vec3i): BlockState {
	return world.getBlockState(pos.blockPos())
}

fun island(
	center: Vec3i,
	radius: Int,
	height: Int,
	surfaceBlock: Block = Blocks.GRASS_BLOCK,
	mainBlock: Block = Blocks.DIRT,
	stoneBlock: Block = Blocks.STONE,
	addSupportingBlocks: Boolean = false,
	grassFeature: String = "patch_grass",
	treeFeature: String = "oak",
	flowerFeature: String = "forest_flowers"
) {
	val rd = radius.toDouble()

	var a = nextDouble(0.1, rd / 5)
	val b = nextInt(1..radius)

	var d = nextDouble(0.1, rd / 5)
	val e = nextInt(1..radius)

	val g = nextDouble(rd * 0.5, rd * 0.75)

	for (y in 0..<height) {
		val yd = y.toDouble()

		val c = nextDouble(0.0, PI * 2.0)
		val f = nextDouble(0.0, PI * 2.0)

		a += nextDouble(-0.1, 0.1)
		d += nextDouble(-0.1, 0.1)

		val heightRatio = clamp((yd - 2.0) / ((height - 1.0) * 0.75), 0.0, 1.0)
		// a * sin(angle * b + pi * c) + d * cos(angle * e + pi * f) + g

		for (x in -radius..radius) {
			for (z in -radius..radius) {
				var block = mainBlock

				val xd = x.toDouble()
				val zd = z.toDouble()
				val angle = atan2(xd, zd)
				val r = sqrt(xd * xd + zd * zd)

				val here = Vec3i(center.x + x, center.y - y, center.z + z)
				val up = Vec3i(center.x + x, center.y - y + 1, center.z + z)

				if (nextBoolean(heightRatio)) {
					block = stoneBlock
				}

				if (y == 0) {
					block = surfaceBlock
				}

				if (y == 0 || getBlock(server.overworld, up).isOpaqueFullCube) {
					if (r < a * sin(angle * b + PI * c) + d * cos(angle * e + PI * f) + g * (1 - heightRatio)) {
						setBlock(server.overworld, here, block)
					}
				}
			}
		}
	}

	if (addSupportingBlocks) {
		for (x in -radius..radius) {
			for (z in -radius..radius) {
				for (y in 0..height) {
					val blockHere = getBlock(server.overworld, Vec3i(center.x + x, center.y - y, center.z + z))
					val blockDown = getBlock(server.overworld, Vec3i(center.x + x, center.y - y - 1, center.z + z))
					if (blockDown.isAir && blockHere.isOf(surfaceBlock)) {
						setBlock(server.overworld, Vec3i(x, y - 1, z), mainBlock)
						break
					}
				}
			}
		}
	}

	for (i in 1..radius) {
		val x = nextInt(-radius..radius)
		val z = nextInt(-radius..radius)
		placeFeature(grassFeature, BlockPos(x + center.x, center.y + 1, z + center.z))
	}

	for (i in 1..(radius / 3)) {
		val x = nextInt(-radius..radius)
		val z = nextInt(-radius..radius)
		placeFeature(flowerFeature, BlockPos(x + center.x, center.y + 1, z + center.z))
	}

	var attempts = 0
	var treesLeft = max(1, radius * radius / 100)

	while (attempts < radius * radius && treesLeft > 0) {
		val x = nextInt(-radius..radius)
		val z = nextInt(-radius..radius)
		val pos = BlockPos(x + center.x, center.y, z + center.z)
		if (getBlock(server.overworld, Vec3i(pos)).isOf(surfaceBlock) && placeFeature(treeFeature, pos.up())) {
			treesLeft--
			attempts = 0
			continue
		}
		attempts++
	}

	setBlock(server.overworld, center, Blocks.BEDROCK)
}

fun island(variant: String, pos: Vec3i) {
	when (variant) {
		"oak" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_default",
			treeFeature = "oak"
		)

		"birch" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_default",
			treeFeature = "birch_tall"
		)

		"spruce" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "patch_brown_mushroom",
			treeFeature = "trees_taiga"
		)

		"acacia" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_plain",
			treeFeature = "acacia"
		)

		"jungle" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass_jungle",
			flowerFeature = "flower_default",
			treeFeature = "jungle_tree"
		)

		"cherry" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_cherry",
			treeFeature = "cherry"
		)

		"pale" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.PALE_MOSS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "pale_moss_patch",
			flowerFeature = "flower_pale_garden",
			treeFeature = "pale_oak"
		)

		"lush" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.GRASS_BLOCK,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_grass",
			flowerFeature = "flower_flower_forest",
			treeFeature = "azalea_tree"
		)

		"crimson" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.CRIMSON_NYLIUM,
			mainBlock = Blocks.NETHERRACK,
			stoneBlock = Blocks.BLACKSTONE,
			grassFeature = "crimson_forest_vegetation",
			flowerFeature = "crimson_forest_vegetation",
			treeFeature = "crimson_fungus"
		)

		"warped" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.WARPED_NYLIUM,
			mainBlock = Blocks.NETHERRACK,
			stoneBlock = Blocks.BLACKSTONE,
			grassFeature = "warped_forest_vegetation",
			flowerFeature = "warped_forest_vegetation",
			treeFeature = "warped_fungus"
		)

		"soul_sand" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.SOUL_SOIL,
			mainBlock = Blocks.SOUL_SAND,
			stoneBlock = Blocks.NETHERRACK,
			grassFeature = "patch_soul_fire",
			flowerFeature = "",
			treeFeature = "fossil_diamonds"
		)

		"desert" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.SAND,
			mainBlock = Blocks.SANDSTONE,
			stoneBlock = Blocks.STONE,
			addSupportingBlocks = true,
			grassFeature = "patch_dead_bush",
			flowerFeature = "patch_cactus",
			treeFeature = ""
		)

		"badlands" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.RED_SAND,
			mainBlock = Blocks.TERRACOTTA,
			stoneBlock = Blocks.STONE,
			addSupportingBlocks = true,
			grassFeature = "patch_dead_bush",
			flowerFeature = "patch_cactus",
			treeFeature = ""
		)

		"snow" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.SNOW_BLOCK,
			mainBlock = Blocks.PACKED_ICE,
			stoneBlock = Blocks.STONE,
			grassFeature = "pile_snow",
			flowerFeature = "patch_pumpkin",
			treeFeature = "trees_taiga"
		)

		"ice" -> island(
			center = pos,
			radius = 10,
			height = 30,
			surfaceBlock = Blocks.BLUE_ICE,
			mainBlock = Blocks.PACKED_ICE,
			stoneBlock = Blocks.STONE,
			grassFeature = "",
			flowerFeature = "blue_ice",
			treeFeature = "ice_spike"
		)

		"mushroom" -> island(
			center = pos,
			radius = 10,
			height = 10,
			surfaceBlock = Blocks.MYCELIUM,
			mainBlock = Blocks.DIRT,
			stoneBlock = Blocks.STONE,
			grassFeature = "patch_brown_mushroom",
			flowerFeature = "patch_red_mushroom",
			treeFeature = "mushroom_island_vegetation"
		)
	}
}

fun getFeature(name: String): ConfiguredFeature<*, *>? {
	val registry = server.registryManager.getOrThrow(RegistryKeys.CONFIGURED_FEATURE)
	return registry.get(Identifier.of("minecraft", name))
}

fun placeFeature(name: String, pos: BlockPos): Boolean {
	val feature = getFeature(name) ?: return false

	return feature.generate(
		server.overworld,
		server.overworld.chunkManager.chunkGenerator,
		server.overworld.random,
		pos
	)
}