package xyz.mantevian.mgames.util

const val WIDTH = 15
const val HEIGHT = 9

fun bedrockBoxAtWorldBottom() {
	executeCommand(
		"fill -$WIDTH -63 -$WIDTH $WIDTH ${-63 + HEIGHT} $WIDTH minecraft:bedrock"
	)
	executeCommand(
		"fill -${WIDTH - 1} -62 -${WIDTH - 1} ${WIDTH - 1} ${-62 + HEIGHT - 2} ${WIDTH - 1} minecraft:air"
	)
}