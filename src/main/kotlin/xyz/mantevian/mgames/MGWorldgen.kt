package xyz.mantevian.mgames

const val WIDTH = 15
const val HEIGHT = 9

class MGWorldgen(private val mg: MG) {
	fun bedrockBoxAtWorldBottom() {
		mg.executeCommand(
			"fill -$WIDTH -63 -$WIDTH $WIDTH ${-63 + HEIGHT} $WIDTH minecraft:bedrock"
		)
		mg.executeCommand(
			"fill -${WIDTH - 1} -62 -${WIDTH - 1} ${WIDTH - 1} ${-62 + HEIGHT - 2} ${WIDTH - 1} minecraft:air"
		)
	}
}
