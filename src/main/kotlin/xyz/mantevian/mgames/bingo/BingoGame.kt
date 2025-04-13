package xyz.mantevian.mgames.bingo

import xyz.mantevian.mgames.game.*

fun createBingoGame(): Game {
	return Game().apply {
		setComponents(
			BingoComponent(),
			GameTimeComponent(),
			HandEnchantmentsComponent(),
			SpawnBoxComponent(),
			WorldSizeComponent(),
			KeepInventoryComponent()
		)
	}
}